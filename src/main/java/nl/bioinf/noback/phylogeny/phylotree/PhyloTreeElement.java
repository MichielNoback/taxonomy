package nl.bioinf.noback.phylogeny.phylotree;

import java.util.ArrayList;

public class PhyloTreeElement {
	private String nodeDescription;
	private String organism;
	private ArrayList<String> lineage;
	private String sequenceIdentifier;
	private double distance;
	private double maximumDistance;
	private int availablePixelHeight;
	private int topPixelOffset;
	private int leftPixelOffset;
	
	/**
	 * get the total/cumulative maximum distance from this node to the root
	 * @return total distance
	 */
	public double getMaximumDistance() {
		return maximumDistance;
	}
	/**
	 * set the total cumulative distance to the root
	 * @param maximumDistance
	 */
	public void setMaximumDistance(double totalDistance) {
		this.maximumDistance = totalDistance;
	}
	/**
	 * get the distance to the root of the tree
	 * @return distance
	 */
	public double getDistance() {
		return distance;
	}
	/**
	 * set the distance to the root of the tree
	 * @param distance to root
	 */
	public void setDistance(double distance) {
		this.distance = distance;
	}
	/**
	 * get the node text
	 * @return
	 */
	public String getNodeDescription() {
		return nodeDescription;
	}
	/**
	 * set the node text
	 * @param nodeText
	 */
	public void setNodeDescription(String nodeDescription) {
		this.nodeDescription = nodeDescription;
	}
	/**
	 * get the lineage of this node
	 * @return lineage
	 */
	public ArrayList<String> getLineage() {
		return lineage;
	}
	/**
	 * set the lineage
	 * @param lineage
	 */
	public void setLineage(ArrayList<String> lineage) {
		this.lineage = lineage;
	}
	/**
	 * get the organism
	 * @return
	 */
	public String getOrganism() {
		return organism;
	}
	/**
	 * set the organism
	 * @param organism
	 */
	public void setOrganism(String organism) {
		this.organism = organism;
	}
	/**
	 * get the sequence identifier for this node
	 * @return
	 */
	public String getSequenceIdentifier() {
		return sequenceIdentifier;
	}
	/**
	 * set a sequence identifier for this node
	 * @param sequenceIdentifier
	 */
	public void setSequenceIdentifier(String sequenceIdentifier) {
		this.sequenceIdentifier = sequenceIdentifier;
	}
	/**
	 * get the available height (pixels) fro this branch of the tree
	 * @return available height in pixels
	 */
	public int getAvailablePixelHeight() {
		return availablePixelHeight;
	}
	/**
	 * get the available height (pixels) fro this branch of the tree
	 * @param available height in pixels
	 */
	public void setAvailablePixelHeight(int availablePixelHeight) {
		this.availablePixelHeight = availablePixelHeight;
	}
	/**
	 * get the pixel offset from the top of the tree panel
	 * @return top pixel offset
	 */
	public int getTopPixelOffset() {
		return topPixelOffset;
	}
	/**
	 * set the pixel offset from the top of the tree panel
	 * @param top pixel offsets
	 */
	public void setTopPixelOffset(int topPixelOffset) {
		this.topPixelOffset = topPixelOffset;
	}
	/**
	 * get the pixel offset from the left of the tree panel
	 * @return left pixel offset
	 */
	public int getLeftPixelOffset() {
		return leftPixelOffset;
	}
	/**
	 * set the pixel offset from the left of the tree panel
	 * @param left pixel offset
	 */
	public void setLeftPixelOffset(int leftPixelOffset) {
		this.leftPixelOffset = leftPixelOffset;
	}

}
