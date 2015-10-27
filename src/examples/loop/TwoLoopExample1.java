package loop;

public class TwoLoopExample1 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		test(100, 100);
	}

	public static void test(int x, int y) {
		int c=0 , p=0 ;
		while(true) {
			if(x<=0){
				break;
			}
			if(c==1000) {
				System.out.println("abort1"); // error 1
				assert false; // error 1
			}
			c=c+1;
			x=x-1;
		}
		
		while(true) {
			if(y<=0){
				break;
			}
			if(p==500 && c==200) {
				System.out.println("abort2");
				assert false; // error 2
			}
			p=p+1;
			y=y-1;
		}
	}
}