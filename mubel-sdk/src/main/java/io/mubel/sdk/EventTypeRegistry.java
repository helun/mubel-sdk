package io.mubel.sdk;

import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EventTypeRegistry {

    private final Map<Class<?>, String> typeByClass = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> classByType = new ConcurrentHashMap<>();

    private final List<EventNamingStrategy> namingStrategies;

    public static EventTypeRegistry.Builder builder() {
        return new Builder();
    }

    private EventTypeRegistry(Builder builder) {
        this.namingStrategies = builder.namingStrategies;
    }

    public Class<?> getClassForType(String typeName) {
        return classByType.computeIfAbsent(typeName, this::resolveClassForType);
    }

    private Class<?> resolveClassForType(String typeName) {
        for (final var strategy : namingStrategies) {
            final var aClass = strategy.classFor(typeName);
            if (aClass != null) {
                typeByClass.put(aClass, typeName);
                return aClass;
            }
        }
        throw new MubelConfigurationException("No class found for event type: '%s'".formatted(typeName));
    }

    public String getTypeNameForClass(Class<?> eventClass) {
        return typeByClass.computeIfAbsent(eventClass, this::resolveTypeNameForClass);
    }

    private String resolveTypeNameForClass(Class<?> aClass) {
        for (final var strategy : namingStrategies) {
            final var name = strategy.nameFor(aClass);
            if (name != null) {
                classByType.put(name, aClass);
                return name;
            }
        }
        throw new MubelConfigurationException("Could not resolve a name for class %s".formatted(aClass.getName()));
    }

    public static class Builder {

        private final List<EventNamingStrategy> namingStrategies = new ArrayList<>();

        public Builder() {

        }

        public Builder withNamingStrategy(io.mubel.sdk.EventNamingStrategy eventNamingStrategy) {
            namingStrategies.add(eventNamingStrategy);
            return this;
        }

        public EventTypeRegistry build() {
            Utils.assertNotEmpty(namingStrategies, () -> new MubelConfigurationException("No naming strategies were provided"));
            Collections.sort(namingStrategies);
            return new EventTypeRegistry(this);
        }
    }
}
