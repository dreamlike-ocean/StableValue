package io.github.dreamlike.stableValue.Benchmark;

import io.github.dreamlike.stableValue.StableValue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Warmup(iterations = 5, time = 200, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.Throughput)
@Threads(value = 5)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
public class StableValueListBenchmarkCase {

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


    private static final List<StableValue<String>> dcl_List = List.of(
            new StableValueBenchmarkCase.DCLStableValue<>(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            new StableValueBenchmarkCase.DCLStableValue<>(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            new StableValueBenchmarkCase.DCLStableValue<>(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            new StableValueBenchmarkCase.DCLStableValue<>(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            }),
            new StableValueBenchmarkCase.DCLStableValue<>(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                return UUID.randomUUID().toString();
            })
    );




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
}
