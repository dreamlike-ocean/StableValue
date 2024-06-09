package io.github.dreamlike.stableValue.Benchmark;

import io.github.dreamlike.stableValue.DirectLambdaFactory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Warmup(iterations = 5, time = 200, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.Throughput)
@Threads(value = 5)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
public class LambdaBenchmarkCase {

    private static final String baseSTR = UUID.randomUUID().toString().repeat(1024);

    public static final Function<String, Integer> jdkLambda = (s1) -> Integer.valueOf(s1.length() + baseSTR.length());
    public static final Function<String, Integer> directLambda;

    public static final Function<String, Integer> jdkMethodHandleProxy;

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
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
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
    public void testjdkMethodHandleProxy(Blackhole bh) {
        for (int i = 0; i < 500_00; i++) {
            bh.consume(jdkMethodHandleProxy.apply("123"));
        }
    }



    public static int testMethod(String s1, String s2) {
        return s1.length() + s2.length();
    }
}
