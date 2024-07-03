/**
 * 
 */
package nl.bioinf.noback.phylogeny.phylotree;

import java.util.Stack;

import nl.bioinf.noback.phylogeny.utils.TreeNode;

/**
 * @author Michiel
 *
 */
public class NewickTreeReader {
	
	private String newickString;
	private PhylogeneticTree tree;
	
	public String getNewickString() {
		return newickString;
	}

	public void setNewickString(String newickString) throws NewickFormatException {
		this.newickString = newickString;
		StringBuilder newickSB = new StringBuilder(newickString);
		readTree(newickSB);
	}
	
	public PhylogeneticTree getTree(){
		return tree;
	}
	
	private void readTree(StringBuilder newickSB) throws NewickFormatException {
		//create tree object
		tree = new PhylogeneticTree();

		//create stack for building tree
		Stack<TreeNode<PhyloTreeElement>> stack = new Stack<TreeNode<PhyloTreeElement>>();
		
		//create root node
		TreeNode<PhyloTreeElement> node = new TreeNode<PhyloTreeElement>();
		TreeNode<PhyloTreeElement> lastNodePopped = new TreeNode<PhyloTreeElement>();
		PhyloTreeElement nodeElement = new PhyloTreeElement();
		nodeElement.setNodeDescription("root");
		node.setElement(nodeElement);
		node.setLevel(0);
		tree.setRoot(node);
		stack.push(node);
		
		StringBuilder description = new StringBuilder();
		StringBuilder distance = new StringBuilder();
		boolean parsingDescription = true;
		boolean parsingDistance = false;
		boolean innerNode = true;
		//iterate over all characters of newick string; start at character 2 since first one is (=root
		for(int pos=1; pos<newickSB.length(); pos++){
			char character = newickSB.charAt(pos);
			switch(character){
			//"((raccoon:19.1,bear:6.80):0.84,((sea_lion:11.9, seal:12.0):7.5,(NOT  (monkey:100.85,cat:47.1):20.5,weasel:18.8):2.09NOT   ):3.8,dog:25.46);"
			//"((human,chimp,jpn),(gorilla,(uran_utan,wakaka)),makaki);";
			//"(B:6.0,(A:5.0,C:3.0,E:4.0)Ancestor1:5.0,D:11.0);"
				case '(':	//new node with children
					innerNode = true;
					node = getNewNode("node", stack.size());
					stack.peek().addChild(node);
					stack.push(node);
					break;
				case ')':	//close node with children
					if(innerNode){
						node = getNewNode((description.toString()), stack.size());
						setNodeDistance(node, distance);
						distance = new StringBuilder();
						stack.peek().addChild(node);
						description = new StringBuilder();
					}
					else{
						setNodeDistance(lastNodePopped, distance);//stack.peek()
						distance = new StringBuilder();
					}
					innerNode = false;
					lastNodePopped = stack.pop();
					parsingDistance = false;//
					parsingDescription = true;//
					break;
				case ':':	//end of description, start of distance (if present)
					parsingDescription = false;
					parsingDistance = true;
					break;
				case ',':	//new node
					parsingDistance = false;
					if(innerNode){
						node = getNewNode((description.toString()), stack.size());
						setNodeDistance(node, distance);
						distance = new StringBuilder();
						stack.peek().addChild(node);
						description = new StringBuilder();
					}
					else{//process distance of node
						setNodeDistance(lastNodePopped, distance);
						distance = new StringBuilder();
						if(description.length() == 0){
							description = new StringBuilder("node");
						}
						lastNodePopped.getElement().setNodeDescription(description.toString());//
						description = new StringBuilder();
					}
					innerNode = true;//
					parsingDescription = true;
					break;
				case ';':	//end of Newick tree description
					setNodeDistance(lastNodePopped, distance);
//					distance = new StringBuilder();
					if(!stack.isEmpty()){
						throw new NewickFormatException("error parsing Newick format");
					}
					break;
				case ' ':	//ignore spaces
					
					break;
				case '_':	//replace underscore with spaces
					description.append(' ');
					break;
				default:
					if(parsingDescription){
//						innerNode = true;
						description.append(character);
					}
					else if(parsingDistance){
						distance.append(character);
					}
					else{
						throw new NewickFormatException("error parsing string");
					}
			}
			
		}
	}
	private TreeNode<PhyloTreeElement> getNewNode(String nodeDescription, int level){
		TreeNode<PhyloTreeElement> node = new TreeNode<PhyloTreeElement>();
		PhyloTreeElement nodeElement = new PhyloTreeElement();
		nodeElement.setNodeDescription(nodeDescription);
		node.setElement(nodeElement);
		node.setLevel(level);
		return node;
	}
	private void setNodeDistance(TreeNode<PhyloTreeElement> node, StringBuilder distance){
		if(distance.length()!=0){//a distance was parsed
//			System.out.println(distance.toString());
			double distanceDouble = Double.parseDouble(distance.toString());//distance.toString()"0.123"
			node.getElement().setDistance(distanceDouble);
//			distance = new StringBuilder();
		}
	}
	
}
