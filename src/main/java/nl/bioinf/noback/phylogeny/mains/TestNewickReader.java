package nl.bioinf.noback.phylogeny.mains;

import java.util.ArrayList;

import nl.bioinf.noback.phylogeny.phylotree.NewickFormatException;
import nl.bioinf.noback.phylogeny.phylotree.NewickTreeReader;
import nl.bioinf.noback.phylogeny.phylotree.PhyloTreeElement;
import nl.bioinf.noback.phylogeny.utils.EmptyTreeException;
import nl.bioinf.noback.phylogeny.utils.InvalidNodeException;
import nl.bioinf.noback.phylogeny.utils.Tree;
import nl.bioinf.noback.phylogeny.utils.TreeNode;

public class TestNewickReader {

	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		String newickTreeNoDistances = "((human,chimp,jpn),(gorilla,(uran_utan,wakaka)),makaki);";
//		String newickTreeNodeNames = "(B:6.0,(A:5.0,C:3.0,E:4.0)Ancestor1:5.0,D:11.0);";
//		String newickTreeOne = "((raccoon:19.1,bear:6.80):0.84,((sea_lion:11.9,seal:12.0):7.5,((monkey:100.85,cat:47.1):20.5,weasel:18.8):2.09):3.8,dog:25.46);"; 
//		String newickTreeTwo = "(Bovine:0.69395,(Gibbon:0.36079,(Orang:0.33636,(Gorilla:0.17147,(Chimp:0.19268, Human:0.11927):0.08386):0.06124):0.15057):0.54939,Mouse:1.21460):0.10;";
//		String newickTreeThree = "(Bovine: 0.69395,( Hylobates:0.36079,(Pongo:0.33636,(G._Gorilla:0.17147, (P._paniscus:0.19268,H._sapiens:0.11927):0.08386):0.06124):0.15057):0.54939, Rodent:1.21460);";
		
		NewickTreeReader treeReader = new NewickTreeReader();
		try {
			treeReader.setNewickString(newickTreeNoDistances);
		} catch (NewickFormatException e) {
			e.printStackTrace();
		}
		Tree tree = treeReader.getTree();

		System.out.println("inlevel traversal: ");
		TreeNode<PhyloTreeElement> root = new TreeNode<PhyloTreeElement>();
		try {
			root = (TreeNode<PhyloTreeElement>) tree.getRoot();
		} catch (EmptyTreeException e) {
			e.printStackTrace();
		}
		
		ArrayList<TreeNode<PhyloTreeElement>> list = new ArrayList<TreeNode<PhyloTreeElement>>();
		try {
			list = Tree.preorderTraversal(tree, root);
			//calculate the offspring at each node
			Tree.calculateOffspring(tree, root);
		} catch (InvalidNodeException e) {
			e.printStackTrace();
		}
		for(int elem=0; elem<list.size(); elem++){
			PhyloTreeElement element = list.get(elem).getElement();
			System.out.print("||" + list.get(elem).getLevel() + "_" + element.getNodeDescription());
			System.out.print("+" + list.get(elem).getOffspring());
			//System.out.print("+" + element.getDistance());
		}

		System.out.println("");
		
		
		/*
		NewickTreeReader treeReader2 = new NewickTreeReader();
		try {
			treeReader2.setNewickString(newickTreeNoDistances);
		} catch (NewickFormatException e) {
			e.printStackTrace();
		}
		Tree tree2 = treeReader2.getTree();

		System.out.println("inlevel traversal: ");
		TreeNode<PhyloTreeElement> root2 = new TreeNode<PhyloTreeElement>();
		try {
			root2 = tree2.getRoot();
		} catch (EmptyTreeException e) {
			e.printStackTrace();
		}

		ArrayList<TreeNode<PhyloTreeElement>> list2 = new ArrayList<TreeNode<PhyloTreeElement>>();
		try {
			list2 = Tree.preorderTraversal(tree2, root2);//= Tree.inLevelTraversal(tree2, root2);
		} catch (InvalidNodeException e) {
			e.printStackTrace();
		}
		for(int elem=0; elem<list2.size(); elem++){
			PhyloTreeElement element = list2.get(elem).getElement();
			System.out.print("||" + list2.get(elem).getLevel() + "_" + element.getNodeDescription());
			//System.out.print("+" + element.getDistance());
		}

		System.out.println("");

		NewickTreeReader treeReader3 = new NewickTreeReader();
		try {
			treeReader3.setNewickString(newickTreeTwo);
		} catch (NewickFormatException e) {
			e.printStackTrace();
		}
		Tree tree3 = treeReader3.getTree();

		System.out.println("inlevel traversal: ");
		TreeNode<PhyloTreeElement> root3 = new TreeNode<PhyloTreeElement>();
		try {
			root3 = tree3.getRoot();
		} catch (EmptyTreeException e) {
			e.printStackTrace();
		}

		ArrayList<TreeNode<PhyloTreeElement>> list3 = new ArrayList<TreeNode<PhyloTreeElement>>();
		try {
			list3 = Tree.preorderTraversal(tree3, root3);//= Tree.inLevelTraversal(tree2, root2);
		} catch (InvalidNodeException e) {
			e.printStackTrace();
		}
		for(int elem=0; elem<list3.size(); elem++){
			PhyloTreeElement element = list3.get(elem).getElement();
			System.out.print("||" + list3.get(elem).getLevel() + "_" + element.getNodeDescription());
			System.out.print("+" + element.getDistance());
		}
	
*/	
	}

}
