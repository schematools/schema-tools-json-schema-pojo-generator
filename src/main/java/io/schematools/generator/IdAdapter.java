package io.schematools.generator;

public record IdAdapter(String id, String packageName, String className, String version) {

    public String fullyQualifiedClassName() {
        return packageName + "." + className;
    }

}
