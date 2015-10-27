package gov.nasa.jpf.memoise.app;

import gov.nasa.jpf.memoise.cfg.CFG;
import gov.nasa.jpf.memoise.cfg.Node;
import gov.nasa.jpf.memoise.trie.Trie;
import gov.nasa.jpf.memoise.trie.Trie.TrieNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class computes impacted trie nodes which are used for applications 
 * such as regression symbolic execution
 * 
 * @author Guowei Yang (guoweiyang@utexas.edu)
 *
 */

public class ImpactAnalysis {
	Set<CFG> cfgs;
	Trie trie;
//	Set<Integer> impactedConditional;
	Set<Element> coveredElementSet;
	Map<Element, Set<TrieNode>> trieNodeMap;
	Set<TrieNode> impactedNodes;
	Set<Element> changeNodes;
	
    /*************************************************************************
	 * constructor
     */
	public ImpactAnalysis(Set<CFG> cfgs, Trie trie){
		this.cfgs = cfgs;
		this.trie = trie;
		coveredElementSet = new HashSet<Element>();
//		impactedConditional = new HashSet<Integer>();
		trieNodeMap = new HashMap<Element, Set<TrieNode>>();
		impactedNodes = new HashSet<TrieNode>();
		changeNodes = new HashSet<Element>();
		traverseTrie(trie.getRoot());
		analyze();
	}
	
	public void analyze(){
		
		// collect all change nodes in CFGs
		for(CFG cfg: cfgs){
			for(Node n: cfg.getDangers()){
				backwardCheckCFG(cfg, n);
			}
		}
		
		for(Element e: changeNodes){
			impactedNodes.addAll(trieNodeMap.get(e));
		}
	}
	
	public void backwardCheckCFG(CFG cfg, Node n){
		for(Node pred: n.getPredecessors()){
			// reaches the entry node
			if(pred.getStartPos()==-1){
				//TODO:
				System.out.println("method is changed");
				break;
			}
			
			Integer branch = cfg.getBlockBranchMap().get(pred.getStartPos());
			if(branch!=null){
				int choice;
				String label = cfg.getLabel(pred, n);
				if(label.equals("True")){
					choice = 0;
				}else{
					choice = 1;
				}
				Element cn = new Element(branch.intValue(), choice, cfg.getMethodName());
				if(coveredElementSet.contains(cn)){
					changeNodes.add(cn);
					continue;
				}
			}
			backwardCheckCFG(cfg, pred);
		}
		
	}
	
	/**
	 * traverse the trie and find all offsets
	 * 
	 * @param node
	 * @param frontiers
	 */
	public void traverseTrie(TrieNode node) {
		if(node.getOffset() != -1){
			int offset = node.getOffset();
			int choice = node.getChoice();
			String method = node.getMethodName();
			if(offset>0){
				Element e = new Element(offset, choice, method);
				coveredElementSet.add(e);
				if(trieNodeMap.get(e)==null){
					Set<TrieNode> nodes = new HashSet<TrieNode>();
					nodes.add(node);
					trieNodeMap.put(e, nodes);
				}else{
					trieNodeMap.get(e).add(node);
				}
			}			
		}
		for (TrieNode n : node.getChildren()) {
			traverseTrie(n);
		}
	}
	
	public Set<Element> getChangeNodes() {
		return changeNodes;
	}

	public void setChangeNodes(Set<Element> changeNodes) {
		this.changeNodes = changeNodes;
	}
	
	
	public class Element{
		int pos;
		int choice;
		String method;
		
		public Element(int pos, int choice, String method){
			this.pos = pos;
			this.choice = choice;
			this.method = method;
		}
		
		public boolean equals(int pos, int choice, String method){
			return this.pos == pos && this.choice == choice && this.method.equals(method);
		}

		public boolean equals(Object o){
			if(o instanceof Element){
				Element n = (Element)o;
				return this.pos == n.pos && this.choice == n.choice && this.method.equals(n.method);				
			}
			return false;
		}
		
		public int hashCode(){
			return (pos * 23 + choice) * 23 +  method.hashCode();
		}
		
		public String toString(){
			return method + ":" + pos + ":" + choice;
		}
	}


	public Set<TrieNode> getImpactedNodes() {
		return impactedNodes;
	}

	public void setImpactedNodes(Set<TrieNode> impactedNodes) {
		this.impactedNodes = impactedNodes;
	}


	
}
