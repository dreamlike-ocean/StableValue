package io.github.dreamlike.stableValue;

import io.github.dreamlike.stableValue.StableValueGenerator;

import java.util.function.Supplier;

public interface StableValue<T> {

    T get();


    public static <T> StableValue<T> of(Supplier<T> factory) {
        return StableValueGenerator.of(factory);
    }
}
