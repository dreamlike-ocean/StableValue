package io.github.dreamlike.stableValue.Benchmark;

import io.github.dreamlike.stableValue.FeatureSwitch;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Warmup(iterations = 5, time = 200, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.Throughput)
@Threads(value = 5)
@Fork(jvmArgsAppend = "--enable-preview")
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
public class FeatureSwitchBenchmarkCase {

    public static volatile Supplier<String> dynamicFeature;

    public static final FeatureSwitch<Supplier<String>> featureSwitch = (FeatureSwitch<Supplier<String>>) (FeatureSwitch)new FeatureSwitch<>(Supplier.class);

    public static final Supplier<String> featureSupplier = featureSwitch.get();

    public static final Supplier<String> plainSupplier = () -> dynamicFeature.get();

    public static final Supplier<String> constSupplier = () -> "";

    static {

        try {
            MethodHandle methodHandle = MethodHandles.lookup().findVirtual(Supplier.class, "get", MethodType.methodType(Object.class));

            Supplier<String> stringSupplier = () -> "hello";
            dynamicFeature = stringSupplier;
            featureSwitch.switchTarget(methodHandle.bindTo(stringSupplier));
            Thread thread = new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(100);
                        String s = UUID.randomUUID().toString();
                        Supplier<String> supplier = new Supplier<>() {
                            @Override
                            public String get() {
                                return s;
                            }
                        };
                        MethodHandle mh = methodHandle.bindTo(supplier);
                        dynamicFeature = supplier;
                        featureSwitch.switchTarget(mh);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }


    }

    @Benchmark
    public void testFeatureSwitch(Blackhole bh)
    {
        bh.consume(featureSupplier.get());
    }

    @Benchmark
    public void testPlain(Blackhole bh)
    {
        bh.consume(plainSupplier.get());
    }

    @Benchmark
    public void testConst(Blackhole bh)
    {
        bh.consume(constSupplier.get());
    }
}
