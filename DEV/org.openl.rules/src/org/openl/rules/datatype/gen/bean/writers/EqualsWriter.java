package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.openl.rules.asm.invoker.EqualsBuilderInvoker;
import org.openl.rules.datatype.gen.FieldDescription;

public class EqualsWriter extends MethodWriter {

    /**
     * 
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *            (e.g. <code>my/test/TestClass</code>)
     * @param allFields collection of fields for current class and parent`s ones.
     */
    public EqualsWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, allFields);
    }

    public void write(ClassWriter classWriter) {
        MethodVisitor methodVisitor;
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);

        Label comparingLabel = new Label();

        // check "instance of" object
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, getBeanNameWithPackage());
        methodVisitor.visitJumpInsn(Opcodes.IFNE, comparingLabel);
        methodVisitor.visitLdcInsn(Boolean.FALSE);
        methodVisitor.visitInsn(Opcodes.IRETURN);
        methodVisitor.visitLabel(comparingLabel);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        // create EqualsBuilder
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(EqualsBuilder.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor
            .visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(EqualsBuilder.class), "<init>", "()V");

        // cast
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, getBeanNameWithPackage());
        methodVisitor.visitVarInsn(Opcodes.ASTORE, 2);

        // comparing by fields
        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            pushFieldToStack(methodVisitor, 0, field.getKey());
            pushFieldToStack(methodVisitor, 2, field.getKey());

            Class<?> fieldType = field.getValue().getType();
            EqualsBuilderInvoker.getAppend(fieldType).invoke(methodVisitor);
        }

        EqualsBuilderInvoker.getIsEquals().invoke(methodVisitor);

        methodVisitor.visitInsn(Opcodes.IRETURN);
        methodVisitor.visitMaxs(0, 0);
    }
}
