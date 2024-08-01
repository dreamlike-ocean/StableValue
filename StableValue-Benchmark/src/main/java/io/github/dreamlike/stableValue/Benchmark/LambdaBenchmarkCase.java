package io.github.dreamlike.stableValue.Benchmark;

import io.github.dreamlike.stableValue.DirectLambdaFactory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 200, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.Throughput)
@Threads(value = 5)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
public class LambdaBenchmarkCase {

    private static final String baseSTR = UUID.randomUUID().toString().repeat(1024);

    private static final List<String> params;

    public static final Function<String, Integer> jdkLambda = (s1) -> s1.length() + baseSTR.length();
    public static final Function<String, Integer> directLambda;

    public static final Function<String, Integer> jdkMethodHandleProxy;

    public static final Function<String, Integer> jdkMethodHandleWorkAround;
    
    @Param({"10", "100", "1000"})
    public int COUNT;

    static {
        try {
            MethodHandle methodHandle = MethodHandles.lookup().findStatic(LambdaBenchmarkCase.class, "testMethod", MethodType.methodType(int.class, String.class, String.class));
            MethodHandle realMh = MethodHandles.filterReturnValue(
                    methodHandle.bindTo(baseSTR),
                    MethodHandles.lookup()
                            .findStatic(Integer.class, "valueOf", MethodType.methodType(Integer.class, int.class))
            );
            directLambda = DirectLambdaFactory.generate(Function.class,
                    realMh
            );

            jdkMethodHandleProxy = MethodHandleProxies.asInterfaceInstance(Function.class, realMh);

            jdkMethodHandleWorkAround = (Function<String, Integer>) LambdaMetafactory
                    .metafactory(
                            MethodHandles.lookup(),
                            "apply",
                            MethodType.methodType(Function.class, MethodHandle.class),
                            MethodType.methodType(Object.class, Object.class),
                            MethodHandles.exactInvoker(realMh.type()),
                           realMh.type()
                    ).getTarget()
                    .invokeExact(realMh);

            params = IntStream.range(0, 100)
                    .mapToObj(s -> UUID.randomUUID().toString())
                    .toList();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void testJDKLambda(Blackhole bh) {
        for (int i = 0; i < COUNT; i++) {
            for (String param : params) {
                bh.consume(jdkLambda.apply(param));
            }

        }
    }

    @Benchmark
    public void testDirectLambda(Blackhole bh) {
        for (int i = 0; i < COUNT; i++) {
            for (String param : params) {
                bh.consume(directLambda.apply(param));
            }
        }
    }


    @Benchmark
    public void testjdkMethodHandleProxy(Blackhole bh) {
        for (int i = 0; i < COUNT; i++) {
            for (String param : params) {
                bh.consume(jdkMethodHandleProxy.apply(param));
            }
        }
    }



    @Benchmark
    public void testjdkexactInvoker(Blackhole bh) {
        for (int i = 0; i < COUNT; i++) {
            for (String param : params) {
                bh.consume(jdkMethodHandleWorkAround.apply(param));
            }
        }
    }


    public static int testMethod(String s1, String s2) {
        return s1.length() + s2.length();
    }
}
