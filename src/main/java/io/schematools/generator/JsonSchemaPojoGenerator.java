package io.schematools.generator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.codemodel.*;
import jakarta.annotation.Generated;
import org.apache.commons.text.CaseUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class JsonSchemaPojoGenerator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<URI, JsonSchema> jsonSchemaMap = new HashMap<>();
    private final JCodeModel jCodeModel = new JCodeModel();

    private URI fileBaseUri = null;

    public void generate(JsonSchemaPojoGeneratorConfiguration jsonSchemaPojoGeneratorConfiguration) {
        try {
            List<Path> filePaths = this.getAllFilePaths(jsonSchemaPojoGeneratorConfiguration.sourcePath());
            for (Path path: filePaths) {
                JsonSchema jsonSchema = this.createJsonSchema(path);
                jsonSchemaMap.put(jsonSchema.getIdAdapter().idUri(), jsonSchema);
            }
            for (JsonSchema jsonSchema: jsonSchemaMap.values()) {
                if (!jsonSchema.isProcessed()) {
                    this.fileBaseUri = jsonSchema.getIdAdapter().baseUri();
                    this.addJsonSchemaToCodeModel(jsonSchema);
                    this.fileBaseUri = null;
                }
            }
            this.writeJavaFiles(jsonSchemaPojoGeneratorConfiguration.destinationPath());
        } catch (IOException | JClassAlreadyExistsException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Path> getAllFilePaths(String sourcePath) throws IOException {
        return Files.walk(Path.of(sourcePath), Integer.MAX_VALUE)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .collect(Collectors.toList());
    }

    private JsonSchema createJsonSchema(Path path) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(path.toFile());
        IdAdapter idAdapter = this.createIdAdapter(jsonNode.get("$id").asText());
        return new JsonSchema(path, jsonNode, idAdapter);
    }

    private IdAdapter createIdAdapter(String id) {
        URI uri = URI.create(id);
        if (Objects.isNull(uri.getHost()) || uri.getHost().isEmpty()) {
            //TODO Better validation and error response
            throw new RuntimeException();
        }
        List<String> hostSegments = Arrays.stream(uri.getHost().split("\\.")).collect(Collectors.toList());
        Collections.reverse(hostSegments);
        List<String> pathSegments = Arrays.stream(uri.getPath().split("/")).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        String version = pathSegments.remove(pathSegments.size() - 1);
        String className = convertToCamelCase(pathSegments.remove(pathSegments.size() - 1), true);
        hostSegments.addAll(pathSegments);
        String packageName = hostSegments.stream().collect(Collectors.joining("."));
        return new IdAdapter(uri, packageName, className, version);
    }

    private String convertToCamelCase(String in, boolean capitalizeFirstLetter) {
        return CaseUtils.toCamelCase(in, capitalizeFirstLetter, '-', '_');
    }

    private JDefinedClass addJsonSchemaToCodeModel(JsonSchema jsonSchema) throws JClassAlreadyExistsException {
        if (jsonSchema.isProcessed()){
            return jsonSchema.getjDefinedClass();
        }
        JDefinedClass jDefinedClass = jCodeModel._class(jsonSchema.getIdAdapter().fullyQualifiedClassName());
        jDefinedClass.annotate(Generated.class)
                .param("value", "io.schematools")
                .param("comments", jsonSchema.getIdAdapter().version())
                .param("date", LocalDateTime.now().toString());
        Set<Map.Entry<String, JsonNode>> properties = jsonSchema.properties();
        this.handleProperties(jDefinedClass, properties);
        jsonSchema.setProcessed(true);
        jsonSchema.setjDefinedClass(jDefinedClass);
        return jDefinedClass;
    }

    private void handleProperties(JDefinedClass parentClass, Set<Map.Entry<String, JsonNode>> properties) throws JClassAlreadyExistsException {
        for (Map.Entry<String, JsonNode> entry: properties) {
            if (entry.getValue().has("$ref")) {
                handleRef(parentClass, entry.getKey(), entry.getValue());
            } else {
                String type = entry.getValue().get("type").asText();
                if (type.equals("string")) {
                    this.handleStringType(parentClass, entry.getKey(), entry.getValue());
                } else if (type.equals("integer")) {
                    JFieldVar field = parentClass.field(JMod.PUBLIC, Integer.class, convertToCamelCase(entry.getKey(), false));
                    field.annotate(JsonProperty.class).param("value", entry.getKey());
                } else if (type.equals("number")) {
                    JFieldVar field = parentClass.field(JMod.PUBLIC, Double.class, convertToCamelCase(entry.getKey(), false));
                    field.annotate(JsonProperty.class).param("value", entry.getKey());
                } else if (type.equals("object")) {
                    this.handleObjectType(parentClass, entry.getKey(), entry.getValue());
                } else {
                    throw new RuntimeException("Unknown type: " + type);
                }
            }
        }
    }

    private void handleStringType(JDefinedClass parentClass, String name, JsonNode jsonNode) {
        if (jsonNode.has("format")) {
            String format = jsonNode.get("format").asText();
            if (format.equals("decimal")) {
                JFieldVar field = parentClass.field(JMod.PUBLIC, BigDecimal.class, convertToCamelCase(name, false));
                field.annotate(JsonProperty.class).param("value", name);
                field.annotate(JsonFormat.class).param("shape", JsonFormat.Shape.STRING);
            } else {
                throw new RuntimeException("Unknown format: " + format);
            }
        } else {
            JFieldVar field = parentClass.field(JMod.PUBLIC, String.class, convertToCamelCase(name, false));
            field.annotate(JsonProperty.class).param("value", name);
        }
    }

    private void handleObjectType(JDefinedClass parentClass, String name, JsonNode node) throws JClassAlreadyExistsException {
        JDefinedClass jDefinedClass = parentClass._class(convertToCamelCase(name, true));
        JFieldVar field = parentClass.field(JMod.PUBLIC, jDefinedClass, convertToCamelCase(name, false));
        field.annotate(JsonProperty.class).param("value", name);
        Set<Map.Entry<String, JsonNode>> properties = node.get("properties").properties();
        this.handleProperties(jDefinedClass, properties);
    }

    private void handleRef(JDefinedClass parentClass, String name, JsonNode node) throws JClassAlreadyExistsException {
        String ref = node.get("$ref").asText();
        String absRef = this.fileBaseUri.toString() + ref;
        JsonSchema jsonSchema = jsonSchemaMap.get(URI.create(absRef));
        JDefinedClass jDefinedClass = addJsonSchemaToCodeModel(jsonSchema);
        JFieldVar field = parentClass.field(JMod.PUBLIC, jDefinedClass, convertToCamelCase(name, false));
        field.annotate(JsonProperty.class).param("value", name);
    }

    private void writeJavaFiles(String destinationPath) throws IOException {
        jCodeModel.build(new File(destinationPath));
    }

}
