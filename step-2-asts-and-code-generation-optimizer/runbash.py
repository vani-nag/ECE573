import os
for i in range(1,11):
	test_str = "tests/test"+str(i)+".uC"
	op_str = "MyOp/o"+str(i)
	cmd = "./runme "+test_str+" "+op_str
	os.system(cmd)
for i in range(1,11):
	op_str = "MyOp/o"+str(i)
	expected_str = "outputs/test"+str(i)+".asm"
	print("Test "+str(i))
	cmd = "diff "+op_str+" "+expected_str
	os.system(cmd)
