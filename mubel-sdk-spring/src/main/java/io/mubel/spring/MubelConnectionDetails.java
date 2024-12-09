package io.mubel.spring;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

public interface MubelConnectionDetails extends ConnectionDetails {

    default String getAddress() {
        return "localhost:9090";
    }

}
