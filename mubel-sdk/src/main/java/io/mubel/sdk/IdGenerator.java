package io.mubel.sdk;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;

import java.util.UUID;

public class IdGenerator {

    private static final IdGenerator DEFAULT = createDefaultGenerator();

    private static IdGenerator createDefaultGenerator() {
        final var defaultType = System.getProperty("mubel.id.generator", "timebased");
        return switch (defaultType) {
            case "timebased" -> timebasedGenerator();
            case "random" -> randomGenerator();
            default -> throw new IllegalArgumentException("Unknown id generator type: " + defaultType);
        };
    }

    private final NoArgGenerator generator;


    private IdGenerator(NoArgGenerator generator) {
        this.generator = generator;
    }

    public static IdGenerator timebasedGenerator() {
        return new IdGenerator(Generators.timeBasedGenerator());
    }

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
