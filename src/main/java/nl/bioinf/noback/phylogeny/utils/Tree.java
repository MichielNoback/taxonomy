package nl.bioinf.noback.phylogeny.utils;

import java.util.ArrayList;
import java.util.Iterator;

public class Tree{
	private int nodeNumber;
//	private boolean isEmpty;
//	private ArrayList<E> elements;
	private TreeNode<?> root = null;

	/**
	* constructor
	*/
	public Tree(){
		
	}
	
	/**
	* method signatures
	*/
	public int size(){
		return nodeNumber;
	}
	
	public boolean isEmpty(){
		if(root == null) return true;
		return false;
	}
	
/*	public Iterator<TreeNode> preorderIterator(){
		try {
			return preorderTraversal(this, root).iterator();
		} catch (InvalidNodeException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Iterator<TreeNode> postorderIterator(){
		try {
			return postorderTraversal(this, root).iterator();
		} catch (InvalidNodeException e) {
			e.printStackTrace();
			return null;
		}
	}
*/	
	public void setRoot(TreeNode<?> root){
		this.root = root;
	}
	
	public TreeNode<?> getRoot() throws EmptyTreeException{
		return root;
	}
	
	public TreeNode<?> getParent(TreeNode<?> node) throws InvalidNodeException, BoundaryViolationException{
		return node.getParent();
	}
	
	public <L> Iterator<TreeNode<L>> getChildren(TreeNode<L> node) throws InvalidNodeException{
		return node.getChildren();
	}

	public boolean isInternal(TreeNode<?> node) throws InvalidNodeException{
		if(!node.isExternal()) return true;
		return false;
	}
	
	public boolean isExternal(TreeNode<?> node) throws InvalidNodeException{
		return node.isExternal();
	}
	
	public boolean isRoot(TreeNode<?> node) throws InvalidNodeException{
		return (node==root);
	}
	/**
	 * get the depth of Tree tree at the given node. 
	 * The depth is the number of nodes from current node to the root
	 * @param tree
	 * @param node
	 * @return depth
	 * @throws InvalidNodeException
	 * @throws BoundaryViolationException
	 */
	public static int getDepth(Tree tree, TreeNode<?> node) throws InvalidNodeException, BoundaryViolationException{
		if(tree.isRoot(node)) return 0;
		else return (1 + getDepth(tree, tree.getParent(node)));
	}
	/**
	 * get the height of the tree at a given node.
	 * The height is the number of nodes from the current node to the outermost "leaf" 
	 * @param tree
	 * @param node
	 * @return
	 * @throws InvalidNodeException
	 */
	public static <L> int getHeight(Tree tree, TreeNode<L> node) throws InvalidNodeException{
		if(tree.isExternal(node)) return 0;
		else{
			int h=0;
			Iterator<TreeNode<L>> children = tree.getChildren(node);
			while(children.hasNext()){
				h = Math.max(h, getHeight(tree, children.next()));
			}
			return 1+h;
		}
	}
	/**
	 * do a preorder traversal of the tree
	 * @param <L>
	 * @param tree
	 * @param node
	 * @return ArrayList<TreeNode<L>>
	 * @throws InvalidNodeException
	 */
	public static <L> ArrayList<TreeNode<L>> preorderTraversal(Tree tree, TreeNode<L> node) throws InvalidNodeException{
		ArrayList<TreeNode<L>> list = new ArrayList<TreeNode<L>>();
		list.add(node);
//		System.out.println("node " + node.getNumber());
		if(tree.isInternal(node)){
			Iterator<TreeNode<L>> children = tree.getChildren(node);
			while(children.hasNext()){
				list.addAll(preorderTraversal(tree, children.next()));
			}
		}
		return list;
	}
	
	/**
	 * do a postorder traversal of the tree
	 * @param <L>
	 * @param tree
	 * @param node
	 * @return ArrayList<TreeNode<L>>
	 * @throws InvalidNodeException
	 */
	public static <L> ArrayList<TreeNode<L>> postorderTraversal(Tree tree, TreeNode<L> node) throws InvalidNodeException{
		ArrayList<TreeNode<L>> list = new ArrayList<TreeNode<L>>();
		if(tree.isInternal(node)){
			Iterator<TreeNode<L>> children = tree.getChildren(node);
			while(children.hasNext()){
				list.addAll(postorderTraversal(tree, children.next()));
			}
		}
		list.add(node);
		return list;
	}
	/**
	 * calculate the number of offspring leaves that originate from this node and all
	 * offspring that descend from this node
	 * @param <L>
	 * @param tree
	 * @param node
	 * @throws InvalidNodeException
	 */
	public static <L> int calculateOffspring(Tree tree, TreeNode<L> node) throws InvalidNodeException{
		int offspring = 0;
		
		if(tree.isInternal(node)){
			Iterator<TreeNode<L>> children = tree.getChildren(node);
			while(children.hasNext()){
				offspring += (calculateOffspring(tree, children.next()));
			}
			node.setOffspring(offspring);
		}
		else{
			offspring++;
		}
		return offspring;
	}
	
	/**
	 * do a inlevel traversal of the tree (ie a preorderTraversal followed by an ordering according to nesting level
	 * @param tree
	 * @param root node to start from 
	 * @return HashMap<Integer, ArrayList<TreeNode<L>>> with level-collection mapped to nesting level
	 * @throws InvalidNodeException
	 */
/*	public static <L> HashMap<Integer, ArrayList<TreeNode<L>>> inLevelTraversal(Tree tree, TreeNode<L> node) throws InvalidNodeException{
		ArrayList<TreeNode<L>> list = new ArrayList<TreeNode<L>>();
		list.add(node);
		if(tree.isInternal(node)){
			Iterator<TreeNode<L>> children = tree.getChildren(node);
			while(children.hasNext()){
				list.addAll(preorderTraversal(tree, children.next()));
			}
		}
		
		//order by nesting level
		HashMap<Integer, ArrayList<TreeNode<L>>> levelsHash = new HashMap<Integer, ArrayList<TreeNode<L>>>();
		
		return levelsHash;
	}
*/	

}


