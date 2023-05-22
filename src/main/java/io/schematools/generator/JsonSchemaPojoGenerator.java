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
    private final JCodeModel jCodeModel = new JCodeModel();

    public void generate(String sourcePath, String destinationPath) {
        try {
            List<Path> filePaths = this.getAllFilePaths(sourcePath);
            for (Path path: filePaths) {
                JsonSchema jsonSchema = this.createJsonSchema(path);
                this.addJsonSchemaToCodeModel(jsonSchema);
            }
            this.writeJavaFiles(destinationPath);
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
        return new IdAdapter(id, packageName, className, version);
    }

    private String convertToCamelCase(String in, boolean capitalizeFirstLetter) {
        return CaseUtils.toCamelCase(in, capitalizeFirstLetter, '-', '_');
    }

    private void addJsonSchemaToCodeModel(JsonSchema jsonSchema) throws JClassAlreadyExistsException {
        JDefinedClass jDefinedClass = jCodeModel._class(jsonSchema.idAdapter().fullyQualifiedClassName());
        jDefinedClass.annotate(Generated.class)
                .param("value", "io.schematools")
                .param("comments", jsonSchema.idAdapter().version())
                .param("date", LocalDateTime.now().toString());
        Set<Map.Entry<String, JsonNode>> properties = jsonSchema.properties();
        this.handleProperties(jDefinedClass, properties);
    }

    private void handleProperties(JDefinedClass parentClass, Set<Map.Entry<String, JsonNode>> properties) throws JClassAlreadyExistsException {
        for (Map.Entry<String, JsonNode> entry: properties) {
            String type = entry.getValue().get("type").asText();
            if (type.equals("string")) {
                this.handleStringType(parentClass, entry.getKey(), entry.getValue());
            }
            if (type.equals("integer")) {
                JFieldVar field = parentClass.field(JMod.PUBLIC, Integer.class, convertToCamelCase(entry.getKey(), false));
                field.annotate(JsonProperty.class).param("value", entry.getKey());
            }
            if (type.equals("number")) {
                JFieldVar field = parentClass.field(JMod.PUBLIC, Double.class, convertToCamelCase(entry.getKey(), false));
                field.annotate(JsonProperty.class).param("value", entry.getKey());
            }
            if (type.equals("object")) {
                this.handleObjectType(parentClass, entry.getKey(), entry.getValue());
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

    private void writeJavaFiles(String destinationPath) throws IOException {
        jCodeModel.build(new File(destinationPath));
    }

}
