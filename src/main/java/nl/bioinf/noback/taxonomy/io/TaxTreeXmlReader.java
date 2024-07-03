/**
 * 
 */
package nl.bioinf.noback.taxonomy.io;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import nl.bioinf.noback.taxonomy.dao.DatabaseException;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoInMemory;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxonomyExperiment;
import nl.bioinf.noback.taxonomy.model.CorruptedLineageException;
import nl.bioinf.noback.taxonomy.model.TaxNode;
import nl.bioinf.noback.taxonomy.model.TaxTree;

import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.*;

/**
 * Objects of this class can be used to read the XML representation of a taxonomy analysis.
 * Such an xml representation should have this basic structure:
 * <experiment>
 *   <id>run_rdp_tax_tree.xml</id>
 *   <description>analysis of run17062010_rrna_hits.csv</description>
 *   <date>2010-8-21</date>
 *   <tree>
 *     <node id="1" name="root" rank="no rank" level="1" leaf_occ="0" cumul_occ="18997" >
 *       <children>
 *         <node id="131567" name="cellular organisms" rank="no rank" level="2" leaf_occ="1" cumul_occ="18997" >
 *       </children>
 *     </node>
 *   </tree>
 * </experiment>
 * 
 * The method start() takes an xml file of this type as argument and returns a TaxonomyExperiment object which 
 * contains the TaxTree object and header information
 * 
 * Usage: 
 *  TaxTreeXmlReader ttxr = new TaxTreeXmlReader();
 *  try {
 *    TaxonomyExperiment te = ttxr.start( xmlFileName );
 *  } catch (ParseException e) {
 *    e.printStackTrace();
 *  }
 *  
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 1.0
 */
public class TaxTreeXmlReader implements ErrorHandler {
	public static final String DOCUMENT_ELEMENT = "experiment";
	public static final String EXPERIMENT_ID = "id";
	public static final String EXPERIMENT_DESCRIPTION = "description";
	public static final String EXPERIMENT_DATE = "date";
	public static final String EXPERIMENT_TREE = "tree";
	public static final String TAX_NODE = "node";
	public static final String NODE_CHILDREN = "children";
	public static final String TAX_NODE_ATTRIBUTE_ID = "id";
	public static final String TAX_NODE_ATTRIBUTE_NAME = "name";
	public static final String TAX_NODE_ATTRIBUTE_RANK = "rank";
	public static final String TAX_NODE_ATTRIBUTE_LEVEL = "level";
	public static final String TAX_NODE_ATTRIBUTE_LEAF_OCC = "leaf_occ";
	public static final String TAX_NODE_ATTRIBUTE_CUMUL_OCC = "cumul_occ";
	/*<node id="933" name="Thiomicrospira" rank="genus" level="8" leaf_occ="3" cumul_occ="3" />*/
	
	//private File file;
	private Document document;
	//private DocumentBuilder builder;
	private TaxonomyDaoInMemory taxDB;


	public TaxTreeXmlReader(TaxonomyDaoInMemory taxDB) {
		this.taxDB = taxDB;
	}

	/**
	 * 
	 * @param xmlFileName, expName
	 * @return TaxonomyExperiment
	 * @throws ParseException
	 */
	public TaxonomyExperiment start( String[] xmlFileName) throws ParseException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			builder.setErrorHandler(this);
			
			document = builder.parse( new File(xmlFileName[0]) );
			
