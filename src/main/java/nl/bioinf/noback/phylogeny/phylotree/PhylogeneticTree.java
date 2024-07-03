/**
 * 
 */
package nl.bioinf.noback.phylogeny.phylotree;

import java.util.Iterator;

import nl.bioinf.noback.phylogeny.utils.BoundaryViolationException;
import nl.bioinf.noback.phylogeny.utils.InvalidNodeException;
import nl.bioinf.noback.phylogeny.utils.Tree;
import nl.bioinf.noback.phylogeny.utils.TreeNode;

/**
 * a subclass of net.noback.utils.Tree with phylogenetics specific methods
 * @author Michiel
 * 
 *
 */
public class PhylogeneticTree extends Tree {

	/**
	 * 
	 */
//	public PhylogeneticTree() {
	
//	}

	/**
	 * calculate the maximum length of the tree, from to the given root
	 * @param tree
	 * @param node
	 * @return
	 * @throws InvalidNodeException
	 */
	public static double calculateTreeHeight(PhylogeneticTree tree, TreeNode<PhyloTreeElement> node) throws InvalidNodeException{
		double height = 0;
		if(tree.isInternal(node)){
			Iterator<TreeNode<PhyloTreeElement>> children = tree.getChildren(node);
			while(children.hasNext()){
				height = calculateTreeHeight(tree, children.next());
			}
		}
		if(!tree.isRoot(node)){
			height = Math.max((height + node.getElement().getDistance()) , node.getParent().getElement().getMaximumDistance());
			node.getParent().getElement().setMaximumDistance(height);
//			System.out.print("_h:_" + height + "_");
		}
		return height;
	}
	/**
	 * calculate the maximum length of the tree, from to the given node upward to the root
	 * @param tree
	 * @param node
	 * @return
	 * @throws InvalidNodeException
	 * @throws BoundaryViolationException
	 */
	public static double calculateTreeDepth(Tree tree, TreeNode<PhyloTreeElement> node) throws InvalidNodeException, BoundaryViolationException{
		PhyloTreeElement element = node.getElement();
		double height = element.getDistance();
		if(tree.isRoot(node)) return height;
		else height += calculateTreeDepth(tree, node.getParent());
		return height;
	}

	
}
