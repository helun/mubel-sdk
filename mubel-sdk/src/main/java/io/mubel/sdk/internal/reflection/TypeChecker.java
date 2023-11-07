package io.mubel.sdk.internal.reflection;

public final class TypeChecker {

    private TypeChecker() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T ensureAssignable(Object instance, Class<T> aClass) {
        if (!aClass.isAssignableFrom(instance.getClass())) {
            throw new IllegalArgumentException("Expected instance of %s, got %s".formatted(aClass.getName(), instance.getClass().getName()));
        }
        return (T) instance;
    }
}
