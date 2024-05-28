package io.github.dreamlike.stableValue;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.ClassFile;
import java.lang.classfile.TypeKind;
import java.lang.constant.*;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;


class StableValueGenerator {

    private static final ClassFile classFile = ClassFile.of();

    static final String FACTORY_FIELD_NAME = "FACTORY_FIELD";

    private static final AtomicInteger count = new AtomicInteger();

    public static <T> StableValue<T> of(Supplier<T> factory) {
        String className = StableValue.class.getName() + "Impl" + count.getAndIncrement();

        byte[] classByteCode = classFile.build(ClassDesc.of(className), cb -> {
            cb.withInterfaceSymbols(ClassDesc.of(StableValue.class.getName()));
            cb.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
                it.aload(0);
                it.invokespecial(ClassDesc.of(Object.class.getName()), "<init>", MethodTypeDesc.ofDescriptor("()V"));
                it.return_();
            });

            cb.withField(FACTORY_FIELD_NAME, ClassDesc.of(Supplier.class.getName()), AccessFlags.ofField(AccessFlag.PUBLIC).flagsMask() | AccessFlags.ofField(AccessFlag.STATIC).flagsMask() | AccessFlags.ofField(AccessFlag.SYNTHETIC).flagsMask());

            cb.withMethodBody(
                    "get",
                    MethodTypeDesc.of(ClassDesc.of(Object.class.getName())),
                    AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask() | AccessFlags.ofMethod(AccessFlag.SYNTHETIC).flagsMask(),
                    it -> {
                        it.invokeDynamicInstruction(
                                DynamicCallSiteDesc.of(
                                        MethodHandleDesc.ofMethod(
                                                DirectMethodHandleDesc.Kind.STATIC, ClassDesc.of(StableValueGenerator.class.getName()), "indyFactory",
                                                MethodTypeDesc.of(
                                                        ClassDesc.of(ConstantCallSite.class.getName()),
                                                        ClassDesc.of(MethodHandles.Lookup.class.getName()), ClassDesc.of(String.class.getName()), ClassDesc.of(MethodType.class.getName()), ClassDesc.ofDescriptor("[Ljava/lang/Object;")
                                                )
                                        ),
                                        "get",
                                        MethodTypeDesc.of(ClassDesc.of(Object.class.getName()))
                                )
                        );
                        it.returnInstruction(TypeKind.ReferenceType);
                    }
            );
        });
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            Class<?> aClass = lookup.defineClass(classByteCode);
            aClass.getField(FACTORY_FIELD_NAME).set(null, factory);
            return ((StableValue) aClass.newInstance());
        } catch (IllegalAccessException | InstantiationException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static ConstantCallSite indyFactory(MethodHandles.Lookup lookup, String name, MethodType type, Object... args) throws NoSuchFieldException, IllegalAccessException {
        Supplier supplier = (Supplier) lookup.lookupClass().getField(FACTORY_FIELD_NAME).get(null);
        return new ConstantCallSite(MethodHandles.constant(type.returnType(), supplier.get()));
    }

    public static ConstantCallSite indyLambdaFactory(MethodHandles.Lookup lookup, String name, MethodType type, Object... args) throws NoSuchFieldException, IllegalAccessException {
        MethodHandle supplier = (MethodHandle) lookup.findStaticVarHandle(lookup.lookupClass(), DirectLambdaFactory.MH_FIELD_NAME, MethodHandle.class).get();
        return new ConstantCallSite(supplier);
    }

}
