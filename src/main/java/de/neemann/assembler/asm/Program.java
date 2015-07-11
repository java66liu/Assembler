package de.neemann.assembler.asm;

import de.neemann.assembler.expression.Constant;
import de.neemann.assembler.expression.Context;
import de.neemann.assembler.expression.ExpressionException;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author hneemann
 */
public class Program {

    private final ArrayList<Instruction> prog;
    private final Context context;
    private final TreeMap<Integer, ArrayList<Integer>> dataMap;
    private int ramPos = 0;
    private PendingString pendingLabel = new PendingString("label");
    private PendingString pendingMacroDescription = new PendingString("description");
    private PendingString pendingComment = new PendingString("comment");
    private int lineNumber;

    public Program() {
        prog = new ArrayList<>();
        context = new Context();
        dataMap = new TreeMap<>();
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Program add(Instruction i) {
        i.setLabel(pendingLabel.get());
        i.setMacroDescription(pendingMacroDescription.get());
        i.setComment(pendingComment.get());

        i.setLineNumber(lineNumber);
        lineNumber = 0;

        prog.add(i);
        return this;
    }

    public Program traverse(InstructionVisitor instructionVisitor) throws ExpressionException {
        int addr = 0;
        for (int i = 0, progSize = prog.size(); i < progSize; i++) {
            Instruction in = prog.get(i);
            try {
                context.setInstrAddr(addr);
                context.setSkipAddr(calcSkipAddr(addr, i));
                instructionVisitor.visit(in, context);
                addr += in.size();
            } catch (ExpressionException e) {
                e.setLineNumber(in.getLineNumber());
                throw e;
            }
        }
        return this;
    }

    private int calcSkipAddr(int addr, int i) {
        if (i < prog.size() - 2)
            return addr + prog.get(i).size() + prog.get(i + 1).size();
        else
            return addr;
    }

    private Program appendData() throws InstructionException {
        int p = 0;
        for (Map.Entry<Integer, ArrayList<Integer>> e : dataMap.entrySet()) {
            int value = e.getKey();
            prog.add(p++, Instruction.make(Opcode.LDI, Register.R0, new Constant(value)));
            for (int addr : e.getValue()) {
                prog.add(p++, Instruction.make(Opcode.STS, Register.R0, new Constant(addr)));
            }
        }
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Instruction i : prog) {
            sb.append(i.toString()).append("\n");
        }
        return sb.toString();
    }

    public int getInstructionCount() {
        return prog.size();
    }

    public Instruction getInstruction(int i) {
        return prog.get(i);
    }

    public int addRam(String ident, int size) throws ExpressionException {
        int r = ramPos;
        context.addIdentifier(ident, ramPos);
        ramPos += size;
        return r;
    }

    public Context getContext() {
        return context;
    }

    public void addData(int value) {
        ArrayList<Integer> list = dataMap.get(value);
        if (list == null) {
            list = new ArrayList<>();
            dataMap.put(value, list);
        }
        list.add(ramPos);
        ramPos++;
    }

    public void setPendingLabel(String pendingLabel) throws ExpressionException {
        this.pendingLabel.set(pendingLabel);
    }

    public void setPendingMacroDescription(String pendingMacroDescription) throws ExpressionException {
        this.pendingMacroDescription.set(pendingMacroDescription);
    }

    public void setPendingComment(String comment) throws ExpressionException {
        this.pendingComment.set(comment);
    }

    public Program optimizeAndLink() throws InstructionException, ExpressionException {
        return appendData()
                .traverse(new LinkAddVisitor())
                .traverse(new OptimizerShort())
                .traverse(new LinkSetVisitor())
                .traverse(new OptimizerJmp())
                .traverse(new LinkSetVisitor());
    }

    private static class LinkAddVisitor implements InstructionVisitor {
        @Override
        public void visit(Instruction instruction, Context context) throws ExpressionException {
            if (instruction.getLabel() != null) {
                context.addIdentifier(instruction.getLabel(), context.getInstrAddr());
            }
        }
    }

    private static class LinkSetVisitor implements InstructionVisitor {
        @Override
        public void visit(Instruction instruction, Context context) throws ExpressionException {
            if (instruction.getLabel() != null) {
                context.setIdentifier(instruction.getLabel(), context.getInstrAddr());
            }
        }
    }


    private class PendingString {
        private String name;
        private String str;

        public PendingString(String name) {
            this.name = name;
        }

        public void set(String s) throws ExpressionException {
            if (this.str != null)
                throw new ExpressionException("two " + name + " for the same command: " + str + ", " + s);
            this.str = s;
        }

        public String get() {
            String s = str;
            str = null;
            return s;
        }
    }
}
