package gov.nasa.jpf.memoise.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ThreadChoiceGenerator;
import gov.nasa.jpf.memoise.trie.Trie;
import gov.nasa.jpf.memoise.trie.TrieNodeType;
import gov.nasa.jpf.memoise.trie.Trie.TrieNode;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.sequences.SequenceChoiceGenerator;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

/**
 * This listener class builds a trie during symbolic execution
 * 
 * @author Guowei Yang (guoweiyang@utexas.edu)
 * 
 */

public class TrieBuilder extends ListenerAdapter {
	Trie trie;
	TrieNode cur;
	String new_trie_name;

	static boolean DEBUG = false;

	public TrieBuilder(Config config, JPF jpf) {
		System.out.println("Building the trie ...");
		trie = new Trie();
		new_trie_name = config.getProperty("memoise.new_trie_name");

		if (new_trie_name == null) {
			new_trie_name = "trie.dat"; // default trie name
		}
	}

	/**
	 * serialize the trie to disk
	 */
	private void writeObject() {
		try {
			long start = System.currentTimeMillis();
			FileOutputStream fout = new FileOutputStream(new_trie_name);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(trie);
			oos.close();// Closing the output stream
			long elapsedTimeMillis = System.currentTimeMillis() - start;
			System.out.println("elapsed Time for serialization: "
					+ elapsedTimeMillis);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void searchFinished(Search search) {
		if (DEBUG) {
			System.out.println(">>> searchFinished");
		}
		writeObject();
	}

	public void searchConstraintHit(Search search) {
		if (DEBUG) {
			System.out.print("search limit");
		}
		if(cur.getType()==TrieNodeType.REGULAR_NODE){
			cur.setType(TrieNodeType.FRONTIER_NODE); // set frontier
		}
		if (DEBUG) {
			System.out.print(" " + search.getStateId());
		}
	}

	public void stateAdvanced(Search search) {
		if (DEBUG) {
			System.out.println(">>> stateAdvanced");
		}
		ChoiceGenerator<?> cg = search.getVM().getChoiceGenerator();
		if (DEBUG) {
			System.out.println("cg: " + cg);
		}

		// thread choice instead of pc choice
		if (cg instanceof ThreadChoiceGenerator) {
			return;
		}
		if (cg instanceof SequenceChoiceGenerator) {
			return;
		}

		if (cg instanceof PCChoiceGenerator) {
			int offset = ((PCChoiceGenerator) cg).getOffset();
			if (offset == 0) {
				return;
			}
		}

		if (trie.getRoot() == null) { // create the root node
			TrieNode root = new TrieNode(-1, -1, null, null);
			trie.setRoot(root);
			cur = root;
		}
		
		// create node, add it as cur's child, and update cur
		int choice = ((PCChoiceGenerator) cg).getNextChoice();
		int offset = ((PCChoiceGenerator) cg).getOffset();
		String method = ((PCChoiceGenerator) cg).getMethodName();
		
		// create node, add it as cur's child, and update cur
		TrieNode n = new TrieNode(choice, offset, method, cur);
		cur = n;
		
		PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
		if (pc == null) {
			// unsatisfiable constraint
			cur.setType(TrieNodeType.UNSAT_NODE);
			if (DEBUG) {
				System.err.println("* unsatisfiable path");
			}
		}
		
		if (DEBUG) {
			System.out.println("**** choice: " + choice);
			System.out.println("**** offset: " + offset);
			System.out.println("**** method: " + method);
		}

	}

	public void stateBacktracked(Search search) {
		if (DEBUG) {
			System.out.println(">>> stateBacktracked");
		}

		ChoiceGenerator<?> cg = search.getVM().getChoiceGenerator();
		if (DEBUG) {
			System.out.println("cg: " + cg);
		}

		if (cg != null && cg instanceof PCChoiceGenerator) { 
			int offset = ((PCChoiceGenerator) cg).getOffset();
			if (offset == 0) { 
				return;
			}

			if (cur == null) {
				if(DEBUG){
					System.err.println("backtracked from root node; no action needed for now");
				}
				return;
			}
			cur = cur.getParent();
			if(DEBUG){
				if (cur.getParent() == null) {
					System.out.println("backtracked to root.");
				}
			}
		}
	}
}
