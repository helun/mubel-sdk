package io.mubel.spring;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

import java.net.URI;

public interface MubelConnectionDetails extends ConnectionDetails {

    default URI getUri() {
        return URI.create("mubel://localhost:9898");
    }

}
