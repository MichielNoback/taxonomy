/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.bioinf.noback.taxonomy.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.model.CorruptedLineageException;
import nl.bioinf.noback.taxonomy.model.TaxNode;
import nl.bioinf.noback.taxonomy.model.TaxTree;

/**
 *
 * @author Gebruiker
 */
public class NcbiTaxonomyArchiveReader {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("no zip archive provided for NCBI taxonomy");
            System.exit(0);
        }
        NcbiTaxonomyArchiveReader ntar = new NcbiTaxonomyArchiveReader();
        try {
            TaxTree tree = ntar.readZip(args[0]);
            System.out.println("tree size: " + tree.size());
            //System.out.println("homo sapiens: " + tree.getNode(9606));
            
//            List<TaxNode> homos = tree.getMatchingNodesForName("homo", false, true);
//            for( TaxNode homo : homos ){
//                System.out.println(homo.getScientificName() + " : " + homo.getChildNumber());
//            }
//            System.out.println("===>>"+homos.toString());
            
            List<TaxNode> children = tree.getLeafChildren(tree.getNode(207598));
            for(TaxNode node : children ){
                System.out.println("" + node);
            }
        } catch (ZipException ex) {
            //ex.printStackTrace();
            Logger.getLogger(NcbiTaxonomyArchiveReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex){
            Logger.getLogger(NcbiTaxonomyArchiveReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch( CorruptedLineageException ex) {
            //ex.printStackTrace();
            Logger.getLogger(NcbiTaxonomyArchiveReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public TaxTree readZip(String zipFileName) throws ZipException, IOException, CorruptedLineageException {
        File zipFile = new File(zipFileName);
        return readZip(zipFile);
    }
    /**
     * reads the NCBI taxonomy archive from zipfile taxdmp.zip and returns a linked taxonomic tree
     * @param zipFileName
     * @return taxTree corresponding to NCBI taxonomy
     * @throws ZipException
     * @throws IOException
     * @throws CorruptedLineageException 
     */
    public TaxTree readZip(File zipFile) throws ZipException, IOException, CorruptedLineageException {
        //System.out.println("start parsing");
        ZipFile archive = new ZipFile(zipFile);
        
        /*first process the nodes file*/
        ZipEntry nodesFile = archive.getEntry("nodes.dmp");
        InputStream is = archive.getInputStream(nodesFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        int lineCount = 0;
        String line;
        TaxTree tree = null;
        TaxNode node = null;
        while ((line = br.readLine()) != null) {
            lineCount++;
            String[] elements = line.split("\\t+\\|\\t+");
            int taxID = Integer.parseInt(elements[0]);
            int parentTaxID = Integer.parseInt(elements[1]);
            String rankStr = elements[2];
            if( lineCount==1 ){
                assert taxID==1 : "First line of nodes file nodes.dmp should be root";
                TaxNode root = new TaxNode(taxID, parentTaxID);
                tree = new TaxTree(root);
            }
            else{
                TaxonomyRank rank = TaxonomyRank.getTaxonomyRank(rankStr);
                node = new TaxNode(taxID, parentTaxID);
                node.setRank(rank);
                tree.addUnlinkedTaxNode(node);
            }

        }
        //link the nodes of the tree
        tree.linkNodes();
        br.close();
        is.close();

        /*process the names file entry to get the scientifi names*/
        ZipEntry namesFile = archive.getEntry("names.dmp");
        is = archive.getInputStream(namesFile);
        br = new BufferedReader(new InputStreamReader(is));
        lineCount = 0;
        //line = null;
        while ((line = br.readLine()) != null) {
            lineCount++;
            String[] elements = line.split("\\t\\|\\t");
            
            assert elements.length == 4: "Error parsing at line " + lineCount + ": " + Arrays.toString(elements);
            
            int taxID = Integer.parseInt(elements[0]);
            
            String name = elements[1];
            String nameCat = elements[3].substring(0, elements[3].length()-2);
            
            if( nameCat.equals("scientific name") ){
                tree.getNode(taxID).setScientificName(name);
            }
        }
        //link the nodes of the tree
        tree.linkNodes();
        br.close();
        is.close();
        
        return tree;
    }
}
