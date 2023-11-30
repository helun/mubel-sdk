package io.mubel.sdk;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;

import java.util.UUID;

public class IdGenerator {

    private static final IdGenerator DEFAULT = createDefaultGenerator();

    private static IdGenerator createDefaultGenerator() {
        final var defaultType = System.getProperty("mubel.id.generator", "ordered");
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

    public static IdGenerator timebasedGenerator() {
        return new IdGenerator(Generators.timeBasedReorderedGenerator());
    }

    public static IdGenerator randomGenerator() {
        return new IdGenerator(Generators.timeBasedEpochGenerator());
    }

    public UUID generate() {
        return generator.generate();
    }

    public static IdGenerator defaultGenerator() {
        return DEFAULT;
    }
}
