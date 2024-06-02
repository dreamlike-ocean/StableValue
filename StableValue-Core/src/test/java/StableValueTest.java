import io.github.dreamlike.stableValue.DirectLambdaFactory;
import io.github.dreamlike.stableValue.StableValue;
import org.junit.Assert;
import org.junit.Test;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;

public class StableValueTest {

    @Test
    public void testStableValue() {
        StableValue<String> value = StableValue.of(() -> UUID.randomUUID().toString());
        Assert.assertEquals(value.get(), value.get());
        StableValue.setMode(true);
        StableValue<String> hidden = StableValue.of(() -> UUID.randomUUID().toString());
        Assert.assertEquals(hidden.get(), hidden.get());
        Assert.assertTrue(hidden.getClass().isHidden());
    }

    @Test
    public void testLambdaFactory() throws NoSuchMethodException, IllegalAccessException, LambdaConversionException {
        MethodHandle mh = MethodHandles.lookup()
                .findStatic(Integer.class, "valueOf", MethodType.methodType(Integer.class, String.class));
        Function<String, Integer> function = DirectLambdaFactory.generate(Function.class, mh);
        String str = "123";
        Assert.assertEquals(function.apply(str), Integer.valueOf(str));

        mh = MethodHandles.lookup()
                .findVirtual(String.class, "length", MethodType.methodType(int.class));
        str = "123";

        MapToInt<String> intFunction = DirectLambdaFactory.generate(MapToInt.class, mh);
        Assert.assertEquals(intFunction.apply(str), str.length());

        mh = MethodHandles.lookup()
                .findStatic(StableValueTest.class, "doubleParam", MethodType.methodType(int.class, String.class, String.class));
        mh = mh.bindTo(str);
        intFunction = DirectLambdaFactory.generate(MapToInt.class, mh);
        Assert.assertEquals(intFunction.apply(str), doubleParam(str, str));
        MethodHandle finalMh = mh;
        LambdaConversionException lambdaConversionException = Assert.assertThrows(LambdaConversionException.class, () -> LambdaMetafactory.metafactory(
                        MethodHandles.lookup(),
                        "apply",
                        MethodType.methodType(MapToInt.class, String.class),
                        MethodType.methodType(int.class, Object.class),
                        finalMh,
                        MethodType.methodType(int.class, Object.class)
                )
        );
        Assert.assertTrue(lambdaConversionException.getMessage().contains("cracked"));


        mh = MethodHandles.lookup()
                .findStatic(StableValueTest.class, "doubleParam", MethodType.methodType(int.class, String.class, String.class));
        MethodHandle finalMh1 = mh;
        Assert.assertThrows(IllegalArgumentException.class, () -> DirectLambdaFactory.generate(MapToInt.class, finalMh1));
    }

    public static int doubleParam(String str, String s1) {
        return str.length() + s1.length();
    }

    public interface MapToInt<T> {
        int apply(T t);
    }


}
