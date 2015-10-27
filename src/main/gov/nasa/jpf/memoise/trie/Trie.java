package gov.nasa.jpf.memoise.trie;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Trie data structure with the following info stored: methodName, bytecode offset, and choice.
 * 
 * @author Guowei Yang (guoweiyang@utexas.edu)
 * 
 */

public class Trie implements Serializable {
	private static final long serialVersionUID = 7526472295622776147L; 

	private TrieNode root;

	public TrieNode getRoot() {
		return root;
	}

	public void setRoot(TrieNode root) {
		this.root = root;
	}

	public Trie(){
//		root = new Node(null, null);
	}
	
	public static class TrieNode implements Serializable{
		private static final long serialVersionUID = 7526472295634343143L; 
		
		private TrieNode parent;
		private ArrayList<TrieNode> children;
		private int choice; // branch choice
		private int offset; // bytecode offset
		private String methodName;
		private boolean enabled; // for node markings
		private int nextChoice; 
		private int type; // node type

		public TrieNode(int choice, int offset, String methodName, TrieNode parent){	
			this.choice = choice;
			this.offset = offset;
			this.methodName = methodName;
			this.parent = parent;
			this.type = TrieNodeType.REGULAR_NODE; // default node type
			children = new ArrayList<TrieNode>();
			if(parent!=null){
				this.parent.addChild(this);
			}
		}

		public TrieNode(int choice, int offset, String methodName, TrieNode parent, int type){	
			this.choice = choice;
			this.offset = offset;
			this.methodName = methodName;
			this.parent = parent;
			this.type = type;
			children = new ArrayList<TrieNode>();
			if(parent!=null){
				this.parent.addChild(this);
			}
		}
		
		public TrieNode getParent() {
			return parent;
		}

		public void setParent(TrieNode parent) {
			this.parent = parent;
		}

		public ArrayList<TrieNode> getChildren() {
			return children;
		}

		public void setChildren(ArrayList<TrieNode> children) {
			this.children = children;
		}
		
		public void addChild(TrieNode child) {
			this.children.add(child);
		}

		public int getChoice() {
			return choice;
		}

		public void setChoice(int choice) {
			this.choice = choice;
		}

		public int getOffset() {
			return offset;
		}

		public void setOffset(int offset) {
			this.offset = offset;
		}
		
		
		public int getNextChoice(){
			return nextChoice++;
		}
		
		public TrieNode getNextChild(){
			if(nextChoice >= children.size()){
				System.err.println("nextChoice out of bound");
				return null;
			}else{
				return children.get(nextChoice++);
			}
		}
		
		public void resetNextChoice(){
			nextChoice = 0;
		}

		public int retNextChoice(){
			return nextChoice;
		}
		
		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}
		
		public String getMethodName() {
			return methodName;
		}

		public void setMethodName(String methodName) {
			this.methodName = methodName;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
		
		public String toString(){
			return methodName + ":" + offset + ":" + choice;
		}
	}
}
