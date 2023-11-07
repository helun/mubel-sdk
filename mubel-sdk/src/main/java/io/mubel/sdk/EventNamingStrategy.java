package io.mubel.sdk;

import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class EventNamingStrategy implements Comparable<EventNamingStrategy> {

    private final Integer order;

    protected EventNamingStrategy(int order) {
        this.order = order;
    }

    @Override
    public int compareTo(EventNamingStrategy o) {
        return this.order.compareTo(o.order);
    }

    public static EventNamingStrategy byClassSimpleName(Class<?>... eventClasses) {
        return new ClassSimpleNameStrategy(Arrays.asList(eventClasses));
    }

    public static EventNamingStrategy byClass() {
        return new ByClassNameStrategy();
    }

    public static EventNamingStrategy mapped(Map<String, Class<?>> mappings) {
        return new MappedNamingStrategy(mappings);
    }

    public abstract String nameFor(Class<?> eventClass);

    public abstract Class<?> classFor(String name);

    static class ClassSimpleNameStrategy extends EventNamingStrategy {

        private final Map<String, Class<?>> nameToClass = new HashMap<>();
        private final Map<Class<?>, String> classToName = new HashMap<>();

        public ClassSimpleNameStrategy(Collection<Class<?>> eventClasses) {
            super(100);
            Utils.assertNotEmpty(eventClasses, () -> new MubelConfigurationException("Event classes cannot be empty"));
            indexClasses(eventClasses);
        }

        private void indexClasses(Collection<Class<?>> eventClasses) {
            if (eventClasses.isEmpty()) {
                return;
            }
            for (final var eventClass : eventClasses) {
                if (!eventClass.isInterface()) {
                    indexClass(eventClass);
                }
                indexClasses(Arrays.asList(eventClass.getClasses()));
            }
        }

        private void indexClass(Class<?> eventClass) {
            final var name = eventClass.getSimpleName();
            nameToClass.put(name, eventClass);
            classToName.put(eventClass, name);
        }

        @Override
        public String nameFor(Class<?> eventClass) {
            return classToName.get(eventClass);
        }

        @Override
        public Class<?> classFor(String name) {
            return nameToClass.get(name);
        }
    }

    static class ByClassNameStrategy extends EventNamingStrategy {

        private final Map<String, Class<?>> nameToClass = new ConcurrentHashMap<>();

        public ByClassNameStrategy() {
            super(Integer.MAX_VALUE);
        }

        @Override
        public String nameFor(Class<?> eventClass) {
            return eventClass.getName();
        }

        @Override
        public Class<?> classFor(String name) {
            return nameToClass.computeIfAbsent(name, this::resolveClassForName);
        }

        private Class<?> resolveClassForName(String className) {
            try {
                return this.getClass().getClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }

    static class MappedNamingStrategy extends EventNamingStrategy {
        private final Map<Class<?>, String> classToName;
        private final Map<String, Class<?>> nameToClass;

        public MappedNamingStrategy(Map<String, Class<?>> mappings) {
            super(0);
            nameToClass = Map.copyOf(mappings);
            this.classToName = Utils.swapKeyValues(mappings);
        }

        @Override
        public Class<?> classFor(String name) {
            return nameToClass.get(name);
        }

        @Override
        public String nameFor(Class<?> eventClass) {
            return classToName.get(eventClass);
        }
    }
}
