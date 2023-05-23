package io.schematools.generator;

import org.junit.jupiter.api.Test;

public class JsonSchemaPojoGeneratorTest {

    @Test
    void basic() {
        JsonSchemaPojoGenerator jsonSchemaPojoGenerator = new JsonSchemaPojoGenerator();
        jsonSchemaPojoGenerator.generate("src/test/resources/schema/basic", "target/generated-sources");
    }

    @Test
    void refexamples() {
        JsonSchemaPojoGenerator jsonSchemaPojoGenerator = new JsonSchemaPojoGenerator();
        jsonSchemaPojoGenerator.generate("src/test/resources/schema/refexamples", "target/generated-sources");
    }

}
