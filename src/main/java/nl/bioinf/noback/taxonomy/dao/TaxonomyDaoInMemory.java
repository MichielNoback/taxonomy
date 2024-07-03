/**
 *
 */
package nl.bioinf.noback.taxonomy.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.bioinf.noback.taxonomy.model.CorruptedLineageException;
import nl.bioinf.noback.taxonomy.model.EmptyTreeException;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxNode;
import nl.bioinf.noback.taxonomy.model.TaxTree;
import nl.bioinf.noback.taxonomy.io.ParseException;
import nl.bioinf.noback.taxonomy.model.TaxonomyRank;

/**
 * This class implements the taxonomy database as in-memory loaded variant. It
 * also uses a lineage cache for efficiency
 *
 * @author michiel
 *
 */
public class TaxonomyDaoInMemory implements TaxonomyDao {

    /**
     * the file containing the TaxDB nodes that are required for the blast
     */
    private File nodesFile;
    /**
     * the file containing the links between gi numbers and theor tax_id s
     */
    private File giNumbersFile;
    /**
     * map that stores the gi numbers from the sequences file and later adds the
     * tax_ids from the gi_numbers file
     */
    private HashMap<Integer, Integer> giNumbersTaxIds;
    /**
     * map that stores the relevant taxnodes with key of tax_id
     */
    private HashMap<Integer, TaxNode> taxNodes;
    /**
     * for performance, the retrieved lineages are cache in memory
     */
    private HashMap<Integer, Lineage> lineageCache;
    /**
     * a cache for when lineages are retrieved based on organism name
     */
    private HashMap<String, Lineage> lineageNameCache;
    /**
     * for heap space safety reasons, a maximum cache size is set
     */
    private int cacheSize = 10000;
    /**
     * the number of cached lineages
     */
    private int cachedLineages = 0;
    private int cachedReturns = 0;//TODO remove after development
    private int lineageRequests = 0;//TODO remove after development
    public static int queryCount = 0;
    private boolean analyseInformationGain;
    private double stronglySupportedCutoffCount = 0.0001;
    private PrintWriter pw;
    private TaxNode root;

    /**
     * load with a nodes and gi numbers file
     *
     * @param nodesFile
     * @throws ParseException
     * @throws CorruptedLineageException
     */
    public TaxonomyDaoInMemory(File nodesFile, File giNumbersFile) throws IOException, ParseException, CorruptedLineageException {
        this.nodesFile = nodesFile;
        this.giNumbersFile = giNumbersFile;
        load();
    }

    /**
     * load with only a nodes file
     *
     * @param nodesFile
     * @throws ParseException
     * @throws CorruptedLineageException
     */
    public TaxonomyDaoInMemory(File nodesFile) throws IOException, ParseException, CorruptedLineageException {
        this.nodesFile = nodesFile;

        load();

    }

    /**
     * @return the taxNodes
     */
    public HashMap<Integer, TaxNode> getTaxNodes() {
        return taxNodes;
    }

