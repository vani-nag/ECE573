'''import subprocess as sp
sp.call(['sh',"./runme tests/test4.uC t4"])
'''
import os
#a = "./runme tests/test4.uC t4"

for i in range(1,12):
	test_str = "tests/test"+str(i)+".uC"
	op_str = "MyOp/o"+str(i)
	cmd = "./runme "+test_str+" "+op_str
	os.system(cmd)
for i in range(1,12):
	op_str = "MyOp/o"+str(i)
	expected_str = "outputs/test"+str(i)+".out"
	print("Test "+str(i))
	cmd = "diff "+op_str+" "+expected_str
	os.system(cmd)
