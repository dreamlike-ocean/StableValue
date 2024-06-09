package io.github.dreamlike.stableValue;

import java.util.function.Supplier;

public interface StableValue<T> {

    T get();


    public static <T> StableValue<T> of(Supplier<T> factory) {
        return StableValueGenerator.of(factory);
    }

    static boolean setHiddenMode(boolean enableHidden) {
        boolean old = StableValueGenerator.enable_hidden;
        StableValueGenerator.enable_hidden = enableHidden;
        return old;
    }

    static BackEnd setBackend(BackEnd backEnd) {
        boolean old = StableValueGenerator.enable_condy;
        StableValueGenerator.enable_condy = backEnd == BackEnd.CONDY;
        return old ? BackEnd.CONDY : BackEnd.INDY;
    }

    enum BackEnd {
        INDY,
        CONDY,
    }
}