    /**
     * loads all data
     *
     * @throws IOException
     * @throws CorruptedLineageException
     */
    private void load() throws IOException, ParseException, CorruptedLineageException {
        this.taxNodes = new HashMap<Integer, TaxNode>();
        this.lineageCache = new HashMap<Integer, Lineage>();
        if (giNumbersFile != null) {
            this.giNumbersTaxIds = new HashMap<Integer, Integer>();
            loadGiNumbers();
        }
        loadNodes();
        /*link the node*/
        linkNodes();

        /*for testing purposes: TODO remove this call*/
        //countNodes();
        analyseInformationGain = false;
        if (analyseInformationGain) {

            try {//TODO remove this block
                File testFile = new File("/share/home/michiel/projects/metagenomics/BGA_reads/information_gain/"
                        + "information_gain_bacref_filtered_000p1_IsStrong.csv");
                pw = new PrintWriter(testFile);
                pw.println("queries\tlineage requests\tcached lineages\tstrongly supported nodes\tcached returns");
                //System.out.println( "lineageRequests\tcachedLineages\tcachedReturns" );
                System.out.println(this.getClass().getSimpleName() + " number of nodes in taxDB: " + taxNodes.size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //System.out.println( this.getClass().getSimpleName() + ".load() TaxDB loaded with " + taxNodes.size() + " nodes" );
    }

    /**
     * counts all nodes that have been linked
     */
    public void countNodes() {
        Set<Integer> taxids = new HashSet<Integer>(1000);

        if (giNumbersTaxIds != null) {
            //int c = (giNumbersTaxIds==null ? 0 : giNumbersTaxIds.size() );
            System.out.println("[ " + this.getClass().getSimpleName() + " ] gi numbers present in collection: " + giNumbersTaxIds.size());

            /*create a set of unique tax_ids*/
            for (int taxid : giNumbersTaxIds.values()) {
                taxids.add(taxid);
            }

            System.out.println("[ " + this.getClass().getSimpleName() + " ] there are " + taxids.size() + " unique tax_ids found ");
            TaxTree tree = new TaxTree();

            /*iterate over gi numbers and fetch lineage */
            for (int taxid : taxids) {
                try {
                    Lineage l = getLineage(taxid);
                    tree.addLineage(l);
                } catch (DatabaseException e) {
                    System.err.println(e.getMessage());
                    //e.printStackTrace();
                } catch (CorruptedLineageException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("[ " + this.getClass().getSimpleName() + " ] the size of the tree loaded from gi_taxid information is " + tree.size());
        }

        /*count the nodes that have been added to the tree*/
        int treeNodesCount = 0;
        int singleNodesCount = 0;
        for (TaxNode n : taxNodes.values()) {
            if (n.getCumulativeChildNumber() > 0) {
                treeNodesCount++;
            } else {
                singleNodesCount++;
            }
        }

        System.out.println("[ " + this.getClass().getSimpleName() + " ] there are " + treeNodesCount
                + " nodes with > 0 children; there are " + singleNodesCount + " nodes with 0 children");

        int linkedCount = 0;
        int unlinkedCount = 0;
        int total = 0;
        /*iterate taxnodes and creates links of this node up to the root, or higher in the tree if links already exist*/
        for (TaxNode tn : taxNodes.values()) {
            total++;
            if (tn.getParentNode() == null) {
                unlinkedCount++;
            } else {
                linkedCount++;
            }
        }
        System.out.println("[ " + this.getClass().getSimpleName() + " ] Total number of nodes in loaded TaxDB: " + total
                + "; unlinkedCount=" + unlinkedCount
                + "; linkedCount=" + linkedCount
                + "; taxnodes.size()=" + taxNodes.size());
    }

    /**
     * processes the dump file that holds the link between gi numbers and
     * tax_ids
     *
     * @return giNumbersProcessed
     * @throws IOException
     * @throws ParseException
     */
    private void loadGiNumbers() throws IOException, ParseException {
        int lineNumber = 0;
        //int matched = 0;
        BufferedReader br = null;
        try {

            /*read from file*/
            if ((!giNumbersFile.exists()) || (!giNumbersFile.canRead())) {
                throw new IOException("can not read from input file " + giNumbersFile.getName());
            }

            br = new BufferedReader(new FileReader(giNumbersFile));
            /*gi_number	tax_id*/
            String line;
            String[] elements;

            /*skip first line*/
            line = br.readLine();

            while ((line = br.readLine()) != null) {
                lineNumber++;

                elements = line.split("\t");
                try {
                    int gi = Integer.parseInt(elements[0]);
                    int taxId = Integer.parseInt(elements[1]);
                    giNumbersTaxIds.put(gi, taxId);

                } catch (Exception e) {
                    throw new ParseException("error parsing file " + giNumbersFile.getName()
                            + ": line does not have correct format: " + line + " at line " + lineNumber);
                }
            }
        } catch (IOException e) {
            throw new IOException("file processing failed at line " + lineNumber + ": " + e.getMessage());
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * loads node data
     *
     * @throws IOException
     */
    private void loadNodes() throws IOException, ParseException, CorruptedLineageException {
        int lineNumber = 0;
        BufferedReader br = null;
        try {
            /*read from file*/
            if ((!nodesFile.exists()) || (!nodesFile.canRead())) {
                throw new IOException("can not read from input file " + nodesFile.getName());
            }

            br = new BufferedReader(new FileReader(nodesFile));
            String line;
            String[] elements;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                /*tax_id \t parent tax_id \t rank \t name*/
                /*2	131567	superkingdom	Bacteria*/
                elements = line.split("\t");
                try {
                    int taxId = Integer.parseInt(elements[0]);
                    int pTaxId = Integer.parseInt(elements[1]);
                    String rank = elements[2];
                    String name = elements[3];

                    //System.out.println("tax_id=" + taxId + " p_tax_id=" + pTaxId + " rank=" +rank);

                    TaxNode tn = new TaxNode(taxId, pTaxId);
                    tn.setRank(TaxonomyRank.getTaxonomyRank(rank));
                    tn.setScientificName(name);

                    taxNodes.put(taxId, tn);

                    //TODO remove after development
                    if (taxId == 1) {
                        this.root = tn;
                    }
                    //System.out.println( taxNodes.get(taxId) );

                } catch (NumberFormatException e) {
                    throw new ParseException("error parsing file " + nodesFile.getName()
                            + ": line does not have correct format: " + line + " at line " + lineNumber);
                }
            }
        } catch (IOException e) {
            throw new IOException("file processing failed at line " + lineNumber + ": " + e.getMessage());
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * creates lineage links between all nodes
     *
     * @param node
     */
    private void linkNodes() throws CorruptedLineageException {
        /*iterate taxnodes and creates links of this node up to the root, or higher in the tree if links already exist*/
        for (TaxNode tn : taxNodes.values()) {
            TaxNode parent = null;

            //System.out.println(this.getClass().getSimpleName() + " creating links for TaxNode=" + tn);

            while (tn.getParentNode() == null && tn.getTaxID() != 1) {

                if (taxNodes.containsKey(tn.getParentTaxID())) {
                    parent = taxNodes.get(tn.getParentTaxID());

                    //System.out.println(this.getClass().getSimpleName() + " tax_id=" + tn.getTaxID() + " parent=" + parent.getTaxID() );

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
    }

    /* (non-Javadoc)
     * @see nl.bioinf.noback.taxonomy.dao.TaxonomyDao#disconnect()
     */
    @Override
    public void disconnect() throws DatabaseException {
        //System.out.println( "lineageRequests\tcachedLineages\tcachedReturns" );
        if (pw != null) {
            pw.close();
        }
        /*void method*/
    }

    /* (non-Javadoc)
     * @see nl.bioinf.noback.taxonomy.dao.TaxonomyDao#getGiLineage(int)
     */
    @Override
    public Lineage getGiLineage(int giNumber) throws DatabaseException {
        if (giNumbersFile == null) {
            throw new DatabaseException("giNumbers file is not loaded: check your configuration file");
        }
        if (giNumbersTaxIds.containsKey(giNumber)) {
            int taxId = giNumbersTaxIds.get(giNumber);
            return getLineage(taxId);
        } else {
            throw new DatabaseException("giNumber is not represented in database: " + giNumber);
        }
    }

    private int countStronglySupportedLineages(double cutoffCount) {
        int count = 0;
        int rootTotal = root.getCumulativeChildNumber();
        for (TaxNode n : taxNodes.values()) {
            if (((double) n.getCumulativeChildNumber() / rootTotal) > cutoffCount) {
                count++;
            }
        }
        return count;

    }

    /* (non-Javadoc)
     * @see nl.bioinf.noback.taxonomy.dao.TaxonomyDao#getLineage(int)
     */
    @Override
    public Lineage getLineage(int taxID) throws DatabaseException {

        //TODO remove this after testing

        if (analyseInformationGain) {
            lineageRequests++;

            if (lineageRequests <= 1000 && lineageRequests % 100 == 0) {
                int stronglySupportedCount = countStronglySupportedLineages(stronglySupportedCutoffCount);
                //System.out.println( lineageRequests	+ "\t" + cachedLineages	+ "\t" + cachedReturns + "\t" + stronglySupportedCount);
                pw.println(queryCount
                        + "\t" + lineageRequests
                        + "\t" + cachedLineages
                        + "\t" + stronglySupportedCount
                        + "\t" + cachedReturns);
            } else if (lineageRequests <= 500000 && lineageRequests % 1000 == 0) {
                int stronglySupportedCount = countStronglySupportedLineages(stronglySupportedCutoffCount);
                //System.out.println( this.getClass().getSimpleName() + " returned " + cachedReturns + " cached lineages"  );
                pw.println(queryCount + "\t" + lineageRequests + "\t" + cachedLineages + "\t" + stronglySupportedCount + "\t" + cachedReturns);
            } else if (lineageRequests % 500000 == 0) {
                int stronglySupportedCount = countStronglySupportedLineages(stronglySupportedCutoffCount);
                //System.out.println( this.getClass().getSimpleName() + " returned " + cachedReturns + " cached lineages"  );
                pw.println(queryCount + "\t" + lineageRequests + "\t" + cachedLineages + "\t" + stronglySupportedCount + "\t" + cachedReturns);
            }
        }


        if (lineageCache.containsKey(taxID)) {
            cachedReturns++;
            return lineageCache.get(taxID);
        } else {
            List<TaxNode> nodesList = new ArrayList<TaxNode>();
            Lineage lineage = null;
            try {
                if (taxNodes.containsKey(taxID)) {
                    TaxNode tn = taxNodes.get(taxID);
                    nodesList.add(tn);
                    while (tn.getParentNode() != null) {//only the root should have a null parent
                        tn = tn.getParentNode();
                        nodesList.add(tn);
                    }
                } else {
                    throw new DatabaseException("taxID is not represented in database: " + taxID);
                }
                //System.out.println( this.getClass().getSimpleName() + " lineage list: " + nodesList.toString() );

                lineage = new Lineage(nodesList);
            } catch (CorruptedLineageException e) {
                e.printStackTrace();
            }
            if (cachedLineages < cacheSize) {
                //System.out.println( "caching lineage for tax_id=" + taxID );
                lineageCache.put(lineage.getExternalNode().getTaxID(), lineage);
                cachedLineages++;

//				if( cachedLineages % 20 == 0 ){
//					System.out.println( this.getClass().getSimpleName() + " lineage cache size=" + cachedLineages );
//				}
            } else {
                System.out.println(this.getClass().getSimpleName() + " exceeding cache size at " + cacheSize);
            }
            return lineage;
        }
    }

    @Override
    public Lineage getLineage(String organismName) throws DatabaseException {
        if (lineageNameCache != null && lineageNameCache.containsKey(organismName)) {
            return lineageNameCache.get(organismName);
        } else {
            for (TaxNode tn : taxNodes.values()) {
                if (tn.getScientificName().equals(organismName)) {
                    Lineage l = getLineage(tn.getTaxID());
                    if (lineageNameCache == null) {
                        lineageNameCache = new HashMap<String, Lineage>();
                    }
                    lineageNameCache.put(organismName, l);
                    cachedLineages++;
                    return l;
                }
            }
            throw new DatabaseException("no TaxNode found with scientific name " + organismName);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        TaxonomyDaoInMemory tdim;
        try {

//			File testFile = new File("/share/home/michiel/projects/metagenomics/BGA_reads/information_gain/" +
//			"information_gain_90percent_0p01_OneIsStrong.csv");
//			PrintWriter pw = new PrintWriter(testFile);
//			pw.println("queries\tlineage requests\tcached lineages\tstrongly supported nodes\tcached returns");
//			//System.out.println( "lineageRequests\tcachedLineages\tcachedReturns" );
//			//System.out.println( TaxonomyDaoInMemory.class.getSimpleName() + " number of nodes in taxDB: " + taxNodes.size() )
//			pw.close();
//			System.exit(0);

            System.out.println("starting test");
            tdim = new TaxonomyDaoInMemory(new File("/share/home/michiel/Desktop/TaxDB_complete.dmp"), null);
            tdim.countNodes();

            File genomesListFile = new File("/data/michiel/bac_ref/download/summary_26112010.txt");
            BufferedReader br = new BufferedReader(new FileReader(genomesListFile));
            TaxTree t = new TaxTree();
            int count = 0;
            int failed = 0;
            try {
                String line = null;
                while ((line = br.readLine()) != null) {
                    String[] lineElements = line.split("\t");
                    count++;
                    if (count > 1) {
                        try {
                            Lineage l = tdim.getLineage(Integer.parseInt(lineElements[3]));
                            System.out.println("taxID=" + lineElements[3] + " was present in database");
                            t.addLineage(l);
                        } catch (Exception e) {
                            failed++;
                            System.err.println("taxID=" + lineElements[3] + " was not present in database");
                        }
                    }
                    br.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(count + " lines read; " + failed + " taxIDs not present");
            try {
                t.preOrderToXmlFile(t.getRoot(), new File("/share/home/michiel/projects/metagenomics/docs/bac_ref_tree.xml"), "test");
            } catch (EmptyTreeException e1) {
                e1.printStackTrace();
            }

//			System.out.println( tdim.getLineage(521098) );
//			System.out.println( tdim.getGiLineage(258510725) );
//			System.out.println( tdim.getLineage("Alicyclobacillus acidocaldarius") );

//			Lineage l1 = tdim.getLineage(521098);
//			Lineage l2 = tdim.getLineage(313603);
//			Lineage l3 = tdim.getLineage(94122);
//			Lineage l4 = tdim.getLineage(204773);
//			
//			TaxTree tree = new TaxTree( );
//			tree.addLineage( l1 );
//			tree.addLineage( l2 );
//			tree.addLineage( l3 );
//			tree.addLineage( l4 );

            //TaxTree t = new taxTree();
            try {
                //TaxTree.preOrderToXmlFile( tdim.root, new File("/share/home/michiel/Desktop/tree.xml") );
                System.out.println("END WRITING");
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("end test");
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (CorruptedLineageException e) {
            e.printStackTrace();
        }
    }
}
