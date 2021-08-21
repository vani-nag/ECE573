import os
for i in range(0,11):
	test_str = "tests/basic/test"+str(i)+".uC"
	op_str = "MyOp/basic/o"+str(i)
	cmd = "./runme "+test_str+" "+op_str + " 9"
	os.system(cmd)

for i in range(0,9):
	test_str = "tests/control-flow/test"+str(i)+".uC"
	op_str = "MyOp/control-flow/o"+str(i)
	cmd = "./runme "+test_str+" "+op_str + " 9"
	os.system(cmd)

for i in range(0,6):
	test_str = "tests/funcs/test"+str(i)+".uC"
	op_str = "MyOp/funcs/o"+str(i)
	cmd = "./runme "+test_str+" "+op_str + " 9"
	os.system(cmd)

#for i in range(0,11):
#	op_str = "MyOp/basic/o"+str(i)
#	expected_str = "outputs/basic/test"+str(i)+".asm"
#	print("Test Basics "+str(i))
#	cmd = "python3 RiscSim/driver.py "+op_str
#	os.system(cmd)

#for i in range(0,9):
#	op_str = "MyOp/control-flow/o"+str(i)
#	expected_str = "outputs/control-flow/test"+str(i)+".asm"
#	print("Test control-flow "+str(i))
#	cmd = "python3 RiscSim/driver.py "+op_str
#	os.system(cmd)

for i in range(0,6):
	op_str = "MyOp/funcs/o"+str(i)
	expected_str = "outputs/funcs/test"+str(i)+".asm"
	print("Test funcs "+str(i))
	cmd = "python3 RiscSim/driver.py "+op_str
	os.system(cmd)


