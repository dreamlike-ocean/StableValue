package io.github.dreamlike.stableValue;

import io.github.dreamlike.stableValue.StableValueGenerator;

import java.util.function.Supplier;

public interface StableValue<T> {

    T get();


    public static <T> StableValue<T> of(Supplier<T> factory) {
        return StableValueGenerator.of(factory);
    }

    static boolean setMode(boolean enableHidden) {
        boolean old = StableValueGenerator.enable_hidden;
        StableValueGenerator.enable_hidden = enableHidden;
        return old;
    }
}
