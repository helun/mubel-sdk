package io.mubel.sdk.internal;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Utils {

    private Utils() {
    }

    public static <K, V> Map<V, K> swapKeyValues(Map<K, V> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        Map.Entry::getKey,
                        (a, b) -> {
                            throw new IllegalArgumentException("Swapping keys & values resulted in duplicates for keys: %s, %s".formatted(a, b));
                        }
                ));
    }

    public static void assertNotEmpty(Collection<?> input, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (input == null || input.isEmpty()) {
            throw exceptionSupplier.get();
        }
    }

    public static long assertGteZeroLong(long input, Function<Long, ? extends RuntimeException> exceptionSupplier) {
        if (input < 0) {
            throw exceptionSupplier.apply(input);
        }
        return input;
    }

    public static int assertGteZeroInt(int input, Function<Integer, ? extends RuntimeException> exceptionSupplier) {
        if (input < 0) {
            throw exceptionSupplier.apply(input);
        }
        return input;
    }

    public static int assertPositive(int input, Function<Integer, ? extends RuntimeException> exceptionSupplier) {
        if (input <= 0) {
            throw exceptionSupplier.apply(input);
        }
        return input;
    }

    public static long assertMaxValue(long input, long maxValue, Function<Long, ? extends RuntimeException> exceptionSupplier) {
        if (input > maxValue) {
            throw exceptionSupplier.apply(input);
        }
        return input;
    }

    public static <T> T requireNonNull(T input, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (input == null) {
            throw exceptionSupplier.get();
        }
        return input;
    }

    public static String validate(
            String input,
            Pattern pattern,
            Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!pattern.matcher(input).matches()) {
            throw exceptionSupplier.get();
        }
        return input;
    }

    public static <T> boolean anyNull(Object... inputs) {
        if (inputs == null) {
            return true;
        }
        for (var input : inputs) {
            if (input == null) {
                return true;
            }
        }
        return false;
    }
}
