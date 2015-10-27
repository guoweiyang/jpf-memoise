package gov.nasa.jpf.memoise.util;

import gov.nasa.jpf.memoise.trie.Trie;
import gov.nasa.jpf.memoise.trie.Trie.TrieNode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class to merge two tries into one
 * 
 * @author Guowei Yang (guoweiyang@utexas.edu)
 * 
 */

public class MergeTrie {
	private static void printHelp(){
		System.out.println("MergeTrie trie_old trie_new trie_merge");
	}
	
	public static void main(String[] args){
		long very_start = System.currentTimeMillis();
		
		if(args.length!=3){
			printHelp();
			System.exit(1);
		}else{
			System.out.println(args[0]);
			System.out.println(args[1]);
			System.out.println(args[2]);
		}
		
		Trie trie_old = null;
		Trie trie_new = null;
		
		// load two tries from disk
		try {
			long start = System.currentTimeMillis();

			FileInputStream fin = new FileInputStream(args[0]);
			ObjectInputStream ois = new ObjectInputStream(fin);
			trie_old = (Trie) ois.readObject();
			ois.close();
			
			fin = new FileInputStream(args[1]);
			ois = new ObjectInputStream(fin);
			trie_new = (Trie) ois.readObject();
			ois.close();
			
			long time = System.currentTimeMillis() - start;
			System.out.println("elapsed Time for de-serialization: " + time);
		} catch (Exception e) {
			System.err.println("something wrong with trie de-serializing");
			e.printStackTrace();
		}
		
		if(trie_old!=null && trie_new!=null)
			traverseTrie(trie_old.getRoot(), trie_new.getRoot());
		
		// store the merged trie to disk
		try {
			long start = System.currentTimeMillis();
			FileOutputStream fout = new FileOutputStream(args[2]);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(trie_new);
			oos.close();// Closing the output stream
			long elapsedTimeMillis = System.currentTimeMillis() - start;
			System.out.println("elapsed Time for serialization: "
					+ elapsedTimeMillis);
		} catch (Exception e) {
			e.printStackTrace();
		}
		long time = System.currentTimeMillis() - very_start;
		System.out.println("elapsed Time for merging: " + time);

	}
	
	/**
	 * traverse the two tries synchronously, and merge trie parts
	 * @param t1
	 * @param t2
	 */
	public static void traverseTrie(TrieNode t1, TrieNode t2) {
		if(!t2.isEnabled()){
			for(int i=0; i<t1.getChildren().size(); i++){
				TrieNode tn = t1.getChildren().get(i);
				t2.addChild(tn);
				tn.setParent(t2);
			}
		}else{
			int size = t1.getChildren().size();
			for(int i = 0; i<size; i++){
				traverseTrie(t1.getChildren().get(i), t2.getChildren().get(i));
			}
		}
	}
	
}
