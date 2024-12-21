package io.mubel.sdk;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import io.mubel.sdk.internal.Constants;

import java.util.UUID;

/**
 * A generator for event ids.
 *
 * This generator is used to generate unique ids for entities such as events, deadlines, scheduled events, etc.
 * The default generator is a time-based generator that generates ids in the order they were created.
 * You can configure the generator to use a random id generator instead.
 * 
 * If you are using a relational database, you can use the {@link IdGenerator#timebasedGenerator()} generator.
 * This generator generates ids in the order they were created, which is useful for database indexing.
 * 
 * If you are using a distributed system, you can use the {@link IdGenerator#randomGenerator()} generator.
 * This generator generates random ids, which is useful for distributed systems.
 */
public class IdGenerator {

    private static final IdGenerator DEFAULT = createDefaultGenerator();

    private static IdGenerator createDefaultGenerator() {
        final var defaultType = System.getProperty(Constants.ID_GENERATOR_TYPE_KEY, "ordered");
        return switch (defaultType) {
            case "ordered" -> timebasedGenerator();
            case "random" -> randomGenerator();
            default -> throw new IllegalArgumentException("Unknown id generator type: " + defaultType);
        };
    }

    private final NoArgGenerator generator;


    private IdGenerator(NoArgGenerator generator) {
        this.generator = generator;
    }

    /**
     * Creates a new id generator that uses a time-based generator (UUID v7).
     *
     * This generator generates ids in the order they were created, which is useful for database indexing.
     *
     * @return The id generator.
     */
    public static IdGenerator timebasedGenerator() {
        return new IdGenerator(Generators.timeBasedEpochRandomGenerator());
    }

    /**
     * Creates a new id generator that uses a random generator (UUID v4).
     *
     * This generator generates random ids, which is useful for distributed systems.
     *
     * @return The id generator.
     */
    public static IdGenerator randomGenerator() {
        return new IdGenerator(Generators.randomBasedGenerator());
    }

    public UUID generate() {
        return generator.generate();
    }

    public static IdGenerator defaultGenerator() {
        return DEFAULT;
    }
}
