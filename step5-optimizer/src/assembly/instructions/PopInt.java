package assembly.instructions;

//3AC to push a value on the stack

public class PopInt extends Instruction {
    
    public PopInt(String src) {
        super();
        this.src1 = src;
	this.dest = "pop";
	this.oc = OpCode.POPINT;
    }

    public String toString() {
        return "POPINT " + src1;
    }

    @Override
    public boolean is3AC() {
        return true;
    }
}
