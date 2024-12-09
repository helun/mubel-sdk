package io.mubel.spring;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesValidationTest {

    Validator validator;

    @BeforeEach
    void setUp() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void invalid_address() {
        MubelProperties props = new MubelProperties(
                ";DROP DATABASE",
                "myEsid",
                "my_backend",
                MubelProperties.IdGenerationStrategy.ORDERED
        );
        var errors = validator.validate(props);
        expectValidationError(errors, "address", "Invalid address");
    }

    @Test
    void address_is_null() {
        MubelProperties props = new MubelProperties(
                null,
                "myEsid",
                "my_backend",
                MubelProperties.IdGenerationStrategy.ORDERED
        );
        var errors = validator.validate(props);
        expectValidationError(errors, "address", "must not be null");
    }

    @Test
    void invalid_eventStoreId() {
        MubelProperties props = new MubelProperties(
                "192.168.0.1:9090",
                "DROP DATABASE;",
                "my_backend",
                MubelProperties.IdGenerationStrategy.ORDERED
        );
        var errors = validator.validate(props);
        expectValidationError(errors, "eventStoreId", "Invalid event store id");
    }

    @Test
    void eventStoreId_is_null() {
        MubelProperties props = new MubelProperties(
                "192.168.0.1:9090",
                null,
                "my_backend",
                MubelProperties.IdGenerationStrategy.ORDERED
        );
        var errors = validator.validate(props);
        expectValidationError(errors, "eventStoreId", "must not be null");
    }

    @Test
    void invalid_storageBackendName() {
        MubelProperties props = new MubelProperties(
                "192.168.0.1:9090",
                "myEsid",
                "DROP DATABASE",
                MubelProperties.IdGenerationStrategy.ORDERED
        );
        var errors = validator.validate(props);
        expectValidationError(errors, "storageBackendName", "must match \"[A-Za-z0-9_-]{1,255}");
    }

    @Test
    void storageBackendName_is_null() {
        MubelProperties props = new MubelProperties(
                "192.168.0.1:9090",
                "myEsid",
                null,
                MubelProperties.IdGenerationStrategy.ORDERED
        );
        var errors = validator.validate(props);
        expectValidationError(errors, "storageBackendName", "must not be null");
    }

    private static void expectValidationError(Set<ConstraintViolation<MubelProperties>> errors, String path, String messageStart) {
        assertThat(errors)
                .isNotEmpty()
                .first()
                .satisfies(error -> {
                    assertThat(error.getPropertyPath().toString()).isEqualTo(path);
                    assertThat(error.getMessage()).startsWith(messageStart);
                });
    }
}
