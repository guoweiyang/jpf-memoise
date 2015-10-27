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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * This listener class supports iterative deepening using Memoise
 * 
 * Apply compaction before search, and no solver calls count is recorded
 * 
 * @author Guowei Yang (guoweiyang@utexas.edu)
 * 
 */

public class IDListener extends ListenerAdapter {
	Trie trie;
	TrieNode cur;
	TrieNode frontier;
	Set<TrieNode> frontiers;

	boolean isNew; // flag of whether the trie is updated
	boolean isReplay; // flag of whether it is replaying or rebuilding
	boolean ifCompact = false; // flag of whether compact trie or just prune the trie in search
	
	int count; // number of solver calls
	
	String old_trie_name;
	String new_trie_name;
	long time;

	
	private static final boolean DEBUG = false;

	public IDListener(Config config, JPF jpf) throws Exception {
		// name of the old trie
		old_trie_name = config.getProperty("memoise.old_trie_name");
		if (old_trie_name == null) {
			old_trie_name = "trie.dat";
		}

		// name of the new trie
		new_trie_name = config.getProperty("memoise.new_trie_name");
		if (new_trie_name == null) {
			new_trie_name=old_trie_name;
		}

		
		// de-serialize the old trie from the disk
		try {
			long start = System.currentTimeMillis();
			FileInputStream fin = new FileInputStream(old_trie_name);
			ObjectInputStream ois = new ObjectInputStream(fin);
			trie = (Trie) ois.readObject();
			ois.close();
			time = System.currentTimeMillis() - start;
		} catch (Exception e) {
			System.err.println("something wrong with trie de-serialization");
			e.printStackTrace();
		}

		if (trie != null && trie.getRoot() != null) {
			cur = trie.getRoot();
			isReplay = true;
			PathCondition.setReplay(isReplay);
		} else { // if no trie can be reused, build a trie from scratch
			throw new Exception("no trie is found");
		}
		
		// option to compact or prune the trie
		String ifCompactString = config.getProperty("memoise.if_compact");
		if (ifCompactString != null) {
			ifCompact=Boolean.valueOf(ifCompactString).booleanValue();
		}
		
		count=0;

		analyzePath();
	}

	/**
	 * compute candidate path
	 */
	private void analyzePath() {
		// find all frontiers of the trie
		frontiers = new HashSet<TrieNode>();

		TrieNode root = trie.getRoot();
		traverseTrie(root, frontiers);
		 if (DEBUG) {
			 System.out.println("# of frontiers: " + frontiers.size());
		 }

		if (frontiers.size() > 0) {
			root.setEnabled(true);
		}
		for (TrieNode tn : frontiers) {
			traverseTriePath(tn);
		}
		
		if(ifCompact){
			compact(root);
		}
	}

	/**
	 * traverse the trie and find all frontiers
	 * 
	 * @param node
	 * @param frontiers
	 */
	public void traverseTrie(TrieNode node, Set<TrieNode> frontiers) {
		for (TrieNode n : node.getChildren()) {
			if (n.getType() == TrieNodeType.FRONTIER_NODE) {
				frontiers.add(n);
			}
			traverseTrie(n, frontiers);
		}
	}
	
	/**
	 * compact trie based on the markings 
	 * @param node
	 */
	public void compact(TrieNode node){
		for (TrieNode n : node.getChildren()) {
			if (!n.isEnabled()) {
				n.getChildren().clear();
			}else{
				compact(n);
			}
		}
	}
	
	/**
	 * mark trie paths which need re-execution 
	 * @param node
	 */
	public void traverseTriePath(TrieNode node) {
		while (!node.isEnabled()) {
			node.setEnabled(true);
			node = node.getParent();
		}
	}



	/**
	 * serialize the new trie to the disk
	 */
	private void writeObject() {
		try {
			long start = System.currentTimeMillis();
			FileOutputStream fout = new FileOutputStream(new_trie_name);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(trie);
			oos.close();
			long elapsedTimeMillis = System.currentTimeMillis() - start;
			System.out.println("elapsed Time for new trie serialization: "
					+ elapsedTimeMillis);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void searchFinished(Search search) {
		if (DEBUG) {
			System.out.println(">>> searchFinished");
		}
		// serialize the new trie to disk if the trie is updated
		if (isNew) {
			if (DEBUG) {
				System.out
						.println(">>> the trie is new and needs to be updated");
			}
			writeObject();
		}
		System.out.println("elapsed Time for de-serialization: " + time);
		System.out.println("# solver calls: " + count);
	}

	public void searchConstraintHit(Search search) {
		if (DEBUG) {
			System.out.println("search limit");
		}
		if(cur.getType()==TrieNodeType.REGULAR_NODE){
			cur.setType(TrieNodeType.FRONTIER_NODE); // set frontier
		}
		if (DEBUG) {
			System.out.println(search.getStateId());
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

		if (!isReplay) {
			// rebuild mode
			
			if (!isNew) {
				isNew = true;
			}
			
			 count++;
			
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
		} else {
			// replay mode
			cur = cur.getNextChild();

			// ignore if the node is not enabled
			if (!cur.isEnabled()) {
				if (DEBUG) {
					System.out.println("node is not enabled");
				}
				search.requestBacktrack();
				return;
			}

			if (frontiers.contains(cur)) { // check if it's a frontier node
				frontier = cur;

				isReplay = false;
				PathCondition.setReplay(isReplay);

				if (DEBUG) {
					System.out.println(">>> hit the trie frontier...");
				}
			}

			// ignore at unsatisfied nodes
			if(TrieNodeType.UNSAT_NODE == cur.getType()){
				if(DEBUG){
					System.out.println("unsatisfiable path");
				}
				search.requestBacktrack();
			}
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

		// pc choice
		if (cg != null && cg instanceof PCChoiceGenerator) {
			int offset = ((PCChoiceGenerator) cg).getOffset();
			if (offset == 0) { 
				if (DEBUG) {
					if (cur.getParent() == null) {
						System.out.println("backtracked from root.");
					}
				}
				return;
			}

			// if backtrack from an old frontier, then switch back to replay and
			// reset the old frontier to non-frontier
			if (!isReplay && cur == frontier) {
				if (cur.getChildren().size() > 0) {
					cur.setType(TrieNodeType.REGULAR_NODE);
				}
				isReplay = true;
				PathCondition.setReplay(isReplay);
			}

			cur = cur.getParent();
		}
	}

}
