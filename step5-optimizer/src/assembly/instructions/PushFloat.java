package assembly.instructions;

//3AC to push a value on the stack

public class PushFloat extends Instruction {
    
    public PushFloat(String src) {
        super();
        this.src1 = src;
	this.dest = "pop";
	this.oc = OpCode.PUSHFLOAT;
    }

    public String toString() {
        return "PUSHFLOAT " + src1;
    }

    @Override
    public boolean is3AC() {
        return true;
    }
}
