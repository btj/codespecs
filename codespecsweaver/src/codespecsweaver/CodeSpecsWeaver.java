package codespecsweaver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.Method;

public class CodeSpecsWeaver implements ClassFileTransformer {

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new CodeSpecsWeaver());
    }
    
    static Type postconditionTypeForReturnType(Type returnType) {
    	if (returnType == Type.VOID_TYPE)
    		return Type.getObjectType("java/lang/Runnable");
    	else
    		return Type.getObjectType("java/util/function/Consumer");
    }
    
    class WrapperMethodInfo {
		final Type[] wrappeeArgumentTypes;
		final Type wrappeeReturnType;
		final Type[] wrapperArgumentTypes;
		final String wrapperDesc;
		final String wrapperName;

		WrapperMethodInfo(String owner, String name, String desc) {
			wrappeeArgumentTypes = Type.getArgumentTypes(desc);
			wrappeeReturnType = Type.getReturnType(desc);
			wrapperArgumentTypes = new Type[wrappeeArgumentTypes.length + 1];
			int i = 0;
			wrapperArgumentTypes[i++] = Type.getObjectType(owner);
			for (Type argType : wrappeeArgumentTypes)
				wrapperArgumentTypes[i++] = argType;
			wrapperDesc = Type.getMethodDescriptor(wrappeeReturnType, wrapperArgumentTypes);
			wrapperName = name + "$codespecs";
		}
		
		Method getMethod() {
			return new Method(wrapperName, wrapperDesc);
		}
    }
    
    static class ClassInfo {
    	boolean hasSeeCodeSpecsAnnotation;
    	String superclassName;
    	ArrayList<Method> instanceMethods = new ArrayList<>();
    	ArrayList<Method> staticMethods = new ArrayList<>();
    	
    	ClassInfo(ClassLoader classLoader, String className) {
			String resourceName = className+".class";
    		try {
	    		InputStream is = classLoader.getResourceAsStream(resourceName);
	    		if (is != null) {
		    		ClassReader reader = new ClassReader(is);
		    		ClassVisitor v = new ClassVisitor(Opcodes.ASM5) {
		    			@Override
		    			public void visit(int version, int access, String name,
		    					String signature, String superName,
		    					String[] interfaces) {
		    				superclassName = superName;
		    			}
		    			
		    			@Override
		    			public AnnotationVisitor visitAnnotation(String desc,
		    					boolean visible) {
		    				if (desc.equals("Lcodespecs/SeeCodeSpecs;"))
		    					hasSeeCodeSpecsAnnotation = true;
		    				return null;
		    			}
		    			
		    			@Override
		    			public MethodVisitor visitMethod(int access,
		    					String name, String desc, String signature,
		    					String[] exceptions) {
		    				if ((access & Opcodes.ACC_STATIC) == 0)
		    					instanceMethods.add(new Method(name, desc));
		    				else
		    					staticMethods.add(new Method(name, desc));
		    				return null;
		    			}
		    		};
		    		reader.accept(v, ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES|ClassReader.SKIP_CODE);
		    		is.close();
	    		}
    		} catch (IOException e) {
    			throw new RuntimeException(resourceName, e);
    		}
    	}
    }
    
    HashMap<String, ClassInfo> classInfoMap = new HashMap<>(); // TODO: Use (classLoader, className) pairs as keys instead of just class names.
    
    ClassInfo getClassInfo(ClassLoader classLoader, String className) {
    	ClassInfo info = classInfoMap.get(className);
    	if (info == null) {
    		info = new ClassInfo(classLoader, className);
    		classInfoMap.put(className, info);
    	}
    	return info;
    }
    
    boolean methodHasContract(ClassLoader classLoader, String className, String name, String desc) {
    	ClassInfo info = getClassInfo(classLoader, className);
    	return info.hasSeeCodeSpecsAnnotation && info.instanceMethods.contains(new Method(name, desc));
    }
    
    Method specMethodForConstructor(String desc) {
    	return new Method("constructorSpec", Type.getMethodDescriptor(Type.getObjectType("java/util/function/Consumer"), Type.getArgumentTypes(desc)));
    }
    
    Method specMethodForInstanceMethod(String owner, String name, String desc) {
    	Type[] argumentTypes = Type.getArgumentTypes(desc);
    	Type[] specArgumentTypes = new Type[argumentTypes.length + 1];
    	int i = 0;
    	specArgumentTypes[i++] = Type.getObjectType(owner);
    	for (Type argType : argumentTypes)
    		specArgumentTypes[i++] = argType;
    	return new Method(name+"Spec", Type.getMethodDescriptor(postconditionTypeForReturnType(Type.getReturnType(desc)), specArgumentTypes));	
    }
    
    String getSpecReferrerClass(ClassLoader classLoader, String owner, String name, String desc) {
    	while (!owner.equals("java/lang/Object")) {
	    	ClassInfo classInfo = getClassInfo(classLoader, owner);
	    	if (!classInfo.hasSeeCodeSpecsAnnotation)
	    		return null;
	    	String specClassName = owner+"Spec";
	    	if (getClassInfo(classLoader, specClassName).staticMethods.contains(specMethodForInstanceMethod(owner, name, desc)))
	    		return owner;
	    	String superclassName = classInfo.superclassName;
	    	owner = superclassName;
    	}
    	return null;
    }

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
        	if (loader == null)
        		return null; // We assume the bootstrap classes have no contracts.
            if (className != null && className.endsWith("package-info"))
                return null;
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(0);
			Weaver weaver = new Weaver(loader, writer);
			reader.accept(weaver, ClassReader.EXPAND_FRAMES);
            byte[] result = writer.toByteArray();
            /*
            try {
                FileOutputStream fos = new FileOutputStream(className.replace('/', '_') + ".class");
                fos.write(result);
                fos.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            */
            return result;
        // The JVM silently drops exceptions thrown by this method.
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Error e) {
            e.printStackTrace();
            throw e;
        }
    }
	
	private static String boxedType(Type type) {
		switch (type.getSort()) {
		case Type.BOOLEAN: return "Boolean";
		case Type.BYTE: return "Byte";
		case Type.SHORT: return "Short";
		case Type.INT: return "Integer";
		case Type.LONG: return "Long";
		case Type.CHAR: return "Character";
		case Type.FLOAT: return "Float";
		case Type.DOUBLE: return "Double";
		default: throw new AssertionError();
		}
	}
	
	private void box(MethodVisitor mv, Type type) {
		switch (type.getSort()) {
		case Type.OBJECT:
		case Type.ARRAY:
			break;
		default:
			String className = "java/lang/" + boxedType(type);
			String desc = type.getDescriptor();
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "valueOf", "("+desc+")L"+className+";", false);
			break;
		}
	}

	class Weaver extends ClassVisitor implements Opcodes {
		ClassLoader classLoader;
		int classAccess;
		String className;
		boolean classHasSeeCodeSpecs;
		ArrayList<Runnable> todoActions = new ArrayList<>();
		
		Weaver(ClassLoader classLoader, ClassVisitor cv) {
			super(ASM5, cv);
			this.classLoader = classLoader;
		}
		
		@Override
		public void visit(int version, int access, String name,
				String signature, String superName, String[] interfaces) {
			this.classAccess = access;
			this.className = name;
			super.visit(version, access, name, signature, superName, interfaces);
		}
		
		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			if (desc.equals("Lcodespecs/SeeCodeSpecs;"))
				classHasSeeCodeSpecs = true;
		    return super.visitAnnotation(desc, visible);
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {
			MethodVisitor superVisitor = super.visitMethod(access, name, desc, signature, exceptions);
			WeaverMethodVisitor wmv = new WeaverMethodVisitor(access, name, desc, superVisitor);
			LocalVariablesSorter sorter = new LocalVariablesSorter(access, desc, wmv);
			wmv.sorter = sorter;
			return sorter;
		}
		
		void performTodoActions() {
			for (Runnable r : todoActions) {
				r.run();
			}
		}
		
		@Override
		public void visitEnd() {
			performTodoActions();
			super.visitEnd();
		}
		
		class WeaverMethodVisitor extends MethodVisitor {
			MethodVisitor mv;
			LocalVariablesSorter sorter;
			int methodAccess;
			String methodName;
			String desc;
			Type returnType;
			int postconditionLocalVarId;
			String specReferrerClass;

			public WeaverMethodVisitor(int access, String name, String desc, MethodVisitor mv) {
				super(ASM5, mv);
				this.methodAccess = access;
				this.methodName = name;
				this.desc = desc;
				this.mv = mv;
				if (name.equals("<init>")) {
					if (classHasSeeCodeSpecs && getClassInfo(classLoader, className+"Spec").staticMethods.contains(specMethodForConstructor(desc)))
						specReferrerClass = className;
				} else {
					if (classHasSeeCodeSpecs && (methodAccess & ACC_STATIC) == 0) {
						specReferrerClass = getSpecReferrerClass(classLoader, className, name, desc);
						if (Objects.equals(specReferrerClass, className)) {
							todoActions.add(() -> {
								int wrapperAccess = access & (ACC_PRIVATE | ACC_PROTECTED | ACC_PUBLIC) | ACC_STATIC;
								WrapperMethodInfo info = new WrapperMethodInfo(className, methodName, desc);
								Type[] wrapperArgumentTypes = info.wrapperArgumentTypes;
								MethodVisitor wrapperVisitor = Weaver.super.visitMethod(wrapperAccess, info.wrapperName, info.wrapperDesc, null, null);
								
								wrapperVisitor.visitCode();
								
								{
									int index = 0;
									for (Type argType : wrapperArgumentTypes) {
										wrapperVisitor.visitVarInsn(argType.getOpcode(ILOAD), index);
										index += argType.getSize();
									}
								}
								Type postconditionType = postconditionTypeForReturnType(info.wrappeeReturnType);
								String specMethodDescriptor = Type.getMethodDescriptor(postconditionType, wrapperArgumentTypes);
								wrapperVisitor.visitMethodInsn(INVOKESTATIC, className+"Spec", methodName+"Spec", specMethodDescriptor, false);
								
								int size;
								{
									int index = 0;
									for (Type argType : wrapperArgumentTypes) {
										wrapperVisitor.visitVarInsn(argType.getOpcode(ILOAD), index);
										index += argType.getSize();
									}
									size = index;
								}
								boolean isInterface = (classAccess & ACC_INTERFACE) != 0;
								wrapperVisitor.visitMethodInsn(isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL, className, methodName, desc, isInterface);
								
								if (info.wrappeeReturnType == Type.VOID_TYPE) {
									wrapperVisitor.visitMethodInsn(INVOKEINTERFACE, "java/lang/Runnable", "run", "()V", true);
									wrapperVisitor.visitInsn(RETURN);
								} else {
									if (info.wrappeeReturnType.getSize() == 2)
										wrapperVisitor.visitInsn(DUP2_X1);
									else
										wrapperVisitor.visitInsn(DUP_X1);
									// Box the return value
									box(wrapperVisitor, info.wrappeeReturnType);
									wrapperVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/function/Consumer", "accept", "(Ljava/lang/Object;)V", true);
									wrapperVisitor.visitInsn(info.wrappeeReturnType.getOpcode(IRETURN));
								}
								
								wrapperVisitor.visitMaxs(Math.max((Type.getArgumentsAndReturnSizes(desc) >> 2) + 2, 1 + 2 * info.wrappeeReturnType.getSize()), size);
								wrapperVisitor.visitEnd();
							});
						}
					}
				}
			}
			
			@Override
			public void visitCode() {
				super.visitCode();
				if (specReferrerClass != null) {
					if (methodName.equals("<init>")) {
						Type[] argTypes = Type.getArgumentTypes(desc);
						int index = 1;
						for (Type argType : argTypes) {
							super.visitVarInsn(argType.getOpcode(ILOAD), index);
							index += argType.getSize();
						}
						Type postconditionType = Type.getType("Ljava/util/function/Consumer;");
						String specMethodDescriptor = Type.getMethodDescriptor(postconditionType, argTypes);
						super.visitMethodInsn(INVOKESTATIC, className+"Spec", "constructorSpec", specMethodDescriptor, false);
						postconditionLocalVarId = sorter.newLocal(postconditionType);
						super.visitVarInsn(ASTORE, postconditionLocalVarId);
					} else {
						super.visitVarInsn(ALOAD, 0);
						Type[] argTypes = Type.getArgumentTypes(desc);
						int index = 1;
						for (Type argType : argTypes) {
							super.visitVarInsn(argType.getOpcode(ILOAD), index);
							index += argType.getSize();
						}
						returnType = Type.getReturnType(desc);
						Type postconditionType = postconditionTypeForReturnType(returnType);
						ArrayList<Type> specMethodArgTypes = new ArrayList<>();
						specMethodArgTypes.add(Type.getType("L" + specReferrerClass + ";"));
						specMethodArgTypes.addAll(Arrays.asList(argTypes));
						String specMethodDescriptor = Type.getMethodDescriptor(postconditionType, specMethodArgTypes.toArray(new Type[argTypes.length + 1]));
						super.visitMethodInsn(INVOKESTATIC, specReferrerClass+"Spec", methodName+"Spec", specMethodDescriptor, false);
						postconditionLocalVarId = sorter.newLocal(postconditionType);
						super.visitVarInsn(ASTORE, postconditionLocalVarId);
					}
				}
			}
			
			@Override
			public void visitInsn(int opcode) {
				if (specReferrerClass != null) {
					if (methodName.equals("<init>")) {
						if (opcode == RETURN) {
							super.visitVarInsn(ALOAD, postconditionLocalVarId);
							super.visitVarInsn(ALOAD, 0);
							super.visitMethodInsn(INVOKEINTERFACE, "java/util/function/Consumer", "accept", "(Ljava/lang/Object;)V", true);
						}
					} else {
						if (opcode == IRETURN || opcode == LRETURN || opcode == FRETURN || opcode == DRETURN || opcode == ARETURN) {
							if (returnType.getSize() == 2)
								super.visitInsn(DUP2);
							else
								super.visitInsn(DUP);
							box(mv, returnType);
							super.visitVarInsn(ALOAD, postconditionLocalVarId);
							super.visitInsn(SWAP);
							super.visitMethodInsn(INVOKEINTERFACE, "java/util/function/Consumer", "accept", "(Ljava/lang/Object;)V", true);
						} else if (opcode == RETURN) {
							super.visitVarInsn(ALOAD, postconditionLocalVarId);
							super.visitMethodInsn(INVOKEINTERFACE, "java/lang/Runnable", "run", "()V", true);
						}
					}
				}
				super.visitInsn(opcode);
			}
			
			@Override
			public void visitMethodInsn(int opcode, String owner, String name,
					String desc, boolean itf) {
				if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) {
					String wrapperClass = getSpecReferrerClass(classLoader, owner, name, desc);
					if (wrapperClass != null) {
						Method wrapper = new WrapperMethodInfo(wrapperClass, name, desc).getMethod();
						super.visitMethodInsn(INVOKESTATIC, wrapperClass, wrapper.getName(), wrapper.getDescriptor(), itf);
						return;
					}
				}
				super.visitMethodInsn(opcode, owner, name, desc, itf);
			}
			
			@Override
			public void visitMaxs(int maxStack, int maxLocals) {
				if (specReferrerClass != null) {
					if (methodName.equals("<init>")) {
						maxStack = Math.max(Type.getArgumentsAndReturnSizes(desc) >> 2, maxStack + 2);
					} else {
						maxStack = Math.max(Type.getArgumentsAndReturnSizes(desc) >> 2, maxStack + 1);
					}
				}
				super.visitMaxs(maxStack, maxLocals);
			}
			
		}
		
	}
}
