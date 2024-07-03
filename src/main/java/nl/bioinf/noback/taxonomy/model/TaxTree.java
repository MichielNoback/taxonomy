/**
 *
 */
package nl.bioinf.noback.taxonomy.model;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * Instances of this class will represent a taxonomic tree. Lineage objects can
 * be added to them and they will be merged into the tree.
 *
 * @author MA Noback (m.a.noback@pl.hanze.nl)
 * @version 0.1
 */
public class TaxTree {
    /*this data structure will hold all TaxNodes that together represent the tree;
     the key is the TaxNode and the value is the cumulative number of child nodes*/

    private HashMap<Integer, TaxNode> tree;
    /**
     * the root of the tree
     */
    private TaxNode root;
    
    /**
     * for searching on name
     */
    private ArrayList<TaxNode> sortedByName;
    
    /**
     * constructs an empty tree
     */
    public TaxTree() {
        this.tree = new HashMap<Integer, TaxNode>();
    }

    /**
     * constructs a tree with the root as argument
     */
    public TaxTree(TaxNode root) {
        this.tree = new HashMap<Integer, TaxNode>();
        this.root = root;
        tree.put(root.getTaxID(), root);
    }

    /**
     * Returns the size of the tree as measured by the number of constituent
     * nodes.
     *
     * @return number of nodes
     */
    public int size() {
        return tree.size();
    }

