package assembly.instructions;

/**
 * Class corresponding to RISC-V FNEG instruction
 * 
 * Models: fneg.s dest src #dest = -1 * src
 */
public class FMovI extends Instruction {

    /**
     * Initializes a FNEG instruction that will print: FNEG.S dest src
     * 
     * @param src source operand 1
     * @param dest destination operand
     */

    public FMovI(String src, String dest) {
        super();
        this.src1 = src;
        this.dest = dest;
        this.oc = OpCode.FMOVI;
    }

    /**
     * @return "FNEG.S dest src"
     */
    public String toString() {
        return this.oc + " " + this.dest + ", " + this.src1;
    }
}
