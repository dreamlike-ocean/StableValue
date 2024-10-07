package io.github.dreamlike.stableValue;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.ClassFile;
import java.lang.constant.*;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.lang.constant.ConstantDescs.*;


class StableValueGenerator {

    static final String FACTORY_FIELD_NAME = "FACTORY_FIELD";
    private static final ClassFile classFile = ClassFile.of();
    private static final AtomicInteger count = new AtomicInteger();
    private static final MethodTypeDesc INDY_MTD;
    public static boolean enable_hidden = false;
    public static boolean enable_condy = false;

    static {
        try {
            Method indyFactory = StableValueGenerator.class.getMethod("indyFactory", MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class);
            INDY_MTD = MethodType.methodType(indyFactory.getReturnType(), indyFactory.getParameterTypes())
                    .describeConstable().get();

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> StableValue<T> of(Supplier<T> factory) {
        String className = StableValue.class.getName() + "Impl" + count.getAndIncrement();
        boolean enableHidden = enable_hidden;
        byte[] classByteCode = classFile.build(ClassDesc.of(className), cb -> {
            cb.withInterfaceSymbols(ClassDesc.of(StableValue.class.getName()));
            cb.withMethodBody(ConstantDescs.INIT_NAME, ConstantDescs.MTD_void, AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
                it.aload(0);
                it.invokespecial(CD_Object, INIT_NAME, MTD_void);
                it.return_();
            });
            if (!enableHidden) {
                cb.withField(FACTORY_FIELD_NAME, ClassDesc.of(Supplier.class.getName()), AccessFlags.ofField(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.SYNTHETIC).flagsMask());
            }

            cb.withMethodBody(
                    "get",
                    MethodTypeDesc.of(ClassDesc.of(Object.class.getName())),
                    AccessFlags.ofMethod(AccessFlag.PUBLIC, AccessFlag.SYNTHETIC).flagsMask(),
                    it -> {
                        if (enable_condy) {

                            it.loadConstant((
                                    DynamicConstantDesc.of(
                                            ofConstantBootstrap(StableValueGenerator.class.describeConstable().get(), "condyFactory", Object.class.describeConstable().get())
                                    )));

                        } else {
                            it.invokedynamic(
                                    DynamicCallSiteDesc.of(
                                            MethodHandleDesc.ofMethod(
                                                    DirectMethodHandleDesc.Kind.STATIC, StableValueGenerator.class.describeConstable().get(), "indyFactory",
                                                    INDY_MTD
                                            ),
                                            "get",
                                            MethodType.methodType(Object.class).describeConstable().get()
                                    )
                            );
                        }
                        it.areturn();
                    }
            );
        });
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            Class<?> aClass = enable_hidden
                    ? lookup.defineHiddenClassWithClassData(classByteCode, factory, false).lookupClass()
                    : lookup.defineClass(classByteCode);
            if (!enableHidden) {
                aClass.getField(FACTORY_FIELD_NAME).set(null, factory);
            }
            return ((StableValue) aClass.newInstance());
        } catch (IllegalAccessException | InstantiationException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static ConstantCallSite indyFactory(MethodHandles.Lookup lookup, String name, MethodType type, Object... args) throws NoSuchFieldException, IllegalAccessException {
        Class<?> aClass = lookup.lookupClass();
        Supplier supplier = aClass.isHidden()
                ? MethodHandles.classData(lookup, DEFAULT_NAME, Supplier.class)
                : (Supplier) aClass.getField(FACTORY_FIELD_NAME).get(null);
        ;
        return new ConstantCallSite(MethodHandles.constant(type.returnType(), supplier.get()));
    }

    public static Object condyFactory(MethodHandles.Lookup lookup, String name, Class type) throws NoSuchFieldException, IllegalAccessException {
        Class<?> aClass = lookup.lookupClass();
        Supplier supplier = aClass.isHidden()
                ? MethodHandles.classData(lookup, DEFAULT_NAME, Supplier.class)
                : (Supplier) aClass.getField(FACTORY_FIELD_NAME).get(null);
        ;
        return supplier.get();
    }
}
