package io.schematools.generator;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public record JsonSchema(Path filePath, JsonNode rootJsonNode, IdAdapter idAdapter) {

    public Set<Map.Entry<String, JsonNode>> properties() {
        return rootJsonNode.get("properties").properties();
    }

}
