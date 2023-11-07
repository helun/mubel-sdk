package io.mubel.sdk;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;

import java.util.UUID;

public class IdGenerator {

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
}
