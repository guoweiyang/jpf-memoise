/**
 * Example class to test Memoise
 * 
 * @author Guowei Yang (guoweiyang@utexas.edu)
 * 
 */

public class Ex {

    public static void main (String[] args) {
        Ex pe = new Ex();
        pe.m(1, 2, 3);
    }


	public int m(int curr, int thresh, int step){
		int delta = 0;
		if (curr < thresh){
			delta = thresh - curr;
			if ((curr + step) < thresh)
				return -delta;
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