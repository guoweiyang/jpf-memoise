package loop;

/* Example taken from:
* "Automatic Partial Loop Summarization in Dynamic Test Generation" by P. Godefroid and D. Luchaup
* ISSSTA 2011
*/

public class NestedLoopExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		test(10,10,10);
	}

	public static void test(int x, int y, int z) {
		int cy=0, y1=0, done =0;
		while(true) {
			if(x<=0) { //GX
				done=1;
				break;
			}
			y1=y;
			while(true) {
				if(y1<=0) //GY
					break;
				y1--;
				cy=cy+1;
			}
			if(z<=0) //GZ
				break;
			x--;
			z--;
		}
		if(cy==y*101) {
			System.out.println("abort()");
			assert false;
		}
	}
}

