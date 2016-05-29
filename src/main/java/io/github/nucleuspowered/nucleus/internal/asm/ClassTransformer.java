/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.asm;

import org.objectweb.asm.*;
import org.slf4j.Logger;

import java.util.function.Function;

/**
 * Takes the API keys class and tries to re-purpose the fake keys.
 */
public class ClassTransformer {

    private final Logger logger;
    private static boolean loaded = false;

    public ClassTransformer (Logger logger) {
        this.logger = logger;
    }

    /**
     * Loads transformed classes for Nucleus.
     *
     * @throws Exception Thrown if classes could not be transformed.
     */
    public void loadClasses() throws Exception {
        if (loaded) {
            logger.error("Class transformations have already been performed.");
            return;
        }

        logger.info("Starting Nucleus Class transformation...");
        loadKeysClass();
        loaded = true;
    }

    private void loadKeysClass() throws Exception {
        transformClass("io/github/nucleuspowered/nucleus/api/spongedata/NucleusKeys.class", StripFinalFromStaticFieldsClassVisitor::new);
    }

    private void transformClass(String classLocation, Function<ClassWriter, ClassVisitor> cv) throws Exception {
        String binaryClass = classLocation.replaceAll("\\.class$", "").replaceAll("/", ".");
        ClassReader reader = new ClassReader(getClass().getClassLoader().getResourceAsStream("io/github/nucleuspowered/nucleus/api/spongedata/NucleusKeys.class"));
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS & ClassWriter.COMPUTE_FRAMES);

        reader.accept(cv.apply(writer), 0);
        loadClass(binaryClass, writer.toByteArray());
    }

    @SuppressWarnings("all")
    private Class loadClass(String className, byte[] b) throws Exception {
        //override classDefine (as it is protected) and define the class.
        Class clazz = null;
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        Class cls = Class.forName("java.lang.ClassLoader");
        java.lang.reflect.Method method =
                cls.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class, int.class });

        // protected method invocaton
        method.setAccessible(true);
        try {
            logger.info("Loading transformed class \"{}\"...", className);
            Object[] args = new Object[] { className, b, new Integer(0), new Integer(b.length)};
            clazz = (Class) method.invoke(loader, args);
        } finally {
            method.setAccessible(false);
        }

        return clazz;
    }

    /**
     * A {@link ClassVisitor} that removes final keywords from static fields.
     */
    private class StripFinalFromStaticFieldsClassVisitor extends ClassVisitor {

        private StripFinalFromStaticFieldsClassVisitor(ClassVisitor cv) {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public FieldVisitor visitField(int access, String fieldName, String s1, String s2, Object o) {
            if ((access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
                logger.debug("Removing final modifier from {}", fieldName);
                return super.visitField(access & ~Opcodes.ACC_FINAL, fieldName, s1, s2, o);
            }

            return super.visitField(access, fieldName, s1, s2, o);
        }
    }
}