			/*process the document tree to a TaxTree*/
			return processTaxonomyExperiment(xmlFileName[1]);
		
		}catch (IOException e){
			e.printStackTrace();
			throw new ParseException(" an IOException occurred: " + e.getMessage());
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new ParseException(" an ParserConfigurationException occurred: " + e.getMessage());
		} catch (SAXException e) {
			e.printStackTrace();
			throw new ParseException(" an SAXException occurred: " + e.getMessage());
		} catch (ParseException e) {
			e.printStackTrace();
			throw e;
		} catch (DatabaseException e) {
			e.printStackTrace();
			throw new ParseException(" an DatabaseException occurred: " + e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param displayName
	 * @return
	 * @throws ParseException
	 * @throws DatabaseException 
	 */
	private TaxonomyExperiment processTaxonomyExperiment( String displayName ) throws ParseException, DatabaseException{
		String expId = null;
		String expDesc = null;
		Calendar expDate = null;
		TaxTree taxTree = null;
				
		/*traverse the child nodes of the document element ( => <experiment> )*/
		NodeList nodes = document.getDocumentElement().getChildNodes();
		for (int i=0; i<nodes.getLength() ; i++){
			Node n = nodes.item(i);
			//System.out.println("Child " + i + " type " + n.getNodeType() + " => \"" + n.getNodeName() + "\":" + n.getNodeValue() );
			
			//System.out.println( "Node.ELEMENT_NODE=" + Node.ELEMENT_NODE );
			
			if( n.getNodeType() == Node.ELEMENT_NODE  ){
				/*it is a content node*/
				
				if( n.getNodeName().equals( EXPERIMENT_ID )){
					expId = n.getTextContent();
					//System.out.println("expID=\"" + expId + "\"" );
				}
				else if( n.getNodeName().equals( EXPERIMENT_DESCRIPTION )){
					expDesc = n.getTextContent();
					//System.out.println("expDesc=\"" + expDesc + "\"" );
				}
				else if( n.getNodeName().equals( EXPERIMENT_DATE )){
					/*2010-8-21*/
					String expDateStr = n.getTextContent();
					String[] elmnts = expDateStr.split("-");
					expDate = Calendar.getInstance( );
					expDate.set(Integer.parseInt(elmnts[0]), Integer.parseInt(elmnts[1]), Integer.parseInt(elmnts[2]));
					//System.out.println("expDate=\"" + expDate + "\"" );
				}
				else if( n.getNodeName().equals( EXPERIMENT_TREE )){
					taxTree = processTaxTree(n);
					//System.out.println("expDate=\"" + expDate + "\"" );
				}
				else{
					throw new ParseException("unknown node is found in the tree file: " + n.getNodeName());
				}
			}
		}
		
		if( expId == null ) throw new ParseException(" the experiment ID is null");
		if( expDate==null ) throw new ParseException(" the experiment date is null");
		if( taxTree == null) throw new ParseException(" the TaxTree is null");
		
		return new TaxonomyExperiment(displayName, expId, expDesc, expDate, taxTree);
	}
	
	/**
	 * processes the actual tree node
	 * @param treeNode
	 * @return
	 * @throws ParseException
	 * @throws DatabaseException 
	 */
	private TaxTree processTaxTree(Node treeNode) throws ParseException, DatabaseException {
		TaxTree t = new TaxTree();

		if( treeNode.getChildNodes().getLength() != 3 ){
			throw new ParseException("there should be exactly one root node to the tree, not " + treeNode.getChildNodes().getLength() );
		}
		/*there are three nodes; second is the "real" node*/
		Node root = treeNode.getChildNodes().item(1);
		if( root.getNodeType() != Node.ELEMENT_NODE ){
			throw new ParseException("this should be the root, not " + treeNode.getNodeName() );
		}
		
		if( root.getNodeName().equals(TAX_NODE) ){// && n.getTextContent().equals("root") ){
			/*it is a content node*/
			processTaxNodes( root, 0, t );
			
			//System.out.println( "parsed tree:\n" + t.toString());
			
		}
		return t;
	}
	
	
	
//	/**
//	 * processes an xml node and returns the corresponding TaxNode object
//	 * @param xmlNode
//	 * @param the parent TaxID of the node being processed
//	 * @param tree 
//	 * @throws ParseException 
//	 */
//	private void processTaxNodes(Node xmlNode, int parentTaxId, TaxTree tree) throws ParseException {
//		TaxNode tn = new TaxNode();
//		tn.setParentTaxID(parentTaxId);
//		
//		int taxID = 0;
//		NamedNodeMap attr = xmlNode.getAttributes();
//		for( int j=0; j<attr.getLength(); j++ ){
////			System.out.println( xmlNode.getNodeName() + " attribute " + attr.item(j) + "    "
////					+ ";" + attr.item(j).getNodeName() 
////					+ ";"+ attr.item(j).getNodeValue()
////					+ ";"+ attr.item(j).getNodeType()
////					+ ";"+ attr.item(j).getTextContent());
//			
//			if( attr.item(j).getNodeName().equals(TAX_NODE_ATTRIBUTE_ID) ){
//				taxID = Integer.parseInt( attr.item(j).getNodeValue() );
//				tn.setTaxID( taxID );
//			}
//			else if( attr.item(j).getNodeName().equals(TAX_NODE_ATTRIBUTE_NAME) ){
//				tn.setScientificName( attr.item(j).getNodeValue() );
//			}
//			else if( attr.item(j).getNodeName().equals(TAX_NODE_ATTRIBUTE_RANK) ){
//				TaxonomyRank rank = TaxonomyRank.getTaxonomyRank(attr.item(j).getNodeValue());
//				tn.setRank( rank );
//			}
//			else if( attr.item(j).getNodeName().equals(TAX_NODE_ATTRIBUTE_LEVEL) ){
//				tn.setLevel( Integer.parseInt( attr.item(j).getNodeValue() ) );
//			}
//			else if( attr.item(j).getNodeName().equals(TAX_NODE_ATTRIBUTE_CUMUL_OCC) ){
//				tn.setCumulativeChildNumber( Integer.parseInt( attr.item(j).getNodeValue() ) );
//			}
//			else if( attr.item(j).getNodeName().equals(TAX_NODE_ATTRIBUTE_LEAF_OCC) ){
//				tn.setOccurenceCount( Integer.parseInt( attr.item(j).getNodeValue() ) );
//			}
//			else{
//				throw new ParseException("unknown attribute found: " + attr.item(j) );
//			}
//		}
//		try {
//			tree.addNode(tn);
//		} catch (CorruptedLineageException e) {
//			e.printStackTrace();
//			ParseException pe = new ParseException( "corrupted tree when parsing node " + tn + "; cause:" + e.getCause() );
//			throw pe;
//		}
//		//System.out.println( tn );
//		
//		/*parse possible children; first on xml child nodes*/
//		NodeList children  = xmlNode.getChildNodes();
//		for( int i=0; i<children.getLength(); i++ ){
//			Node child = children.item(i);
//			if( child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(NODE_CHILDREN) ){
//				/*parse possible children; now on taxnode child nodes*/
//				NodeList childTaxNodes = child.getChildNodes();
//				for( int j=0; j<childTaxNodes.getLength(); j++ ){
//					Node childTaxNode = childTaxNodes.item(j);
//					if( childTaxNode.getNodeType() == Node.ELEMENT_NODE && childTaxNode.getNodeName().equals(TAX_NODE) ){
//						/*recurse on child TaxNodes*/
//						processTaxNodes(childTaxNode, taxID, tree);
//					}
//				}
//			}
//		}
//	}
	
	/**
	 * processes an xml node and returns the corresponding TaxNode object
	 * @param xmlNode
	 * @param the parent TaxID of the node being processed
	 * @param tree 
	 * @throws ParseException 
	 * @throws DatabaseException 
	 */
	private void processTaxNodes(Node xmlNode, int parentTaxId, TaxTree tree) throws ParseException {
		int taxID = 0;
		NamedNodeMap attr = xmlNode.getAttributes();
		Lineage l = null;
		
		int cumOccurrences = -1;
		int leafOccurrences = -1;
		for( int j=0; j<attr.getLength(); j++ ){
			if( attr.item(j).getNodeName().equals(TAX_NODE_ATTRIBUTE_ID) ){
				taxID = Integer.parseInt( attr.item(j).getNodeValue() );
				try{
					l = taxDB.getLineage(taxID);
				}catch (DatabaseException e) {
					System.err.println("TaxID not in taxDB: " + taxID);
				}
			}
			else if( attr.item(j).getNodeName().equals(TAX_NODE_ATTRIBUTE_CUMUL_OCC) ){
				cumOccurrences = Integer.parseInt( attr.item(j).getNodeValue() );
			}
			else if( attr.item(j).getNodeName().equals(TAX_NODE_ATTRIBUTE_LEAF_OCC) ){
				leafOccurrences = Integer.parseInt( attr.item(j).getNodeValue() );
			}
		}
		if( cumOccurrences == -1 || leafOccurrences == -1 || l == null){
			//throw new ParseException("an error occurred during xml file parsing; cumOccurrences=" + cumOccurrences 
			//		+ " leafOccurrences=" + leafOccurrences + " lineage=" + l);
			System.err.println("an error occurred during xml file parsing for taxID=" + taxID + "; cumOccurrences=" + cumOccurrences 
							+ " leafOccurrences=" + leafOccurrences + " lineage=" + l);
		}
		else{
			try {
				//l.getExternalNode().setCumulativeChildNumber(cumOccurrences);
				l.getExternalNode().setOccurenceCount(leafOccurrences);
				for( TaxNode tn : l.getListFromRoot() ){
					tn.setCumulativeChildNumber(cumOccurrences);
					//tn.setOccurenceCount(leafOccurrences);
					//System.out.println("leaf node=" + tn);
					if( ! tree.hasNode(tn.getTaxID() ) ){
						tree.addNode( tn.clone() );
					}
				}
			} catch (CorruptedLineageException e) {
				e.printStackTrace();
				ParseException pe = new ParseException( "corrupted tree when parsing lineage " + l + "; cause:" + e.getCause() );
				throw pe;
			}
		}
		//System.out.println( tn );
		
		/*parse possible children; first on xml child nodes*/
		NodeList children  = xmlNode.getChildNodes();
		for( int i=0; i<children.getLength(); i++ ){
			Node child = children.item(i);
			if( child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(NODE_CHILDREN) ){
				/*parse possible children; now on taxnode child nodes*/
				NodeList childTaxNodes = child.getChildNodes();
				for( int j=0; j<childTaxNodes.getLength(); j++ ){
					Node childTaxNode = childTaxNodes.item(j);
					if( childTaxNode.getNodeType() == Node.ELEMENT_NODE && childTaxNode.getNodeName().equals(TAX_NODE) ){
						/*recurse on child TaxNodes*/
						processTaxNodes(childTaxNode, taxID, tree);
					}
				}
			}
		}
	}
	@Override
	public void error(SAXParseException e) throws SAXException {
		System.err.println( "an error occurred: " + e.getMessage() );
		e.printStackTrace();
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		System.err.println( "a fatal error occurred: " + e.getMessage() );
		e.printStackTrace();
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
		System.err.println( "a warning was issued: " + e.getMessage() );
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java TaxTreeXmlReader xmlfilename");
            System.exit(1);
        }
        TaxonomyDaoInMemory taxDB = null;
		try {
			taxDB = new TaxonomyDaoInMemory( new File("/share/home/michiel/projects/taxonomy/TaxDB/TaxDB_complete.dmp"), null);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (CorruptedLineageException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		TaxTreeXmlReader ttxr = new TaxTreeXmlReader(taxDB);
		try {
			TaxonomyExperiment te = ttxr.start( args );
			System.out.println( "TaxonomyExperiment=" + te.toString() );
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}


}
