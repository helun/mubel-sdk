package io.mubel.sdk;

import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A strategy for naming events.
 * Multiple strategies can be used to provide a fallback mechanism.
 */
public abstract class EventNamingStrategy implements Comparable<EventNamingStrategy> {

    private final Integer order;

    protected EventNamingStrategy(int order) {
        this.order = order;
    }

    @Override
    public int compareTo(EventNamingStrategy o) {
        return this.order.compareTo(o.order);
    }

    /**
     * Creates a naming strategy that uses the simple name of the event class.
     *
     * This strategy is convenient as it is automatic, but you may run into naming conflicts if you have multiple event classes with the same simple name.
     * And the event type name will be coupled to your classes. Beware of this when refactoring your code.
     *
     * @param eventClasses The event classes.
     * @return The naming strategy.
     */
    public static EventNamingStrategy byClassSimpleName(Class<?>... eventClasses) {
        return new ClassSimpleNameStrategy(Arrays.asList(eventClasses));
    }

    /**
     * Creates a naming strategy that uses the fully qualified class name of the event.
     * 
     * This strategy is convenient as it is automatic, but your event names will be long and coupled to your class structure.
     * This strategy has the same drawbacks as the {@link #byClassSimpleName(Class[])} strategy.
     *
     * @return The naming strategy.
     */
    public static EventNamingStrategy byClass() {
        return new ByClassNameStrategy();
    }

    /**
     * Creates a naming strategy that uses a map of event types to event classes.
     *
     * This strategy provides the most flexibility, but requires the most work to configure.
     * 
     * @param mappings The map of event types to event classes.
     * @return The naming strategy.
     */
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
