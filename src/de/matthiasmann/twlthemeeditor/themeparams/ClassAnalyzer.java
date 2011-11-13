/*
 * Copyright (c) 2008-2011, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twlthemeeditor.themeparams;

import java.util.ArrayList;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.objectweb.asm.tree.analysis.Value;

/**
 *
 * @author Matthias Mann
 */
public class ClassAnalyzer implements ClassVisitor {
    
    private final ArrayList<MethodNode> methods;
    private ClassThemeParamInfo classInfo;

    public ClassAnalyzer() {
        methods = new ArrayList<MethodNode>();
    }
    
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        classInfo = new ClassThemeParamInfo(name, superName);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    @Override
    public void visitAttribute(Attribute attr) {
    }
    
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return null;
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if(name.startsWith("applyTheme") && "(Lde/matthiasmann/twl/ThemeInfo;)V".equals(desc)) {
            MethodNode mn = new MethodNode(access, name, desc, signature, exceptions);
            methods.add(mn);
            return mn;
        } else {
            return null;
        }
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
    }

    @Override
    public void visitSource(String source, String debug) {
    }

    @Override
    public void visitEnd() {
        for(MethodNode mn : methods) {
            processMethod(mn);
        }
    }

    public ClassThemeParamInfo getClassInfo() {
        return classInfo;
    }

    private void processMethod(MethodNode mn) {
        Analyzer analyzer = new Analyzer(new SourceInterpreter());
        try {
            Frame[] frames = analyzer.analyze(classInfo.getClassName(), mn);
            for(int insIdx=0 ; insIdx<mn.instructions.size() ; insIdx++) {
                AbstractInsnNode ins = mn.instructions.get(insIdx);
                if(ins instanceof MethodInsnNode) {
                    MethodInsnNode invoke = (MethodInsnNode)ins;
                    if("de/matthiasmann/twl/ThemeInfo".equals(invoke.owner)) {
                        Type[] argTypes = Type.getArgumentTypes(invoke.desc);
                        Type returnType = Type.getReturnType(invoke.desc);
                        if(argTypes.length >= 1 &&
                                "java/lang/String".equals(argTypes[0].getInternalName()) &&
                                returnType.getSort() != Type.VOID) {
                            Frame frame = frames[insIdx];
                            Object[] args = new Object[argTypes.length];
                            for(int i=0 ; i<argTypes.length ; i++) {
                                args[i] = getValue(frame.getStack(frame.getStackSize() - argTypes.length + i));
                            }
                            ThemeParamInfo tp = ThemeParamInfo.create(invoke.name, returnType, args);
                            if(tp != null) {
                                classInfo.addParam(tp);
                            }
                        }
                    }
                }
            }
        } catch(AnalyzerException ex) {
            ex.printStackTrace();
        }
    }
    
    private Object getValue(Value value) {
        if(value instanceof SourceValue) {
            SourceValue sourceValue = (SourceValue)value;
            if(sourceValue.insns.size() == 1) {
                AbstractInsnNode insn = (AbstractInsnNode)sourceValue.insns.iterator().next();
                switch(insn.getOpcode()) {
                    case Opcodes.ICONST_0: return 0;
                    case Opcodes.ICONST_1: return 1;
                    case Opcodes.ICONST_2: return 2;
                    case Opcodes.ICONST_3: return 3;
                    case Opcodes.ICONST_4: return 4;
                    case Opcodes.ICONST_5: return 5;
                    case Opcodes.ICONST_M1: return -1;
                    case Opcodes.LCONST_0: return 0;
                    case Opcodes.LCONST_1: return 1;
                    case Opcodes.ACONST_NULL: return null;
                    case Opcodes.LDC: return ((LdcInsnNode)insn).cst;
                    case Opcodes.BIPUSH: return ((IntInsnNode)insn).operand;
                    case Opcodes.SIPUSH: return ((IntInsnNode)insn).operand;
                    case Opcodes.GETSTATIC:
                        FieldInsnNode fieldInsn = (FieldInsnNode)insn;
                        return new StaticFieldAccess(fieldInsn.owner, fieldInsn.name);
                }
            }
        }
        return null;
    }
}
