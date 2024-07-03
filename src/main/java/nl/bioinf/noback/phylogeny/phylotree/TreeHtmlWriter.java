package nl.bioinf.noback.phylogeny.phylotree;

import java.util.ArrayList;
import java.util.Iterator;

import nl.bioinf.noback.phylogeny.utils.BoundaryViolationException;
import nl.bioinf.noback.phylogeny.utils.EmptyTreeException;
import nl.bioinf.noback.phylogeny.utils.InvalidNodeException;
import nl.bioinf.noback.phylogeny.utils.Tree;
import nl.bioinf.noback.phylogeny.utils.TreeNode;

public class TreeHtmlWriter {

	private PhylogeneticTree tree;
	private StringBuilder output;
	private int levelNumber;
	boolean useLevelDistances;
	private double totalDistance;
	private int pixelWidth;	//width of panel
	private int textWidth = 80;	//width of text
	private int textHeight = 15;
	private int pixelHeight;//height of panel
	private int branchHeight;
	
	public void setWidth(int width){
		this.pixelWidth = width - textWidth;
	}
	
	public void setHeight(int height){
		this.pixelHeight = height;
	}

	public void setBranchSize(int pixels){
		this.branchHeight = pixels;
	}
	
	public void setTree(PhylogeneticTree tree){
		this.tree = tree;
	}
	
	public String getOutputHtml(){
		return output.toString();
	}
	
