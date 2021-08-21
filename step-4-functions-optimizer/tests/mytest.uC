int foo(int x, int y);

int bar(int x, int y);

int main() {

	int a;
	int b;
	int c;
	int d;
float k;
	read(a);
	read(b);
	c = 1;
	d = 1;
	d = -k;
	if(a<b)
	{
	c = foo(a, b);
	}
	if(b<a)
	{
	d = bar(a, b);
	}

	print(c);
	print(d);

	return 0;
}

int foo(int x, int y) {
	int b;
	float c;
	b = x + y;
	
	
	return b;
}

int bar(int x, int y) {
	return x - y;
}