    /**
     * Returns whether the tree is empty
     *
     * @return tree is empty
     */
    public boolean isEmpty() {
        if (tree.size() == 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns the root of this tree. This will usually be node "[tax_id=1;
     * parent_tax_id=1; rank=no rank; scientific_name=root]"
     *
     * @return root of the tree
     * @throws EmptyTreeException
     */
    public TaxNode getRoot() throws EmptyTreeException {
        if (tree.isEmpty()) {
            throw new EmptyTreeException();
        }
        return root;
    }
    
    /**
     * Will make this tree keep track of a sorted names cache used for fast searching on scientific name
     * WARNING: is memory intensive!
     */
    public void createSortedNamesCache(){
        //lazy loading and only once
        if( sortedByName == null ){
            sortedByName = new ArrayList<TaxNode>();
            for( TaxNode node : this.tree.values()){
                sortedByName.add(node);
            }
            Collections.sort(sortedByName, new Comparator<TaxNode>(){
                @Override
                public int compare(TaxNode n1, TaxNode n2) {
                    return n1.getScientificName().compareTo(n2.getScientificName());
                }
            });
        }
    }

    /**
     * returns a lsit of taxnodes matching the search query
     * @param substring
     * @param anchorOnStart
     * @param ignoreCase
     * @return 
     */
    public List<TaxNode> getMatchingNodesForName(String substring, boolean anchorOnStart, boolean ignoreCase) {
        if (sortedByName == null) {
            createSortedNamesCache();
        }
        List<TaxNode> result = new ArrayList<TaxNode>();
        if (ignoreCase) {
            substring = substring.toUpperCase();
            for (TaxNode n : this.sortedByName) {
                if (anchorOnStart && n.getScientificName().toUpperCase().startsWith(substring)) {
                    result.add(n);
                } else if (!anchorOnStart && n.getScientificName().toUpperCase().contains(substring)) {
                    result.add(n);
                }
            }
        } else {
            for (TaxNode n : this.sortedByName) {
                if (anchorOnStart && n.getScientificName().startsWith(substring)) {
                    result.add(n);
                } else if (!anchorOnStart && n.getScientificName().contains(substring)) {
                    result.add(n);
                }
            }
        }
        return result;
    }
    
    /**
     * returns the species nodes that reside under the given node 
     * @param tn a TaxNode
     * @return list
     */
    public List<TaxNode> getSpeciesChildren( TaxNode node ){
        List<TaxNode> speciesChildren = new ArrayList<TaxNode>();
        getSpeciesChildren(node, speciesChildren);
        return speciesChildren;
    }

    /**
     * recursive method to support getSpeciesChildren(TaxNode node)
     * @param node
     * @param childs
     * @return childList
     */
    private List<TaxNode> getSpeciesChildren(TaxNode node, List<TaxNode> childs){
        if( node.getRank() == TaxonomyRank.SPECIES ){
            childs.add(node);
            return childs;
        }
        if( node.getRank() == TaxonomyRank.SUBSPECIES ){
            return childs;
        }
        if( node.hasChildren()) {
            Iterator<TaxNode> children = node.getChildren();
            while (children.hasNext()) {
                /*recursive call*/
                getSpeciesChildren(children.next(), childs);//append(preOrderToXml(children.next(), showChildNodes));
            }
        }
        return childs;
    }

    /**
     * returns the leaf nodes (i.e. without children) that reside under the given node 
     * @param tn a TaxNode
     * @return leafNodes
     */
    public List<TaxNode> getLeafChildren( TaxNode node ){
        List<TaxNode> leafChildren = new ArrayList<TaxNode>();
        getLeafChildren(node, leafChildren);
        return leafChildren;
    }

    /**
     * recursive method to support getLeafChildren(TaxNode node)
     * @param node
     * @param childs
     * @return childList
     */
    private List<TaxNode> getLeafChildren(TaxNode node, List<TaxNode> childs){
        if( node.hasChildren()) {
            Iterator<TaxNode> children = node.getChildren();
            while (children.hasNext()) {
                /*recursive call*/
                 getLeafChildren(children.next(), childs);//append(preOrderToXml(children.next(), showChildNodes));
            }
        }
        else{
            childs.add(node);
            return childs;
        }
        return null;
    }

    /**
     * returns the total of all occurrence counts from taxnodes where occurrence count > 0 below node in the TaxTree
     * @param node
     * @return subTreeOccurrenceCount
     */
    public int getSubTreeOccurrenceCount(TaxNode node){//, int subTreeOccurrenceCount) {
        int subTreeOccurrenceCount = 0;
        if( node.getOccurenceCount() > 0 ){
            subTreeOccurrenceCount += node.getOccurenceCount();
            //System.out.println("!!" + node + "; occurrence= " + node.getOccurenceCount() + " " + subTreeOccurrenceCount);
        }
        if (node.hasChildren()) {
            Iterator<TaxNode> children = node.getChildren();
            while (children.hasNext()) {
                /*recursive call*/
                subTreeOccurrenceCount += getSubTreeOccurrenceCount(children.next());
            }
        }
        return subTreeOccurrenceCount;
    }

    /**
     * returns the total of all occurrence counts from taxnodes where occurrence count > 0 below node in the TaxTree
     * @param node
     * @param occurrences occurrences holds all count data (to keep the tree clean) with taxID as key and count as value
     * @return subTreeOccurrenceCount
     */
    public int getSubTreeOccurrenceCount(TaxNode node, HashMap<Integer, Integer> occurrences){//, int subTreeOccurrenceCount) {
        int subTreeOccurrenceCount = 0;
        if( occurrences.containsKey(node.getTaxID()) ){//node.getOccurenceCount() > 0 ){
            subTreeOccurrenceCount += occurrences.get(node.getTaxID());
            //System.out.println("!!" + node + "; occurrence= " + node.getOccurenceCount() + " " + subTreeOccurrenceCount);
        }
        if (node.hasChildren()) {
            Iterator<TaxNode> children = node.getChildren();
            while (children.hasNext()) {
                /*recursive call*/
                subTreeOccurrenceCount += getSubTreeOccurrenceCount(children.next(), occurrences);
            }
        }
        return subTreeOccurrenceCount;
    }

    /**
     * returns the number of nodes below argument node in the TaxTree where occurrenceCount > 0
     * @param node
     * @return subTreeObservedNodesCount
     */
    public int getSubTreeObservedNodesCount(TaxNode node){
        int subTreeObservedNodesCount = 0;
        if( node.getOccurenceCount() > 0 ){
            subTreeObservedNodesCount++;
            //System.out.println("!!" + node + "; occurrence= " + node.getOccurenceCount() + " " + subTreeOccurrenceCount);
        }
        if (node.hasChildren()) {
            Iterator<TaxNode> children = node.getChildren();
            while (children.hasNext()) {
                /*recursive call*/
                subTreeObservedNodesCount += getSubTreeObservedNodesCount(children.next());
            }
        }
        return subTreeObservedNodesCount;
    }

        /**
     * returns the number of nodes below argument node in the TaxTree where occurrenceCount > 0
     * @param node
     * @param occurrences occurrences holds all count data (to keep the tree clean) with taxID as key and count as value
     * @return subTreeObservedNodesCount
     */
    public int getSubTreeObservedNodesCount(TaxNode node, HashMap<Integer, Integer> occurrences){
        int subTreeObservedNodesCount = 0;
        if( occurrences.containsKey(node.getTaxID()) ){//node.getOccurenceCount() > 0 ){
            subTreeObservedNodesCount++;
            //System.out.println("!!" + node + "; occurrence= " + node.getOccurenceCount() + " " + subTreeOccurrenceCount);
        }
        if (node.hasChildren()) {
            Iterator<TaxNode> children = node.getChildren();
            while (children.hasNext()) {
                /*recursive call*/
                subTreeObservedNodesCount += getSubTreeObservedNodesCount(children.next(), occurrences);
            }
        }
        return subTreeObservedNodesCount;
    }

    /**
     * adds a single node to the tree
     *
     * @param node
     * @throws CorruptedLineageException
     */
    public void addNode(TaxNode node) throws CorruptedLineageException {
        if (tree.containsKey(node.getTaxID())) {
            System.err.println("[TaxTree] tree already has node: " + node);
            return;
            //throw new CorruptedLineageException("node is already present in tree: " + node);
        }
        if (node.getTaxID() == 1) {
            /*this is the root node*/
            root = node;
            tree.put(node.getTaxID(), node);
            return;
        }
        if (!tree.containsKey(node.getParentTaxID())) {
            throw new CorruptedLineageException("no parent is present in tree for node " + node);
        }

        /*it is a regular node; put it in the tree and link it*/
        tree.put(node.getTaxID(), node);
        TaxNode parent = tree.get(node.getParentTaxID());
        node.setParentNode(parent);
        parent.addChild(node);
    }

    /**
     * adds a single node to the tree without linking into lineages. This method
     * should be used when an unordered collection is used to build the tree.
     * After adding all nodes, call method linkNodes() to achieve a correct tree
     * representation
     *
     * @param node
     */
    public void addUnlinkedTaxNode(TaxNode node) {
        if (tree.containsKey(node.getTaxID())) {
            System.err.println("[TaxTree] tree already has node: " + node);
            return;
            //throw new CorruptedLineageException("node is already present in tree: " + node);
        }
        if (node.getTaxID() == 1) {
            /*this is the root node*/
            root = node;
            tree.put(node.getTaxID(), node);
            return;
        }
        /*it is a regular node; put it in the tree and link it*/
        tree.put(node.getTaxID(), node);
    }

    /**
     * links the nodes of this tree (child to parent and vise versa)
     * @throws CorruptedLineageException 
     */
    public void linkNodes() throws CorruptedLineageException {
        /*iterate taxnodes and creates links of this node up to the root, or higher in the tree if links already exist*/
        for (TaxNode tn : this.tree.values()) {
            TaxNode parent = null;
            while (tn.getParentNode() == null && tn.getTaxID() != 1) {//
                if (this.tree.containsKey(tn.getParentTaxID())) {
                    parent = this.tree.get(tn.getParentTaxID());
                    try {
                        tn.setParentNode(parent);
                        tn = parent;
                    } catch (CorruptedLineageException e) {
                        throw new CorruptedLineageException("a CorruptedLineageException occurred with node "
                                + tn + " when attempting to set parent " + parent);
                    }
                } else {
                    throw new CorruptedLineageException("TaxNode without parent =" + tn);
                }
            }
        }
        /*iterate to register children with their parents*/
        this.tree.get(1).setParentTaxID(-1);
        for (TaxNode tn : this.tree.values()) {
            if( tn.getParentTaxID() != -1 ) tn.getParentNode().addChild(tn);
        }

    }

    /**
     * check whether the tree contains a node with given taxID
     *
     * @param taxID
     * @return hasNode
     */
    public boolean hasNode(int taxID) {
        return tree.containsKey(taxID);
    }

    /**
     * returns a node with given taxID
     *
     * @param taxID
     * @return
     */
    public TaxNode getNode(int taxID) throws IllegalArgumentException{
        //assert tree.containsKey(taxID) : "tree should contain node " + taxID;
        if(tree.containsKey(taxID)) return tree.get(taxID);
        else throw new IllegalArgumentException("no node with ID " + taxID + " is present in the tree");
    }

    /**
     * Merges the added lineage into the tree
     *
     * @param lineage
     * @throws CorruptedLineageException
     */
    public void addLineage(Lineage lineage) throws CorruptedLineageException {

        if (root == null) {
            root = lineage.getRoot();
            //root.setCumulativeChildNumber(0);
            //tree.put( root.getTaxID(), root );
        }
        int length = lineage.getLength();
        int nodeNumber = 0;
        int level = 1;
        Iterator<TaxNode> tni = lineage.iterateFromRoot();
        while (tni.hasNext()) {
            TaxNode tn = tni.next();
            tn.setLevel(level++);
            nodeNumber++;
            //System.out.println( "[TaxTree] adding node: " + tn );

            int taxID = tn.getTaxID();
            /*add tax node to the tree, or increment cumulative number of existing node */
            if (tree.containsKey(taxID)) {
                tree.get(taxID).setCumulativeChildNumber(tree.get(taxID).getCumulativeChildNumber() + 1);
            } else {
                tn.setCumulativeChildNumber(1);
                tree.put(taxID, tn);
            }
            /*add current node as child to its parent*/
            if (taxID != root.getTaxID()) {
                tree.get(tree.get(taxID).getParentTaxID()).addChild(tree.get(taxID));
            }

            /*last node of lineage*/
            if (nodeNumber == length) {
                tree.get(taxID).setOccurenceCount(tree.get(taxID).getOccurenceCount() + 1);
            }
        }
        //System.out.println("tree size: " + tree.size());
    }

    /**
     * returns a lineage for the given taxnode
     *
     * @param tn
     * @return lineage
     * @throws IllegalArgumentException if the node is not present in the tree
     */
    public Lineage getLineage(TaxNode tn) {
        if (!tree.containsKey(tn.getTaxID())) {
            throw new IllegalArgumentException("node " + tn + " is not present in this tree");
        }
        Lineage l = new Lineage();
        try {
            l.addNode(tn);

            while (tn.getTaxID() != 1) {
                tn = tree.get(tn.getParentTaxID());
                l.addNode(tn);
            }
        } catch (CorruptedLineageException e) {
            e.printStackTrace();
        }
        //System.out.println(l);
        //System.exit(0);
        return l;
    }

    /**
     * generates an XML representation of the tree. INcludes
     *
     * @param node
     * @return StringBuilder XML tree
     * @throws IOException
     */
    public void preOrderToXmlFile(TaxNode root, File file, String description) throws IOException {
        /*generates an XML format representation of this tree */
        //TaxTree t = new TaxTree(root); 
        StringBuilder tsb = preOrderToXml(root, true);
        PrintWriter pw = new PrintWriter(file);
        Calendar c = Calendar.getInstance();

        pw.println("<experiment>");
        pw.println("\t<id>" + file.getName() + "</id>");
        pw.println("\t<description>analysis of " + description + "</description>");
        pw.println("\t<date>" + c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH) + "-" + c.get(Calendar.DATE) + "</date>");

        int start = 0;
        for (int i = 0; i < tsb.length(); i++) {
            if (tsb.charAt(i) == '\n') {
                pw.print(tsb.subSequence(start, i));
                start = i;
            }
        }

        pw.println("</experiment>");

        pw.close();
    }

