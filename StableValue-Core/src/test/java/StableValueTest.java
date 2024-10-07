
import io.github.dreamlike.stableValue.StableValue;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class StableValueTest {

    @Test
    public void testStableValue() {
        StableValue<String> value = StableValue.of(() -> UUID.randomUUID().toString());
        Assert.assertEquals(value.get(), value.get());
        StableValue.setHiddenMode(true);
        StableValue<String> hidden = StableValue.of(() -> UUID.randomUUID().toString());
        Assert.assertEquals(hidden.get(), hidden.get());
        Assert.assertTrue(hidden.getClass().isHidden());

        StableValue.setBackend(StableValue.BackEnd.CONDY);
        StableValue<String> condy = StableValue.of(() -> UUID.randomUUID().toString());
        Assert.assertEquals(condy.get(), condy.get());
    }



    public static int doubleParam(String str, String s1) {
        return str.length() + s1.length();
    }

    public interface MapToInt<T> {
        int apply(T t);
    }


}
