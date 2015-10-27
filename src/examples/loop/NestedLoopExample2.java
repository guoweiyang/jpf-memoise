package loop;

/* Example taken from:
* "Automatic Partial Loop Summarization in Dynamic Test Generation" by P. Godefroid and D. Luchaup
* ISSSTA 2011
*/

public class NestedLoopExample2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		test(10,10,10);
	}

	public static void test(int x, int y, int z) {
		int cy=0, y1=0, done =0;
		while(x>0) {
//			if(x<=0) { //GX
//				done=1;
//				break;
//			}
			y1=y;
			while(y1>0) {
//				if(y1<=0) //GY
//					break;
				y1--;
				cy=cy+1;
			}
			if(z<=0) //GZ
				break;
			x--;
			z--;
		}
		done=1;
		if(cy==y*1001) {
			System.out.println("abort()");
			assert false;
		}
	}
}