    /**
     * generates an XML representation of the tree
     *
     * @param node
     * @param showCurrentNode indicates whether this node should be displayed
     * @return StringBuilder XML tree
     */
    public StringBuilder preOrderToXml(TaxNode node, boolean showCurrentNode) {
        /*generates an XML format representation of this tree */
        StringBuilder tsb = new StringBuilder();
        if (node.equals(root)) {
            tsb.append("<tree>\n");
        }
        if (node.hasChildren()) {
            Iterator<TaxNode> children;
            boolean showChildNodes = false;

            /*check whether ANY of the children will have to be be shown. In that case, ALL children will be shown*/
            children = node.getChildren();
            while (children.hasNext()) {
                TaxNode childNode = children.next();
                if (childNode.getChildNumber() > 1
                        || (childNode.getChildNumber() == 1
                        && (childNode.getSingleChild().getCumulativeChildNumber() < childNode.getCumulativeChildNumber()))) {
                    showChildNodes = true;
                }
            }

            if (showCurrentNode) {
                tsb.append("<node id=\"");
                tsb.append(node.getTaxID());
                tsb.append("\" name=\"");
                tsb.append(node.getScientificName());
                tsb.append("\" rank=\"");
                tsb.append(node.getRank());
                tsb.append("\" level=\"");
                tsb.append(node.getLevel());
                tsb.append("\" leaf_occ=\"");
                tsb.append(node.getOccurenceCount());
                tsb.append("\" cumul_occ=\"");
                tsb.append(node.getCumulativeChildNumber());
                tsb.append("\" >\n");
                tsb.append("<children>\n");
            }

            children = node.getChildren();
            while (children.hasNext()) {
                /*recursive call*/
                tsb.append(preOrderToXml(children.next(), showChildNodes));
            }

            if (showCurrentNode) {
                tsb.append("</children>\n");
                tsb.append("</node>\n");
            }
        } else {
            tsb.append("<node id=\"");
            tsb.append(node.getTaxID());
            tsb.append("\" name=\"");
            tsb.append(node.getScientificName());
            tsb.append("\" rank=\"");
            tsb.append(node.getRank());
            tsb.append("\" level=\"");
            tsb.append(node.getLevel());
            tsb.append("\" leaf_occ=\"");
            tsb.append(node.getOccurenceCount());
            tsb.append("\" cumul_occ=\"");
            tsb.append(node.getCumulativeChildNumber());
            tsb.append("\" />\n");
        }
        if (node.equals(root)) {
            tsb.append("</tree>\n");
        }
        return tsb;
    }

