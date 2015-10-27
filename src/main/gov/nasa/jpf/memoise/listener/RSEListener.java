package gov.nasa.jpf.memoise.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.ThreadChoiceGenerator;
import gov.nasa.jpf.memoise.app.ImpactAnalysis;
import gov.nasa.jpf.memoise.cfg.CFG;
import gov.nasa.jpf.memoise.cfg.CFGBuilder;
import gov.nasa.jpf.memoise.cfg.CFGComparator;
import gov.nasa.jpf.memoise.cfg.Edge;
import gov.nasa.jpf.memoise.cfg.Node;
import gov.nasa.jpf.memoise.trie.Trie;
import gov.nasa.jpf.memoise.trie.TrieNodeType;
import gov.nasa.jpf.memoise.trie.Trie.TrieNode;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.sequences.SequenceChoiceGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;

/**
 * This listener class supports regression symbolic execution using Memoise
 * The program change is characterized as CFG nodes, method names, and class names specified by the user 
 *
 * Apply pruning during search, and no solver calls count is recorded
 * 
 * @author Guowei Yang (guoweiyang@utexas.edu)
 *
 */

public class RSEListener extends ListenerAdapter {
	Trie trie;
	TrieNode cur;
	TrieNode frontier;
	TrieNode change;
	Set<TrieNode> impactedTrieNodes;

	boolean isNew; // flag of whether the trie is updated
	boolean isReplay; // flag of whether it is replaying or rebuilding
	boolean ifCompact = false; // flag of whether compact trie or just prune the trie in search
	long time;
	long time_analysis;
	int count; // number of solver calls
	
	String old_trie_name;
	String new_trie_name;

	
	private static final boolean DEBUG = false;

