package io.github.dreamlike.stableValue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class FeatureSwitch<T> {
    private final MethodType methodType;
    private final Class<T> invokerInterfaceClass;
    private final MutableCallSite callSite;

    private final MethodHandle callSiteInvoker;

        public FeatureSwitch(Class<T> invokerInterfaceClass) {
        if (!invokerInterfaceClass.isInterface()) {
            throw new IllegalArgumentException("Invoker interface class must be an interface");
        }
        this.invokerInterfaceClass = invokerInterfaceClass;
        Method method = getUniqueMethod(invokerInterfaceClass);
        this.methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
        this.callSite = new MutableCallSite(methodType);
        this.callSiteInvoker = callSite.dynamicInvoker();
    }

    private static boolean isObjectMethod(Method m) {
        return switch (m.getName()) {
            case "toString" -> m.getReturnType() == String.class
                               && m.getParameterCount() == 0;
            case "hashCode" -> m.getReturnType() == int.class
                               && m.getParameterCount() == 0;
            case "equals" -> m.getReturnType() == boolean.class
                             && m.getParameterCount() == 1
                             && m.getParameterTypes()[0] == Object.class;
            default -> false;
        };
    }

    public T get() {
            //hidden class 保证trusted final
        return MethodHandleProxies.asInterfaceInstance(invokerInterfaceClass, callSiteInvoker);
    }

    public void switchTarget(MethodHandle methodHandle) {
        if (methodType.equals(methodHandle.type())) {
            callSite.setTarget(methodHandle);
            MutableCallSite.syncAll(new MutableCallSite[] { callSite });
            return;
        }
        throw new IllegalArgumentException("Method handle type must match feature switch type");
    }

    private Method getUniqueMethod(Class<T> invokerInterfaceClass) {
        Method uniqueMethod = null;
        for (Method m : invokerInterfaceClass.getMethods()) {
            if (!Modifier.isAbstract(m.getModifiers()))
                continue;

            if (isObjectMethod(m))
                continue;

            if (uniqueMethod != null) {
                throw new IllegalArgumentException("More than one abstract method in interface " + invokerInterfaceClass);
            }
            uniqueMethod = m;
        }

        if (uniqueMethod == null) {
            throw new IllegalArgumentException("No abstract method in interface " + invokerInterfaceClass);
        }

        return uniqueMethod;
    }


}