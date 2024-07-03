/**
 *
 */
package nl.bioinf.noback.taxonomy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


/**
 * This class represents a node in a taxonomic tree, mapped to the NCBI taxonomy
 * DB node. It can be a leaf node (an organism) or an internal node (group; i.e.
 * genus, family etc).
 *
 * @author MA Noback (m.a.noback@pl.hanze.nl)
 * @version 0.1
 *
 */
public class TaxNode implements Comparable<TaxNode> {
    /*taxonomy ID from NCBI taxDB*/

    private int taxID;
    /*parent taxonomy ID from NCBI taxDB*/
    private int parentTaxID;
    /*idem, object reference*/
    private TaxNode parentNode;
    /*rank of this node (superkingdom, kingdom, ...)*/
    private TaxonomyRank rank;
    /*scientific name*/
    private String scientificName;
    /*a set of children; inherently unique*/
    private HashMap<Integer, TaxNode> children;
    /*cumulative child count*/
    private int cumulativeChildNumber;
    /*number of occurrences of this node*/
    private int occurrenceCount;
    /*the level in the tree or lineage, as counted from the root*/
    private int level;

    /**
     * bean constructor
     */
    public TaxNode() {
    }

    /**
     * constructs with tax_id
     *
     * @param taxId
     */
    public TaxNode(int taxId) {
        this.taxID = taxId;
    }

    /**
     * constructs with tax_id and parent tax_id
     *
     * @param taxId
     * @param parentTaxId
     */
    public TaxNode(int taxId, int parentTaxId) {
        this.taxID = taxId;
        this.parentTaxID = parentTaxId;
    }

    /**
     * returns the taxonomy ID
     *
     * @return the taxID
     */
    public int getTaxID() {
        return taxID;
    }

    /**
     * sets the taxonomy ID
     *
     * @param taxID the taxID to set
     */
    public void setTaxID(int taxID) {
        this.taxID = taxID;
    }

    /**
     * returns the parent taxonomy ID
     *
     * @return the parentTaxID
     */
    public int getParentTaxID() {
        return parentTaxID;
    }

    /**
     * returns whether this node is parent to the argument node.
     *
     * @param node
     * @return this object is parent to node
     */
    public boolean isParent(TaxNode node) {
        return (node.getParentTaxID() == this.getTaxID());
    }

    /**
     * returns whether this node is child to the argument node.
     *
     * @param node
     * @return this object is child to node
     */
    public boolean isChild(TaxNode node) {
        return (this.getParentTaxID() == node.getTaxID());
    }

    /**
     * sets the parent taxonomy ID
     *
     * @param parentTaxID the parentTaxID to set
     */
    public void setParentTaxID(int parentTaxID) {
        this.parentTaxID = parentTaxID;
    }

    /**
     * returns the parent TaxNode
     *
     * @return the parentNode
     */
    public TaxNode getParentNode() {
        return parentNode;
    }

    /**
     * sets the parent TaxNode
     *
     * @param parentNode the parentNode to set
     */
    public void setParentNode(TaxNode parentNode) throws CorruptedLineageException {
        if (parentNode.getTaxID() == this.getParentTaxID()) {
            this.parentNode = parentNode;
        } else {
            throw new CorruptedLineageException("added child node (tax_id=" + parentNode.getTaxID() + ") is no parent of this node!");
        }
    }

    /**
     * Adds a child to this node
     *
     * @param childNode
     * @throws CorruptedLineageException
     */
    public void addChild(TaxNode childNode) throws CorruptedLineageException {
        if (childNode.getParentTaxID() == this.getTaxID()) {
            if (children == null) {
                children = new HashMap<Integer, TaxNode>();
            }
            if (!children.containsKey(childNode.getTaxID())) {
                children.put(childNode.getTaxID(), childNode);
                //System.out.println("[TaxNode.addChild()] taxID=" + getTaxID() + " adds new child " + childNode + " children.size()=" + children.size());
            }
        } else {
            throw new CorruptedLineageException("added child node (tax_id=" + childNode.getTaxID() + ") is no child of this node!");
        }
    }