    /**
     * generates an text representation of the tree example call of this method:
     *
     * taxTree.preOrderToText( taxTree.getRoot(), true, 5, 1 );
     *
     * @param node
     * @param showCurrentNode indicates whether this node should be displayed
     * @param maxLevels the maximum number of levels to display
     * @param currentLevel is the level that is currently displayed
     * @return StringBuilder XML tree
     */
    public StringBuilder preOrderToText(TaxNode node, boolean showCurrentNode, int maxLevels, int currentLevel) {
        /*generates an text representation of this tree up to a maximum number of levels*/
        StringBuilder tsb = new StringBuilder();
        if (currentLevel <= maxLevels) {
            if (node.hasChildren()) {
                Iterator<TaxNode> children;
                boolean showChildNodes = false;

                /*check whether ANY of the children will have to be be shown. In that case, ALL children will be shown*/
                children = node.getChildren();
                while (children.hasNext()) {
                    TaxNode childNode = children.next();
                    if (childNode.getChildNumber() > 1
                            || (childNode.getChildNumber() == 1
                            && (childNode.getSingleChild().getCumulativeChildNumber() < childNode.getCumulativeChildNumber()))) {
                        showChildNodes = true;
                    }
                }

                if (showCurrentNode) {
                    for (int i = 1; i < currentLevel; i++) {
                        tsb.append("\t");
                    }
                    tsb.append(node.getRank());
                    tsb.append(" ");
                    tsb.append(node.getScientificName());
                    tsb.append(": ");
                    tsb.append(node.getCumulativeChildNumber());
                    tsb.append(" (");
                    tsb.append(node.getOccurenceCount());
                    tsb.append(")\n");
                }

                currentLevel++;
                children = node.getChildren();
                while (children.hasNext()) {
                    /*recursive call*/
                    tsb.append(preOrderToText(children.next(), showChildNodes, maxLevels, currentLevel));
                }
            } else {
                for (int i = 1; i < currentLevel; i++) {
                    tsb.append("\t");
                }
                tsb.append(node.getRank());
                tsb.append(" ");
                tsb.append(node.getScientificName());
                tsb.append(": ");
                tsb.append(node.getCumulativeChildNumber());
                tsb.append(" (");
                tsb.append(node.getOccurenceCount());
                tsb.append(")\n");
            }
        }
        return tsb;
    }

