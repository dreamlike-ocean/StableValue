package io.github.dreamlike;

import io.github.dreamlike.stableValue.StableValue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

@Warmup(iterations = 5, time = 200, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.Throughput)
@Threads(value = 5)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
public class Main {
    private static final StableValue<String> value = StableValue.of(() -> {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        return "2023";
    });

    private static final StableValue<String> dcl = new DCLStableValue<>(() -> {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        return "2023";
    });


    public static void main(String[] args) throws IOException {
        org.openjdk.jmh.Main.main(args);
    }

    @Benchmark
    public void testIndyStabValue(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            bh.consume(value.get());
        }
    }

    @Benchmark
    public void testDCL(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            bh.consume(dcl.get());
        }
    }


    public static class DCLStableValue<T> implements StableValue<T> {
        public final Supplier<T> factory;

        private volatile T cache;

        public DCLStableValue(Supplier<T> factory) {
            this.factory = factory;
        }


        @Override
        public T get() {
            if (cache != null) {
                return cache;
            }

            synchronized (this) {
                if (cache != null) {
                    return cache;
                }
                return cache = factory.get();
            }
        }
    }
}