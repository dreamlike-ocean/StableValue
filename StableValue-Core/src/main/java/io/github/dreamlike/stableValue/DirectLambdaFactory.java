package io.github.dreamlike.stableValue;

import java.io.IOException;
import java.lang.classfile.AccessFlags;
import java.lang.classfile.ClassFile;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectLambdaFactory {

    static final String MH_FIELD_NAME = "MH_KEY";

    static final ClassFile classFile = ClassFile.of();

    private static final AtomicInteger count = new AtomicInteger();

    public static <T> T generate(Class<T> samInterface, MethodHandle target) throws RuntimeException {
        Method method = samMT(samInterface);
        if (method == null) {
            throw new IllegalArgumentException("Class " + samInterface.getName() + " is not a SAM interface");
        }
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        String className = lookup.lookupClass().getName() + samInterface.getSimpleName() + "Impl" + count.getAndIncrement();
        byte[] classByteCode = classFile.build(ClassDesc.of(className), cb -> {
            cb.withInterfaceSymbols(ClassDesc.of(samInterface.getName()));
            cb.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
                it.aload(0);
                it.invokespecial(ClassDesc.of(Object.class.getName()), "<init>", MethodTypeDesc.ofDescriptor("()V"));
                it.return_();
            });

            cb.withField(MH_FIELD_NAME, ClassDesc.of(MethodHandle.class.getName()), AccessFlags.ofField(AccessFlag.PUBLIC).flagsMask() | AccessFlags.ofField(AccessFlag.STATIC).flagsMask() | AccessFlags.ofField(AccessFlag.SYNTHETIC).flagsMask());

            List<ClassDesc> paramDesc = Arrays.stream(method.getParameters())
                    .map(Parameter::getType)
                    .map(DirectLambdaFactory::toDesc)
                    .toList();

            cb.withMethodBody(
                    method.getName(),
                    MethodTypeDesc.of(toDesc(method.getReturnType()), paramDesc),
                    AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask() | AccessFlags.ofMethod(AccessFlag.SYNTHETIC).flagsMask(),
                    it -> {
                        Parameter[] parameters = method.getParameters();
                        int nextSlot = 1;
                        for (int i = 0; i < parameters.length; i++) {
                            TypeKind typeKind = calType(parameters[i].getType());
                            it.loadInstruction(typeKind, nextSlot);
                            nextSlot += typeKind.slotSize();
                        }
                        it.invokeDynamicInstruction(
                                DynamicCallSiteDesc.of(
                                        MethodHandleDesc.ofMethod(
                                                DirectMethodHandleDesc.Kind.STATIC, ClassDesc.of(StableValueGenerator.class.getName()), "indyLambdaFactory",
                                                MethodTypeDesc.of(
                                                        ClassDesc.of(ConstantCallSite.class.getName()),
                                                        ClassDesc.of(MethodHandles.Lookup.class.getName()), ClassDesc.of(String.class.getName()), ClassDesc.of(MethodType.class.getName()), ClassDesc.ofDescriptor("[Ljava/lang/Object;")
                                                )
                                        ),
                                        method.getName(),
                                        MethodTypeDesc.of(toDesc(method.getReturnType()), paramDesc),
                                        className
                                )
                        );
                        it.returnInstruction(calType(method.getReturnType()));
                    }
            );
        });
        try {
            Class<?> aClass = lookup.defineClass(classByteCode);
            lookup.ensureInitialized(aClass);
            aClass.getField(MH_FIELD_NAME).set(null, adjustMethodHandle(method, target));
            return (T) aClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

    }

    private static Method samMT(Class<?> clazz) {
        if (!clazz.isInterface()) {
            return null;
        }
        int amCount = 0;
        Method methodType = null;
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (Modifier.isAbstract(method.getModifiers())) {
                amCount++;

                methodType = method;
            }
        }
        if (amCount == 1) {
            return methodType;
        }
        return null;
    }

    private static ClassDesc toDesc(Class a) {
        if (a.isArray()) {
            return ClassDesc.of(a.componentType().getName()).arrayType();
        }
        if (!a.isPrimitive()) {
            return ClassDesc.of(a.getName());
        }
        return ClassDesc.ofDescriptor(calType(a).descriptor());
    }

    private static TypeKind calType(Class type) {
        return switch (type.getName()) {
            case "int" -> TypeKind.IntType;
            case "long" -> TypeKind.LongType;
            case "float" -> TypeKind.FloatType;
            case "double" -> TypeKind.DoubleType;
            case "boolean" -> TypeKind.BooleanType;
            case "byte" -> TypeKind.ByteType;
            case "char" -> TypeKind.CharType;
            case "short" -> TypeKind.ShortType;
            case "void" -> TypeKind.VoidType;
            default -> TypeKind.ReferenceType;
        };
    }

    private static MethodHandle adjustMethodHandle(Method sam, MethodHandle mh) {
        MethodType type = mh.type();
        type = type.erase();
        if (type.parameterCount() != sam.getParameterCount()) {
            throw new IllegalArgumentException("MethodHandle parameter count not match");
        }
        if (type.returnType() != sam.getReturnType()) {
            throw new IllegalArgumentException("MethodHandle return type not match");
        }
        MethodType interfaceSamType = MethodType.methodType(sam.getReturnType(), sam.getParameterTypes());
        if (interfaceSamType.equals(type)) {
            return mh.asType(type);
        }
        throw new IllegalArgumentException("MethodHandle type not match");
    }

}
