package other;

public class Ex_ch {

    public static void main (String[] args) {

        Ex_ch pe = new Ex_ch();
        pe.computeValue(1, 2, 3);
    }


	public int computeValue(int curr, int thresh, int step){
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
	
	
	
//	public int computeValue(int curr, int thresh, int step, int standard){
//		int delta = 0;
//		if (curr < thresh){
//			delta = thresh - curr;
//			if (delta < standard)
//				return delta;
//			else
//				return 0;
//		}else{
//			int counter = 0;
//			while (curr >= thresh){
//				curr = curr - step;
//				counter++;
//			}
//			if(counter>standard){
//				return standard;
//			}else{
//				return counter;
//			}
//		}
//	}
}