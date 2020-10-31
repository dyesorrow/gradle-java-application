/*********************************************************************
 * java 类相关操作工具
 * @Author: huxiaomin 
 * @Date: 2020-10-05 19:38:16 
 * @Last Modified by: huxiaomin
 * @Last Modified time: 2020-10-05 19:39:53
*********************************************************************/
package com.vito.common.type;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.util.ParameterizedTypeImpl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import lombok.Setter;
import lombok.experimental.Accessors;

public class ClassUtil {

    /**
     * 取消 java11 cglib 警告
     */
    @SuppressWarnings("all")
    public static void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
            Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
            putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception ignored) {
        }
    }

    private static class ClassReaderPlus extends ClassVisitor {

        private ClassReader reader;

        @Setter
        @Accessors(chain = true)
        private String methodName;
        private String[] paramNames;

        public ClassReaderPlus(Class<?> clazz) throws IOException {
            super(Opcodes.ASM5);
            this.reader = new ClassReader(clazz.getName());
        }

        public String[] getMethodParamNames(Method method) {
            this.paramNames = new String[method.getParameterCount()];
            this.reader.accept(this, 0);
            return paramNames;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (this.methodName.equals(name)) {
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                        paramNames[index] = name;
                    }
                };
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    private static Map<Class<?>, ClassReaderPlus> classReaders = new HashMap<>();

    /**
     * 获取函数参数名称
     * 
     * @param clazz
     * @param method
     * @return
     * @throws IOException
     */
    public static String[] getMethondParams(Class<?> clazz, Method method) throws IOException {
        if (!classReaders.containsKey(clazz)) {
            classReaders.put(clazz, new ClassReaderPlus(clazz));
        }
        return classReaders.get(clazz).getMethodParamNames(method);
    }

    /**
     * 获取泛型类型
     * @param type
     * @param at
     * @return
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("all")
    public static Class getGenericType(Type type, int at) throws ClassNotFoundException {
        ParameterizedType ptype = (ParameterizedType) type;
        ParameterizedTypeImpl ptypeImpl = new ParameterizedTypeImpl(ptype.getActualTypeArguments(), ptype.getOwnerType(), ptype.getRawType());
        Type[] genericTypes = ptypeImpl.getActualTypeArguments();
        return Class.forName(genericTypes[at].getTypeName());
    }

}