	@SuppressWarnings("unchecked")
	public void generateHtml(){
		
		TreeNode<PhyloTreeElement> root = new TreeNode<PhyloTreeElement>();
		try {
			root = (TreeNode<PhyloTreeElement>) tree.getRoot();
			root.getElement().setAvailablePixelHeight(pixelHeight);
			root.getElement().setTopPixelOffset(0);
			root.getElement().setLeftPixelOffset(0);
		} catch (EmptyTreeException e) {
			e.printStackTrace();
		}
		
		ArrayList<TreeNode<PhyloTreeElement>> list = new ArrayList<TreeNode<PhyloTreeElement>>();
		try {
			list = Tree.preorderTraversal(tree, root);
		} catch (InvalidNodeException e) {
			e.printStackTrace();
		}
		useLevelDistances = false;
		for(TreeNode<PhyloTreeElement> node : list){
			if(node.getElement().getDistance() != 0){
				useLevelDistances = true;
			}
		}
		
		if(useLevelDistances){
			try {//"(B:6.0,(A:5.0,C:3.0,E:4.0)Ancestor1:5.0,D:11.0);"
				totalDistance = PhylogeneticTree.calculateTreeHeight(tree, root);
				//System.out.println("max tree distance: " + totalDistance);
			} catch (InvalidNodeException e) {
				e.printStackTrace();
			}
			if(list.get(0).getElement().getDistance() == 0){//if root distance == 0, correct slightly
				list.get(0).getElement().setDistance(totalDistance/10);
			}

		}
		
		//first traverse list and get nesting level for level-ordering process
		//for(int elem=0; elem<list.size(); elem++){
			//TreeNode<PhyloTreeElement> treeNode = list.get(elem);
			//PhyloTreeElement element = treeNode.getElement();
			//System.out.print("||" + list.get(elem).getLevel() + "_" + element.getNodeDescription());
			//System.out.print("+" + element.getDistance());
		//}
		
		try {
			levelNumber = Tree.getHeight(tree, root);
			Tree.calculateOffspring(tree, root);
			this.output = preorderTraversal(root);
		} catch (InvalidNodeException e) {
			e.printStackTrace();
		}
	}
	/*style for container: <div id="tree" style="position:absolute;top:100px;left:10px;"></div>	
	 * CSS style for node and leaf divs:
	 * .tree_node {
	 * 	background: black;
	 * }
	 * .tree_leaf {
	 *   background: black;
	 * }
	 * */
	private StringBuilder preorderTraversal(TreeNode<PhyloTreeElement> node) throws InvalidNodeException{
		StringBuilder out = new StringBuilder();
		PhyloTreeElement element = node.getElement();
		int totalOffspring = node.getOffspring();
		int level = node.getLevel();
		int availablePixelHeight = element.getAvailablePixelHeight();
		int branchLength;
		String distanceElement;

		if(useLevelDistances){	//is a distance defined?
			double distance = element.getDistance();
			double rootDistance = 0;
			try {
				rootDistance = PhylogeneticTree.calculateTreeDepth(tree, node);
				//System.out.println("node " + element.getNodeDescription() + " has depth " + rootDistance);
			} catch (BoundaryViolationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			distanceElement = "node_length=\"" + distance + "\" root_distance=\"" + String.format("%.4f", rootDistance) + "\" ";
			branchLength = (int)(pixelWidth*(distance/totalDistance));
		}
		else{	//no distance defined
			//int distance = level;
			distanceElement = "node_length=\"1\" root_distance=\"" + level + "\" ";
			branchLength = (int)((double)(pixelWidth-textWidth)/(double)(levelNumber+1));
		}

		int topOffset = element.getTopPixelOffset();
		int left = element.getLeftPixelOffset();
		int top = (availablePixelHeight/2);
		//int containerWidth = pixelWidth-left;
		//int containerHeight = pixelHeight-topOffset;
		
		//open container for branch
		out.append("<div class=\"branch_container\" id=\"container_" + node.getNumber() + "\" ");
		out.append("style=\"top:" + topOffset + "px;left:" + left + "px;height:" + availablePixelHeight + "px;\" ");//width:" + containerWidth + "px;
		out.append(">\n");

		int verticalConnectorTop = 0;
		int verticalConnectorBottom = 0;
			
		if(tree.isInternal(node)){
			int childTopOffset = 0;
			int childNumber = 1;
			int childrenTotal = node.getNumberOfChildren();
			Iterator<TreeNode<PhyloTreeElement>> children = tree.getChildren(node);
			while(children.hasNext()){
				//recurse over children
				TreeNode<PhyloTreeElement> child = children.next();
				PhyloTreeElement childElement = child.getElement();
				int offspring = child.getOffspring();
				if(offspring == 0){
					offspring = 1;
				}
				int childAvailablePixelHeight = (int)((double)availablePixelHeight*((double)offspring/(double)totalOffspring));
				childElement.setAvailablePixelHeight(childAvailablePixelHeight);
				childElement.setTopPixelOffset(childTopOffset);
				childElement.setLeftPixelOffset(branchLength);
				
				if(childNumber == 1){
					verticalConnectorTop = (childTopOffset+(childAvailablePixelHeight/2));
//					System.out.print("first child; node pixel height: " + childAvailablePixelHeight);
//					System.out.print("; first child: availablePixelHeight: " + availablePixelHeight);
//					System.out.print("; first child: verticalConnectorTop " + verticalConnectorTop);
//					System.out.print("; first child: childTopOffset: " + childTopOffset  + "\n");
				}
				
				if(childNumber == childrenTotal){
					verticalConnectorBottom = childTopOffset+(childAvailablePixelHeight/2) + branchHeight;
//					System.out.print("last child: node pixel height: " + childAvailablePixelHeight);
//					System.out.print("; last child: availablePixelHeight: " + availablePixelHeight);
//					System.out.print("; last child: verticalConnectorBottom " + verticalConnectorBottom);
//					System.out.print("; last child: childTopOffset: " + childTopOffset  + "\n");
				}
				
				childTopOffset += childAvailablePixelHeight;
				
				
				//and recurse to output
				out.append(preorderTraversal(child));
				
				childNumber++;
			}
			
			
		}
		
		out.append("<div class=\"tree_node\" ");
		out.append("id=\"node_" +  node.getNumber() + "\" ");
		out.append("style=\"top:" + top + "px;left:" + 0 + "px;width:" + branchLength + "px;height:" + branchHeight + "px;\" ");
		out.append("level=\"" + level + "\" ");
		out.append(distanceElement);
		out.append("nodeText=\"" +  element.getNodeDescription() + "\">");
//		out.append("[-]");
		out.append("</div>\n");
		//append (invisible) view for collapsed node
		out.append("<div class=\"collapsed_node\" ");
		out.append("id=\"collapsed_node_" +  node.getNumber() + "\" ");
		out.append("style=\"visibility:hidden;top:" + top + "px;left:" + 0 + "px;width:" + branchLength + "px;height:" + branchHeight + "px;text-align:right;font-size:10px;\" ");
		out.append("level=\"" + level + "\" ");
		out.append(distanceElement);
		out.append("nodeText=\"" +  element.getNodeDescription() + "\">");
		out.append("[+]");
		out.append("</div>\n");
		
		if(node.isExternal()){
			out.append("<div class=\"tree_leaf\" ");
			out.append("style=\"id:leaf_" + node.getNumber() + ";top:" + (top-10) + "px;left:" + (branchLength+2) + "px;width:" + textWidth + "px;height:" + textHeight + "px;\">" + element.getNodeDescription() + "</div>\n");
//			out.append("level=\"" + level + "\" ");
		}
		else{
			//create the vertical branch
			out.append("<div class=\"tree_branch\" ");
			out.append("style=\"top:" + verticalConnectorTop + "px;left:" + branchLength + "px;width:" + branchHeight + "px;height:" + (verticalConnectorBottom - verticalConnectorTop) + "px;\" ");
			out.append("></div>\n");
		}
		
		//close container for branch
		out.append("\n</div>");
		return out;
	}
	
}