	public RSEListener(Config config, JPF jpf) {
		System.out.println("Using the RSEListener ...");
		
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

		// init the count of solover calls
		count = 0;
		
		// de-serialize the old trie from the disk
		System.out.println("de-serializing the stored trie");
		try {
			long start = System.currentTimeMillis();

			FileInputStream fin = new FileInputStream(old_trie_name);
			ObjectInputStream ois = new ObjectInputStream(fin);
			trie = (Trie) ois.readObject();
			ois.close();

			time = System.currentTimeMillis() - start;

		} catch (Exception e) {
			System.err.println("something wrong with trie de-serializing");
			e.printStackTrace();
		}

		if (trie != null && trie.getRoot() != null) {
			cur = trie.getRoot();
			isReplay = true;
			PathCondition.setReplay(isReplay);
		} else { // if no trie is available for reuse, build one from scratch
			trie = new Trie();
			TrieNode root = new TrieNode(-1, -1, null, null);
			trie.setRoot(root);
			cur = root;
			isReplay = false;
			PathCondition.setReplay(isReplay);
			return; // no need to compute change impact
		}
		
		// option to compact or prune the trie
		String ifCompactString = config.getProperty("memoise.if_compact");
		if (ifCompactString != null) {
			ifCompact=Boolean.valueOf(ifCompactString).booleanValue();
		}
		
		long start = System.currentTimeMillis();
		
		/*
		 * change impact analysis
		 */
		
        File oldFile = new File("/Users/gyang/workspaces/memoise-workspace/jpf-memoise/version0/Ex.class"); 
        File newFile = new File("/Users/gyang/workspaces/memoise-workspace/jpf-memoise/version1/Ex.class"); 

		JavaClass oldJavaClass = null;
		JavaClass newJavaClass = null;
		
        try {
        	oldJavaClass = new ClassParser(oldFile.toString()).parse(); 
        	newJavaClass = new ClassParser(newFile.toString()).parse(); 
        } catch (ClassFormatException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        
        ClassGen oldCG = new ClassGen(oldJavaClass);
		ClassGen newCG = new ClassGen(newJavaClass);
		
		ConstantPoolGen oldCPG = oldCG.getConstantPool();
		ConstantPoolGen newCPG = newCG.getConstantPool();

		MethodGen oldMG = null;
		MethodGen newMG = null;

		CFG oldCFG = null;
		CFG newCFG = null;

		Map<String, MethodGen> oldMGMap = new HashMap<String, MethodGen>();
		Map<String, MethodGen> newMGMap = new HashMap<String, MethodGen>();

		Map<String, CFG> oldCFGMap = new HashMap<String, CFG>();
		Map<String, CFG> newCFGMap = new HashMap<String, CFG>();

		
		for (Method m: oldCG.getMethods()) {
			String fullName = oldCG.getClassName() + "." + m.getName() + m.getSignature();
			oldCFG = new CFG();
			oldMG = new MethodGen(m, oldCG.getClassName(), oldCPG);
			oldMGMap.put(fullName, oldMG);
			buildCFG(oldCFG, oldMG, oldCPG);
			oldCFGMap.put(fullName, oldCFG);
			oldCFG.setMethodName(fullName);
		}

		for (Method m: newCG.getMethods()) {
			String fullName = newCG.getClassName() + "." + m.getName() + m.getSignature();
			newCFG = new CFG();
			newMG = new MethodGen(m, newCG.getClassName(), newCPG);
			newMGMap.put(fullName, newMG);
			buildCFG(newCFG, newMG, newCPG);
			newCFGMap.put(fullName, newCFG);
			newCFG.setMethodName(fullName);
		}

		Set<CFG> dangerousCFGs = new HashSet<CFG>();
		
		for(String method: oldCFGMap.keySet()){
			oldCFG = oldCFGMap.get(method);
			//TODO: IF newCFG is null?? i.e., the method is not existent any more
			newCFG = newCFGMap.get(method);
			CFGComparator bc = new CFGComparator(oldCPG, oldMGMap.get(method), oldCFG, newCPG, newMGMap.get(method), newCFG);
			bc.compareCFG();
			if(DEBUG){
				print(bc);
			}
			if(bc.getDangers().size()>0){
				oldCFG.setDangers(bc.getDangers());
				dangerousCFGs.add(oldCFG);
			}
		}

		// compute impacted trie nodes based on the CFG change
		ImpactAnalysis ia = new ImpactAnalysis(dangerousCFGs, trie);
		impactedTrieNodes = ia.getImpactedNodes();
//		
		if(impactedTrieNodes.size()>0){
			if (DEBUG) {
				System.out.println("# of impacted nodes:" + impactedTrieNodes.size());
			}
			trie.getRoot().setEnabled(true);
		}
		
		for (TrieNode tn : impactedTrieNodes) {
			traverseTriePath(tn);
		}

		if(ifCompact){
			compact(trie.getRoot());
		}
		
		time_analysis = System.currentTimeMillis() - start;
	}

	
	public static void buildCFG(CFG cfg, MethodGen mg, ConstantPoolGen cpg){
		//1. build CFG, including blocks, edges, preds, and succs
		CFGBuilder builder = new CFGBuilder(mg,cpg);
		builder.formBasicBlocks();
		builder.checkBranchInstruction();
		
		cfg.setNodeMap(builder.getAllBasicBlocks());
		for(Node n: builder.getAllBasicBlocks().values()){
			cfg.addNode(n);
		}
		for(Edge e: builder.getAllEdges()){
			cfg.addEdge(e);
		}
		
		cfg.setReverseInsnLookup(builder.getReverseInsnLookup());
		cfg.setBlockBranchMap(builder.getBlockBranchMap());
	}
	
	private static void print(CFGComparator cfgc){
		System.out.println("\n >>> dangerous cfg nodes: ");
		for(Node node: cfgc.getDangers()){
			System.out.println(node.getStartPos());
		}
		
		System.out.println("\n >>> matched cfg nodes: ");
		for(Node node: cfgc.getMatchPair().keySet()){
			System.out.println(node.getStartPos() + " : " + cfgc.getMatchPair().get(node).getStartPos());
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
	 * serialize the new trie to the disk
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
		if (isNew) {
			 writeObject();
		}
		System.out.println("elapsed Time for de-serialization: " + time);
		System.out.println("elapsed Time for impact analysis: " + time_analysis);
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

	public void choiceGeneratorAdvanced(JVM vm) {
		if (DEBUG) {
			System.out.println(">>> choiceGeneratorAdvanced");
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
			
			// create node, add it as cur's child, and update cur		
			int choice = ((PCChoiceGenerator) cg).getNextChoice();
			int offset = ((PCChoiceGenerator) cg).getOffset();
			String method = ((PCChoiceGenerator) cg).getMethodName();
			TrieNode n = new TrieNode(choice, offset, method, cur);
			cur = n;
			
			// check satisfiability
			PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
			if (pc == null) {
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
				search.setIgnoredState(true);
				return;
			}
			
			if (TrieNodeType.FRONTIER_NODE == cur.getType()) { // check if it's a frontier node
				if (!isNew) {
					isNew = true;
				}
				isReplay = false;
				PathCondition.setReplay(isReplay);

				frontier = cur;

				if (DEBUG) {
					System.out.println(">>> hit the trie frontier...");
				}
			}

			if (impactedTrieNodes.contains(cur)) {
				if (!isNew) {
					isNew = true;
				}

				isReplay = false;
				PathCondition.setReplay(isReplay);

				if (DEBUG) {
					System.out.println(">>> hit the change ...");
				}

				change = cur;
				change.getChildren().clear(); // all nodes rooted at change should be rebuilt
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

			if (!isReplay && cur == change) {
				isReplay = true;
				PathCondition.setReplay(isReplay);
			}

			cur = cur.getParent();
		}
	}
}
