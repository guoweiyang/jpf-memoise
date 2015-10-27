/**
 * Example class to test Memoise, version 1
 * 
 * @author Guowei Yang (guoweiyang@utexas.edu)
 * 
 */

public class Ex_v1 {

    public static void main (String[] args) {

        Ex_v1 pe = new Ex_v1();
        pe.m(1, 2, 3);
    }


	public int m(int curr, int thresh, int step){
		int delta = 0;
		if (curr < thresh){
			delta = thresh - curr;
			if ((curr + step) < thresh)
				return delta; // change from -delta
			else
				return 0;
		}else{
			int counter = 0;
			while (curr >= thresh){
				curr = curr - step;
				counter++;
			}
			return counter;
		}
	}
}