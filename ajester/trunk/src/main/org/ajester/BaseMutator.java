package org.ajester;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Label;

import java.util.HashSet;
import java.util.Set;

class BaseMutator extends ClassAdapter implements Mutator {
	private final Set mutations;
	private CodeVisitor codeVisitor;
	private String currentClassName;
	private String currentMethodName;
	
	private final InstructionMatcher instructionMatcher;
	private final InstructionMutator instructionMutator;
	
	public BaseMutator(InstructionMutator instructionMutator) {
		super(new ReuseableClassWriter());
		this.mutations = new HashSet();

		this.instructionMatcher = instructionMutator;
		this.instructionMutator = instructionMutator;
		currentClassName = "";
	}

	public Set getMutations() {
		return mutations;
	}

	public ReuseableClassWriter getClassWriter() {
		return (ReuseableClassWriter) getClassVisitor();
	}

	public final CodeVisitor visitMethod(final int access, final String methodName,
		final String desc, final String[] exceptions) {

		setMethodName(methodName);
		print("visitMethod(" + methodName + ")");
		setCodeVisitor(cv.visitMethod(access, methodName, desc, exceptions));

		CodeVisitor codeVisitor = this.codeVisitor;
		Instruction instruction = new MethodInstruction(getCurrentCodeLocation(), access,
			desc, exceptions);
		
		Access methodAccess = new Access(access);
		boolean abstractOrNative = methodAccess.isAbstract || methodAccess.isNative; 
		if (!abstractOrNative && instructionMatcher.getCodeMatcher().matches(getCurrentCodeLocation())) {
			MethodCoverageInstructionMutator mutator =
				new MethodCoverageInstructionMutator(null);
			
			if (mutator.matches(instruction)) {
				mutator.mutate(instruction).visit(getClassVisitor(), codeVisitor);
			}

			codeVisitor = this;
			
			if (instructionMatcher.matches(instruction)) {
				addCovered();
			
				instructionMutator.mutate(instruction).visit(getClassVisitor(), codeVisitor);
			}
		}

		return codeVisitor;
	}

	public final void visit(int access, String className, String superName, String[] interfaces,
		String sourceFile) {

		setCurrentClassName(className);
		print("visit(" + access + ", " + className + ")");

		super.visit(access, className, superName, interfaces, sourceFile);
	}

	// CodeVisitor methods
	public final void visitInsn(final int opcode) {
		print("visitInsn(" + lookup(opcode) + ")");
		//codeVisitor.visitInsn(opcode);
		mutateIfMatches(new OrdinaryInstruction(getCurrentCodeLocation(), opcode));
	}

	public final void visitIntInsn(final int opcode, final int operand) {
		print("visitIntInsn(" + lookup(opcode) + ")");
		codeVisitor.visitIntInsn(opcode, operand);
	}

	public final void visitVarInsn(final int opcode, final int var) {
		print("visitVarInsn(" + lookup(opcode) + ")");
		codeVisitor.visitVarInsn(opcode, var);
	}

	public final void visitTypeInsn(final int opcode, final String desc) {
		print("visitTypeInsn(" + lookup(opcode) + ")");
		codeVisitor.visitTypeInsn(opcode, desc);
	}

	public final void visitFieldInsn(final int opcode, final String owner, final String name,
		final String desc) {

		print("visitFieldInsn(" + lookup(opcode) + ")");
		codeVisitor.visitFieldInsn(opcode, owner, name, desc);
	}

	public final void visitMethodInsn(final int opcode, final String owner, final String name,
		final String desc) {

		print("visitMethodInsn(" + lookup(opcode) + ")");
		codeVisitor.visitMethodInsn(opcode, owner, name, desc);
	}

	public final void visitJumpInsn(final int opcode, final Label label) {
		print("visitJumpInsn(" + lookup(opcode) + ")");
		mutateIfMatches(new JumpInstruction(getCurrentCodeLocation(), opcode, label));
		//codeVisitor.visitJumpInsn(opcode, label);
	}

	private CodeLocation getCurrentCodeLocation() {
		return new CodeLocation(getCurrentClassNameWithDots(), getMethodName());
	}

	public final void visitLabel(final Label label) {
		print("visitLabel");
		codeVisitor.visitLabel(label);
	}

	public final void visitLdcInsn(final Object cst) {
		print("visitLdcInsn(" + cst + ")");
		codeVisitor.visitLdcInsn(cst);
	}

	public final void visitIincInsn(final int var, final int increment) {
		codeVisitor.visitIincInsn(var, increment);
	}

	public final void visitTableSwitchInsn(final int min, final int max, final Label dflt,
		final Label labels[]) {

		codeVisitor.visitTableSwitchInsn(min, max, dflt, labels);
	}

	public final void visitLookupSwitchInsn(final Label dflt, final int keys[],
		final Label labels[]) {

		codeVisitor.visitLookupSwitchInsn(dflt, keys, labels);
	}

	public final void visitMultiANewArrayInsn(final String desc, final int dims) {
		codeVisitor.visitMultiANewArrayInsn(desc, dims);
	}

	public final void visitTryCatchBlock(final Label start, final Label end, final Label handler,
		final String type) {

		codeVisitor.visitTryCatchBlock(start, end, handler, type);
	}

	public final void visitMaxs(final int maxStack, final int maxLocals) {
		codeVisitor.visitMaxs(maxStack, maxLocals);
	}

	public final void visitLocalVariable(final String name, final String desc, final Label start,
		final Label end, final int index) {

		print("visitLocalVariable(" + name + ")");
		codeVisitor.visitLocalVariable(name, desc, start, end, index);
	}

	public final void visitLineNumber(final int line, final Label start) {
		print("visitLineNumber(" + line + ")");
		codeVisitor.visitLineNumber(line, start);
	}

	public String getMethodName() {
		return currentMethodName;
	}	

	public void setMethodName(String methodName) {
		currentMethodName = methodName;
	}

	public String getCurrentClassName() {
		return currentClassName;
	}

	protected String getCurrentClassNameWithDots() {
		return getCurrentClassName().replace('/', '.');
	}

	public void setCodeVisitor(CodeVisitor codeVisitor) {
		this.codeVisitor = codeVisitor;
	}

	public void setCurrentClassName(String name) {
		this.currentClassName = name;
	}
	
	protected void print(final String toPrint) {
//		System.out.println(this + "[" + getCurrentCodeLocation() + "]." +
//			toPrint);
	}
	
	private String lookup(int opcode) {
		return new ConstantLookup().getOpcodeName(opcode);
	}
	
	public byte[] visit(ClassReader reader) {
		getClassWriter().reset();
		reader.accept(this, false);
		return getClassWriter().toByteArray();
	}
	
	protected ClassVisitor getClassVisitor() {
		return this.cv;
	}
	
	protected CodeVisitor getCodeVisitor() {
		return codeVisitor;
	}

	protected void addCovered() {
		mutations.add(new CodeLocation(getCurrentClassNameWithDots(), getMethodName()));
	}

	public void mutateIfMatches(Instruction instruction) {
		if (instructionMatcher.matches(instruction)) {
			addCovered();
			instruction = instructionMutator.mutate(instruction);
		}
		
		instruction.visit(getClassVisitor(), getCodeVisitor());
	}

	public InstructionMutator getInstructionMutator() {
		return instructionMutator;
	}
}