    /**
     * returns the number of children for this node
     *
     * @return child number
     */
    public int getChildNumber() {
        if (children == null) {
            return 0;
        }
        return children.size();
    }

    /**
     * Returns an iterator of all children of this node.
     *
     * @return iterator of children
     */
    public Iterator<TaxNode> getChildren() {
        if (children == null) {
            return new HashSet<TaxNode>().iterator();
        }

        ArrayList<TaxNode> childrenArr = new ArrayList<TaxNode>();
        childrenArr.addAll(children.values());
        Collections.sort(childrenArr);
        return childrenArr.iterator();
    }

    /**
     * Returns a single child. Use if known that only a single child exists. If
     * more children exist, a random one will be returned.
     *
     * @return single child
     */
    public TaxNode getSingleChild() {
        //return children.values().//Array()[0];
        for (TaxNode node : children.values()) {
            return node;
        }
        return null;
    }

    /**
     * Returns whether this node has children.
     *
     * @return node has children
     */
    public boolean hasChildren() {
        //System.out.println("[ TaxNode_" + getScientificName() + "_.hasChildren()] " + (children != null) );
        return children != null;
    }

    /**
     * returns the cumulative child number for this node
     *
     * @return cumulativeChildNumber
     */
    public int getCumulativeChildNumber() {
        return cumulativeChildNumber;
    }

    /**
     * sets the cumulative child number count
     *
     * @param cumulativeChildNumber
     */
    public void setCumulativeChildNumber(int cumulativeChildNumber) {
        this.cumulativeChildNumber = cumulativeChildNumber;
    }

    /**
     * returns the number of occurrences of this node in the tree
     *
     * @return occurrenceCount
     */
    public int getOccurenceCount() {
        return occurrenceCount;
    }

    /**
     * sets the number of occurrences of this node in the tree
     *
     * @param occurrenceCount
     */
    public void setOccurenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    /**
     * returns the level of this node in the containing lineage or tree
     *
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * sets the level of this node in the containing lineage or tree
     *
     * @param level the level to set
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Returns the rank of this node (superkingdom, kingdom, ...)
     *
     * @return the rank
     */
    public TaxonomyRank getRank() {
        return rank;
    }

    /**
     * sets the rank of this node (superkingdom, kingdom, ...)
     *
     * @param rank the rank to set
     */
    public void setRank(TaxonomyRank rank) {
        this.rank = rank;
    }

    /**
     * @return the scientificName
     */
    public String getScientificName() {
        return scientificName;
    }

    /**
     * @param scientificName the scientificName to set
     */
    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TaxNode:[tax_id=");
        sb.append(getTaxID());
        sb.append("; parent_tax_id=");
        sb.append(getParentTaxID());
        sb.append("; rank=");
        sb.append(getRank() == null ? "null rank" : getRank().toString());
        sb.append("; scientific_name=");
        sb.append(getScientificName() != null ? getScientificName() : "not present");
        sb.append("]");

        return sb.toString();
    }

    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null) {
            return false;
        }
        if (otherObject.getClass() != this.getClass()) {
            return false;
        }
        if (((TaxNode) otherObject).getTaxID() != this.getTaxID()) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return new Integer(getTaxID()).hashCode();
    }

    public TaxNode clone() {
        TaxNode cloned = new TaxNode();
        cloned.setOccurenceCount(this.occurrenceCount);
        cloned.setCumulativeChildNumber(this.cumulativeChildNumber);
        try {
            if (parentNode != null) {
                cloned.setParentNode(parentNode);
            }
        } catch (CorruptedLineageException e) {
        }
        cloned.setParentTaxID(parentTaxID);
        cloned.setRank(rank);
        cloned.setScientificName(scientificName);
        cloned.setTaxID(taxID);

        return cloned;
    }

    public int compareTo(TaxNode otherNode) {
        return (this.taxID - otherNode.getTaxID());
    }
}
