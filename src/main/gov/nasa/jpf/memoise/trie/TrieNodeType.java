package gov.nasa.jpf.memoise.trie;

/**
 * Help class for different trie node types
 * 
 * @author Guowei Yang (guoweiyang@utexas.edu)
 * 
 */

public class TrieNodeType {
	public static int REGULAR_NODE = 0;		// regular node
	public static int UNSAT_NODE = 1;  		// unsatisfiable node
	public static int FRONTIER_NODE = 2; 	// frontier node
}
