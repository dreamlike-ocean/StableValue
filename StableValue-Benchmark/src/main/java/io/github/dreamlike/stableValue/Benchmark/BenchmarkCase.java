package io.github.dreamlike.stableValue.Benchmark;

import io.github.dreamlike.stableValue.DirectLambdaFactory;
import io.github.dreamlike.stableValue.StableValue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Supplier;

@Warmup(iterations = 5, time = 200, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.Throughput)
@Threads(value = 5)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
public class BenchmarkCase {
    private static final StableValue<String> value = StableValue.of(() -> {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        return UUID.randomUUID().toString();
    });

    private static final List<StableValue<String>> Stable_VALUE_LIST = List.of(
            StableValue.of(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            StableValue.of(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            StableValue.of(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            StableValue.of(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            StableValue.of(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            })
    );

    private static final StableValue<String> dcl = new DCLStableValue<>(() -> {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        return UUID.randomUUID().toString();
    });

    private static final List<StableValue<String>> dcl_List = List.of(
            new DCLStableValue<>(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            new DCLStableValue<>(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            new DCLStableValue<>(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            new DCLStableValue<>(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            new DCLStableValue<>(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            })
    );

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

    private static final String baseSTR = UUID.randomUUID().toString().repeat(1024);

    public static final Function<String, Integer> jdkLambda = (s1) -> Integer.valueOf(s1.length() + baseSTR.length());
    public static final Function<String, Integer> directLambda;


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

    @Benchmark
    public void testJDKLambda(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            bh.consume(jdkLambda.apply("123"));
        }
    }

    @Benchmark
    public void testDirectLambda(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            bh.consume(directLambda.apply("123"));
        }
    }

    @Benchmark
    public void testStableList(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            for (StableValue<String> stableValue : Stable_VALUE_LIST) {
                bh.consume(stableValue.get());
            }
        }
    }

    @Benchmark
    public void testDCLList(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            for (StableValue<String> stableValue : dcl_List) {
                bh.consume(stableValue.get());
            }
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

    static {
        try {
            MethodHandle methodHandle = MethodHandles.lookup().findStatic(BenchmarkCase.class, "testMethod", MethodType.methodType(int.class, String.class, String.class));
            directLambda = DirectLambdaFactory.generate(Function.class,
                    MethodHandles.filterReturnValue(
                            methodHandle.bindTo(baseSTR),
                            MethodHandles.lookup()
                                    .findStatic(Integer.class, "valueOf", MethodType.methodType(Integer.class, int.class))
                    )
            );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static int testMethod(String s1, String s2) {
        return s1.length() + s2.length();
    }
}