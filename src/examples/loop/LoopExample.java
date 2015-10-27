package loop;

/* Example taken from:
* "Automatic Partial Loop Summarization in Dynamic Test Generation" by P. Godefroid and D. Luchaup
* ISSSTA 2011
*/

public class LoopExample {

	public static void main(String[] args) {
		test(100);
	}

	public static void test(int x) {
		int c=0 , p=0 ;
		while(true) {
			if(x<=0) break;
			if(c==50) {
				System.out.println("abort1");
				assert false; // error 1
			}
			c=c+1;
			p=p+c;
			x=x-1;
		}
		if(c==30) {
			System.out.println("abort2");
			assert false; // error 2
		}
	}
}