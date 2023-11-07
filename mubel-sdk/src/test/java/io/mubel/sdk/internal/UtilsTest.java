package io.mubel.sdk.internal;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UtilsTest {

    @Test
    void swapKeyValues() {
        final var input = Map.of(
                "k1", 1,
                "k2", 2,
                "k3", 3
        );

        assertThat(Utils.swapKeyValues(input))
                .containsEntry(1, "k1")
                .containsEntry(2, "k2")
                .containsEntry(3, "k3");
    }

    @Test
    void swapKeyValuesResultingInDuplicateKeys() {
        final var input = Map.of(
                "k1", 2,
                "k2", 2
        );

        assertThatThrownBy(() -> Utils.swapKeyValues(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Swapping keys & values resulted in duplicates for keys:");
    }

}