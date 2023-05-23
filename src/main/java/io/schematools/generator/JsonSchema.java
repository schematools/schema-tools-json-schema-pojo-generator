package io.schematools.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JDefinedClass;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class JsonSchema {

    private final Path filePath;
    private final JsonNode rootJsonNode;
    private final IdAdapter idAdapter;

    private JDefinedClass jDefinedClass;

    private boolean processed;

    public JsonSchema(Path filePath, JsonNode rootJsonNode, IdAdapter idAdapter) {
        this.filePath = filePath;
        this.rootJsonNode = rootJsonNode;
        this.idAdapter = idAdapter;
    }

    public Set<Map.Entry<String, JsonNode>> properties() {
        return rootJsonNode.get("properties").properties();
    }

    public Path getFilePath() {
        return filePath;
    }

    public JsonNode getRootJsonNode() {
        return rootJsonNode;
    }

    public IdAdapter getIdAdapter() {
        return idAdapter;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public JDefinedClass getjDefinedClass() {
        return jDefinedClass;
    }

    public void setjDefinedClass(JDefinedClass jDefinedClass) {
        this.jDefinedClass = jDefinedClass;
    }

    @Override
    public String toString() {
        return "JsonSchema{" +
                "filePath=" + filePath +
                ", rootJsonNode=" + rootJsonNode +
                ", idAdapter=" + idAdapter +
                ", processed=" + processed +
                '}';
    }
}
