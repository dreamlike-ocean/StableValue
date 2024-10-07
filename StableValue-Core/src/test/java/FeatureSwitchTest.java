import io.github.dreamlike.stableValue.FeatureSwitch;
import org.junit.Assert;
import org.junit.Test;

import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.function.Supplier;

public class FeatureSwitchTest {

    @Test
    public void testSwitch() {
        @SuppressWarnings(value = {"unchecked", "rawtypes"})
        FeatureSwitch<Supplier<String>> featureSwitch = (FeatureSwitch<Supplier<String>>) (FeatureSwitch)new FeatureSwitch<>(Supplier.class);
        Supplier<String> supplier = featureSwitch.get();
        Assert.assertTrue(supplier.getClass().isHidden());

        String s1 = UUID.randomUUID().toString();
        featureSwitch.switchTarget(MethodHandles.constant(Object.class,s1));
        Assert.assertEquals(s1, supplier.get());

        String s2 = UUID.randomUUID().toString();
        featureSwitch.switchTarget(MethodHandles.constant(Object.class,s2));
        Assert.assertEquals(s2, supplier.get());
    }
}
