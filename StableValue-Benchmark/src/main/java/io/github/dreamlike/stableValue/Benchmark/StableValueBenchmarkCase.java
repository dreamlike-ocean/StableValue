package io.github.dreamlike.stableValue.Benchmark;

import io.github.dreamlike.stableValue.StableValue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

@Warmup(iterations = 5, time = 200, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.Throughput)
@Threads(value = 5)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
public class StableValueBenchmarkCase {
    private static final StableValue<String> value = StableValue.of(() -> {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        return UUID.randomUUID().toString();
    });

    private static final StableValue<String> valueHidden;

    private static final StableValue<String> valueCondy;

    static {
        StableValue.setHiddenMode(true);
        StableValue<String> stableValue = StableValue.of(() -> {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            return UUID.randomUUID().toString();
        });
        StableValue.setHiddenMode(false);
        valueHidden = stableValue;

        StableValue.setBackend(StableValue.BackEnd.CONDY);

        valueCondy = StableValue.of(() -> {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            return UUID.randomUUID().toString();
        });

        StableValue.setBackend(StableValue.BackEnd.INDY);
    }


    private static final StableValue<String> dcl = new DCLStableValue<>(() -> {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        return UUID.randomUUID().toString();
    });

    private static final StableValue<String> classInit = new StableValue<String>() {
        static class InternalClass {
            private static final String CACHE;

            static {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                CACHE = UUID.randomUUID().toString();
            }
        }

        @Override
        public String get() {
            return InternalClass.CACHE;
        }
    };

    private static final String plain = UUID.randomUUID().toString();

    private static final StableValue<String> constant = () -> plain;



    @Benchmark
    public void testIndyStabValue(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            bh.consume(value.get());
        }
    }

    @Benchmark
    public void testIndyStabValueHidden(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            bh.consume(valueHidden.get());
        }
    }

    @Benchmark
    public void testIndyStabValueCody(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            bh.consume(valueCondy.get());
        }
    }


    @Benchmark
    public void testDCL(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            bh.consume(dcl.get());
        }
    }

    @Benchmark
    public void testClassInit(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            bh.consume(classInit.get());
        }
    }

    @Benchmark
    public void testPlain(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            bh.consume(constant.get());
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