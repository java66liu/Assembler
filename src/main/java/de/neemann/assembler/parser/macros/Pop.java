package de.neemann.assembler.parser.macros;

import de.neemann.assembler.asm.*;
import de.neemann.assembler.expression.Constant;
import de.neemann.assembler.expression.ExpressionException;
import de.neemann.assembler.parser.Macro;
import de.neemann.assembler.parser.Parser;
import de.neemann.assembler.parser.ParserException;

import java.io.IOException;

/**
 * @author hneemann
 */
public class Pop extends Macro {

    /**
     * Creates a new instance
     */
    public Pop() {
        super("POP", MnemonicArguments.DEST, "copy value from the stack to the given register, adds one to the stack pointer");
    }

    @Override
    public void parseMacro(Program p, String name, Parser parser) throws IOException, ParserException, InstructionException, ExpressionException {
        Register r = parser.parseReg();
        p.setPendingMacroDescription(getName() + " " + r.name());
        pop(r, p);
    }

    /**
     * Add a pop instruction to the program
     *
     * @param r the register to pop
     * @param p the program
     * @throws InstructionException InstructionException
     */
    public static void pop(Register r, Program p) throws InstructionException {
        p.add(new InstructionBuilder(Opcode.LD).setDest(r).setSource(Register.SP).build());
        p.add(new InstructionBuilder(Opcode.ADDIs).setDest(Register.SP).setConstant(new Constant(1)).build());
    }
}
