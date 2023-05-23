package io.schematools.generator;

import java.net.URI;

public record IdAdapter(URI idUri, String packageName, String className, String version) {

    public String fullyQualifiedClassName() {
        return packageName + "." + className;
    }

    public URI baseUri() {
        return URI.create(idUri.getScheme()+ "://" + idUri.getHost());
    }

}