    /**
     * generates an text representation of the tree example call of this method:
     *
     * taxTree.preOrderToText( taxTree.getRoot(), true, 5, 1 );
     *
     * @param node
     * @param showCurrentNode indicates whether this node should be displayed
     * @param currentLevel is the level that is currently displayed
     * @param maxOccurences the maximum number of occurrences at the base level
     * @param minimumPercentage the minimum percentage of maxOccurences that
     * should be represented to have a node displayed
     * @return StringBuilder XML tree
     */
    public StringBuilder preOrderToTextAbundance(TaxNode node, boolean showCurrentNode, int currentLevel, int maxOccurences, double minimumPercentage) {
        /*generates an text representation of this tree up to a maximum number of levels*/
        StringBuilder tsb = new StringBuilder();

        int cumOccurences = node.getCumulativeChildNumber();
        double percentage = ((double) cumOccurences / maxOccurences) * 100;

        if (percentage >= minimumPercentage) {
            if (node.hasChildren()) {
                Iterator<TaxNode> children;
                boolean showChildNodes = false;

                /*check whether ANY of the children will have to be be shown. In that case, ALL children will be shown*/
                children = node.getChildren();
                while (children.hasNext()) {
                    TaxNode childNode = children.next();
                    if (childNode.getChildNumber() > 1
                            || (childNode.getChildNumber() == 1
                            && (childNode.getSingleChild().getCumulativeChildNumber() < childNode.getCumulativeChildNumber()))) {
                        showChildNodes = true;
                    }
                }

                if (showCurrentNode) {
                    for (int i = 1; i < currentLevel; i++) {
                        tsb.append("\t");
                    }
                    tsb.append(node.getRank());
                    tsb.append(" ");
                    tsb.append(node.getScientificName());
                    tsb.append(": ");
                    tsb.append(node.getCumulativeChildNumber());
                    tsb.append(" (");
                    tsb.append(node.getOccurenceCount());
                    tsb.append(")\n");
                }

                currentLevel++;
                children = node.getChildren();
                while (children.hasNext()) {
                    /*recursive call*/
                    tsb.append(preOrderToTextAbundance(children.next(), showChildNodes, currentLevel, maxOccurences, minimumPercentage));
                }
            } else {
                for (int i = 1; i < currentLevel; i++) {
                    tsb.append("\t");
                }
                tsb.append(node.getRank());
                tsb.append(" ");
                tsb.append(node.getScientificName());
                tsb.append(": ");
                tsb.append(node.getCumulativeChildNumber());
                tsb.append(" (");
                tsb.append(node.getOccurenceCount());
                tsb.append(")\n");
            }
        }
        return tsb;
    }

