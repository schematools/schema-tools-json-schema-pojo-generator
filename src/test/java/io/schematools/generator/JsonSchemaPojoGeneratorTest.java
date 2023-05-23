package io.schematools.generator;

import org.junit.jupiter.api.Test;

public class JsonSchemaPojoGeneratorTest {

    @Test
    void basic() {
        var config = new JsonSchemaPojoGeneratorConfiguration("src/test/resources/schema/basic", "target/generated-sources");
        JsonSchemaPojoGenerator jsonSchemaPojoGenerator = new JsonSchemaPojoGenerator();
        jsonSchemaPojoGenerator.generate(config);
    }

    @Test
    void refexamples() {
        var config = new JsonSchemaPojoGeneratorConfiguration("src/test/resources/schema/basic", "target/generated-sources");
        JsonSchemaPojoGenerator jsonSchemaPojoGenerator = new JsonSchemaPojoGenerator();
        jsonSchemaPojoGenerator.generate(config);
    }

}
