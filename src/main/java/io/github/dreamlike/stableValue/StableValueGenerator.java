package io.github.dreamlike.stableValue;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.ClassFile;
import java.lang.classfile.TypeKind;
import java.lang.constant.*;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

class StableValueGenerator {

    private static final ClassFile classFile = ClassFile.of();

    private static ConcurrentHashMap<String, Supplier<Object>> factories = new ConcurrentHashMap<>();

    private static final AtomicInteger count = new AtomicInteger();

    public static <T> StableValue<T> of(Supplier<T> factory) {
        String className = StableValue.class.getName() + "Impl" + count.getAndIncrement();
        factories.put(className, factory::get);
        byte[] classByteCode = classFile.build(ClassDesc.of(className), cb -> {
            cb.withInterfaceSymbols(ClassDesc.of(StableValue.class.getName()));
            cb.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
                it.aload(0);
                it.invokespecial(ClassDesc.of(Object.class.getName()), "<init>", MethodTypeDesc.ofDescriptor("()V"));
                it.return_();
            });

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
                                        MethodTypeDesc.of(ClassDesc.of(Object.class.getName())),
                                        className
                                )
                        );
                        it.returnInstruction(TypeKind.ReferenceType);
                    }
            );
        });
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            Class<?> aClass = lookup.defineClass(classByteCode);
            return ((StableValue) aClass.newInstance());
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public static ConstantCallSite indyFactory(MethodHandles.Lookup lookup, String name, MethodType type, Object... args) {
        String key = (String) args[0];
        Supplier<Object> supplier = factories.get(key);
        if (supplier == null) {
            throw new IllegalArgumentException("No factory found for key: " + key);
        }
        return new ConstantCallSite(MethodHandles.constant(type.returnType(), supplier.get()));
    }

}
