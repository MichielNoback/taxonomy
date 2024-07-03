package nl.bioinf.noback.phylogeny.mains;

import java.util.ArrayList;

import nl.bioinf.noback.phylogeny.phylotree.PhyloTreeElement;
import nl.bioinf.noback.phylogeny.utils.BoundaryViolationException;
import nl.bioinf.noback.phylogeny.utils.InvalidNodeException;
import nl.bioinf.noback.phylogeny.utils.Tree;
import nl.bioinf.noback.phylogeny.utils.TreeNode;


public class TestTree {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		Tree tree = new Tree();
		
		TreeNode<PhyloTreeElement> root = new TreeNode<PhyloTreeElement>();
		PhyloTreeElement rootElement = new PhyloTreeElement();
		rootElement.setNodeDescription("root");
		root.setElement(rootElement);
		tree.setRoot(root);
//		System.out.println("tree is empty: " + tree.isEmpty());

		TreeNode<PhyloTreeElement> firstNode = new TreeNode<PhyloTreeElement>();
		PhyloTreeElement firstNodeElement = new PhyloTreeElement();
		firstNodeElement.setNodeDescription("firstNode");
		firstNode.setElement(firstNodeElement);
		root.addChild(firstNode);
		
		TreeNode<PhyloTreeElement> secondNode = new TreeNode<PhyloTreeElement>();
		PhyloTreeElement secondNodeElement = new PhyloTreeElement();
		secondNodeElement.setNodeDescription("secondNode");
		secondNode.setElement(secondNodeElement);
		firstNode.addChild(secondNode);
		
		TreeNode<PhyloTreeElement> thirdNode = new TreeNode<PhyloTreeElement>();
		PhyloTreeElement thirdNodeElement = new PhyloTreeElement();
		thirdNodeElement.setNodeDescription("thirdNode");
		thirdNode.setElement(thirdNodeElement);
		firstNode.addChild(thirdNode);

		
		try {
			System.out.println("depth of tree at " + root.getNumber() + "  root: " + Tree.getDepth(tree, root));
			System.out.println("depth of tree at " + firstNode.getNumber() + "  firstNode: " + Tree.getDepth(tree, firstNode));
			System.out.println("depth of tree at " + secondNode.getNumber() + "  secondNode: " + Tree.getDepth(tree, secondNode));
			System.out.println("depth of tree at " + thirdNode.getNumber() + "  thirdNode: " + Tree.getDepth(tree, thirdNode));
			System.out.println("");
			System.out.println("height of tree at " + root.getNumber() + "  root: " + Tree.getHeight(tree, root));
			System.out.println("height of tree at " + firstNode.getNumber() + "  firstNode: " + Tree.getHeight(tree, firstNode));
			System.out.println("height of tree at " + secondNode.getNumber() + "  secondNode: " + Tree.getHeight(tree, secondNode));
			System.out.println("height of tree at " + thirdNode.getNumber() + "  thirdNode: " + Tree.getHeight(tree, thirdNode));
			
			
		} catch (InvalidNodeException e) {
			e.printStackTrace();
		}
		catch (BoundaryViolationException e) {
			e.printStackTrace();
		}
		
		try {
			System.out.println("");
			System.out.println("preorder traversal: ");
			ArrayList<TreeNode<PhyloTreeElement>> list = Tree.preorderTraversal(tree, root);
			for(int elem=0; elem<list.size(); elem++){
				PhyloTreeElement element = list.get(elem).getElement();
				System.out.print("__" + element.getNodeDescription());
			}

			System.out.println("");
			System.out.println("");
			
			System.out.println("postorder traversal: ");
			ArrayList<TreeNode<PhyloTreeElement>> postlist = Tree.postorderTraversal(tree, root);
			for(int elem=0; elem<postlist.size(); elem++){
				PhyloTreeElement element = postlist.get(elem).getElement();
				System.out.print("__" + element.getNodeDescription());
			}

		} catch (InvalidNodeException e1) {
			e1.printStackTrace();
		}
		

	}

}
