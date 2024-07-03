package nl.bioinf.noback.phylogeny.utils;

import java.util.ArrayList;
import java.util.Iterator;

public class TreeNode<L>{
	private static int nodeNumber = 0;
	private int number;
	private int offspring;
	private int level;
	private ArrayList<TreeNode<L>> children;
	private TreeNode<L> parent;
	private L element;

	/**
	 * constructor creates a new children List
	 */
	public TreeNode(){
		children = new ArrayList<TreeNode<L>>();
		this.number = nodeNumber;
		nodeNumber ++;
	}
	/**
	 * get iterator of children of this node
	 */
	public Iterator<TreeNode<L>> getChildren() {
		return children.iterator();
	}
	
	public int getNumberOfChildren(){
		return children.size();
	}
	/**
	 * get the parent node of this node 
	 */
	public TreeNode<L> getParent() {
		return parent;
	}
	/**
	 * check whether this node has children: if so, it is not external
	 */
	public boolean isExternal() {
		if(children.isEmpty()) return true;
		return false;
	}
	/**
	 * add a child to this node
	 * @param childnode
	 */
	public void addChild(TreeNode<L> node) {//TreeNode<E,L>
		node.setParent(this);
		children.add(node);
	}
	/**
	 * set the parent to this node
	 */
	public void setParent(TreeNode<L> parent) {
		this.parent = parent;
	}
	/**
	 * get the content element of the node
	 */
	public L getElement() {
		return element;
	}
	/**
	 * get the content element of the node
	 */
	public void setElement(L element) {
		this.element = element;
	}
	/**
	 * node number
	 * @return
	 */
	public int getNumber() {
		return number;
	}
	/**
	 * get the nesting level from the root
	 * @return nesting level
	 */
	public int getLevel() {
		return level;
	}
	/**
	 * set the nesting level from the root 
	 * @param nesting level
	 */
	public void setLevel(int level) {
		this.level = level;
	}
	/**
	 * get the number of offspring that originate from this node
	 * @return offspring
	 */
	public int getOffspring() {
		return offspring;
	}
	/**
	 * set the number of offspring that originate from this node
	 * @param offspring number
	 */
	public void setOffspring(int offspring) {
		this.offspring = offspring;
	}

}