    public String toString() {
        if (root == null) {
            return "<tree></tree>";
        }
        return preOrderToXml(root, true).toString();
    }

    /**
     * returns a list of top occurrences for the given Taxonomic rank (counted
     * as cumulative child number)
     *
     * @param rank the rank that is requested
     * @param amount the number of taxnodes to return; will be corrected if the
     * actual number of nodes is lower
     * @param cumulative whether the cumulative (true) or non-cumulative number
     * (false) should be used
     * @return list of taxnodes
     */
    public List<TaxNode> getTopOccurrence(TaxonomyRank rank, int amount, boolean cumulative) {
        ArrayList<TaxNode> rankNodes = new ArrayList<TaxNode>();
        for (TaxNode tn : this.tree.values()) {
            if (tn.getRank() == rank) {
                //System.out.println( this.getClass().getSimpleName() + " preselecting node " + tn);
                rankNodes.add(tn);
            }
        }
        if (cumulative) {
            Collections.sort(rankNodes, new Comparator<TaxNode>() {
                @Override
                public int compare(TaxNode t1, TaxNode t2) {
                    if (t1.getCumulativeChildNumber() > t2.getCumulativeChildNumber()) {
                        return -1;
                    } else if (t1.getCumulativeChildNumber() < t2.getCumulativeChildNumber()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
        } else {
            Collections.sort(rankNodes, new Comparator<TaxNode>() {
                @Override
                public int compare(TaxNode t1, TaxNode t2) {
                    if (t1.getChildNumber() > t2.getChildNumber()) {
                        return -1;
                    } else if (t1.getChildNumber() < t2.getChildNumber()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
        }
        amount = rankNodes.size() < amount ? rankNodes.size() : amount;
        return rankNodes.subList(0, amount);
    }

    /**
     * returns the total cumulative count of a given taxonomy level
     *
     * @param taxonomyRank
     * @return cumulativeCount
     */
    public int getTaxonomyLevelOccurrenceCount(TaxonomyRank taxonomyRank) {
        int total = 0;
        for (TaxNode tn : this.tree.values()) {
            //System.out.println( this.getClass().getSimpleName() + " preselecting node " + tn + ": " + ( tn.getRank() == taxonomyRank ));
            if (tn.getRank() == taxonomyRank) {
                //System.out.println( this.getClass().getSimpleName() + " preselecting node " + tn + "; cum child no=" + tn.getCumulativeChildNumber());
                total += tn.getCumulativeChildNumber();
            }
        }
        return total;

    }
}
