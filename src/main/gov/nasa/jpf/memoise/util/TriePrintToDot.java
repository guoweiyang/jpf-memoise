package gov.nasa.jpf.memoise.util;

import gov.nasa.jpf.memoise.trie.Trie;
import gov.nasa.jpf.memoise.trie.Trie.TrieNode;
import gov.nasa.jpf.memoise.trie.TrieNodeType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Writer;

/**
 * Help class to print a trie to a dot graph
 * 
 * @author Guowei Yang (guoweiyang@utexas.edu)
 * 
 */

public class TriePrintToDot {
	Trie trie;
	
	public void loadTrie(String trieName){
		// de-serialize the stored trie from the disk
		try {
			FileInputStream fin = new FileInputStream(trieName);
			ObjectInputStream ois = new ObjectInputStream(fin);
			trie = (Trie) ois.readObject();
			ois.close();
		} catch (Exception e) {
			System.err.println("something wrong with trie de-serializing");
			e.printStackTrace();
		}
	}
	
	public void print(String fileName){
		Writer output = null;
		File file = new File(fileName);
		try {
			output = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			System.err.println("error while creating the file to write");
			e.printStackTrace();
		}
		try {
			output.write("digraph \"\" { \n");
			if(null != trie.getRoot()){		
				printTrieNodes(trie.getRoot(), output);
				printTrieEdges(trie.getRoot(), output);
			}
			output.write("}");
		} catch (IOException e) {
			System.err.println("Error while writing to the XML file");
			e.printStackTrace();
		}
		
		try {
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printTrieNodes(TrieNode node, Writer output) throws IOException{
		if(node.isEnabled()){
			if(node.getOffset()==-1){//root node
				output.write(node.hashCode() +
						"[ color=\"lightblue\" style=\"filled\" label=Root\"" + "\"];\n");			
			}else {
				output.write(node.hashCode() +
						"[ color=\"red\" label=\"" + node.getMethodName() + ", " + node.getOffset() + ", " + node.getChoice()  + "\"];\n");				
			}
		}else{
			if(node.getOffset()==-1){//root node
				output.write(node.hashCode() +
						"[ color=\"lightblue\" style=\"filled\" label=Root\"" + "\"];\n");			
			}else if(node.getType() == TrieNodeType.UNSAT_NODE){
				output.write(node.hashCode() +
					"[ color=\"grey\" style=\"filled\" label=\"" + node.getMethodName() + ", " + node.getOffset() + ", " + node.getChoice()  + "\"];\n");
			}else if(node.getType() == TrieNodeType.FRONTIER_NODE){
				output.write(node.hashCode() +
					"[ color=\"pink\" style=\"filled\" label=\"" + node.getMethodName() + ", " + node.getOffset() + ", " + node.getChoice()  + "\"];\n");
			}else{
				output.write(node.hashCode() +
						"[ label=\"" + node.getMethodName() + ", " + node.getOffset() + ", " + node.getChoice()  + "\"];\n");		
			}			
		}
		
		//print its children recursively
		for(TrieNode child: node.getChildren()){
			printTrieNodes(child, output);			
		}
	}

	public void printTrieEdges(TrieNode node, Writer output) throws IOException{
		//print its children recursively
		for(TrieNode child: node.getChildren()){
			if(child.isEnabled()){
				output.write(node.hashCode() + "->" + child.hashCode() + "[ color=\"red\"];\n");				
			}else{
				output.write(node.hashCode() + "->" + child.hashCode() + ";\n");				
			}
			printTrieEdges(child, output);			
		}
	}
	
	public static void main(String[] args){
		TriePrintToDot tp = new TriePrintToDot();
//		if(args.length<1){
//			System.err.println("java TriePrintToDot <trie_name>");
//			System.exit(1);
//		}
		
		String name = "trie_init.dat";
		if(args.length>=1){
			name = args[0];
		}
		tp.loadTrie(name);
		tp.print(name.replaceAll("dat", "dot"));
	}

	
}
