/**
 * 
 */
package nl.bioinf.noback.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;

import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoInMemory;
import nl.bioinf.noback.taxonomy.io.TaxTreeXmlReader;
import nl.bioinf.noback.taxonomy.model.EmptyTreeException;
import nl.bioinf.noback.taxonomy.model.TaxNode;
import nl.bioinf.noback.taxonomy.model.TaxNodeFilter;
import nl.bioinf.noback.taxonomy.model.TaxonomyExperiment;

import org.apache.commons.configuration.XMLConfiguration;

/**
 * Converts the xml tree format to Megan import format
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.1
 */
public class XmlToMeganConverter {
    public static final String XML_TREE_FILE = "input_xml_file";
    public static final String MEGAN_FORMAT_FILE = "output_megan_file";
    public static final String TAXNODES_FILE = "taxnodes_file";
    public static final String TAXLEVELS = "taxonomy_levels";
    public static final String OCCURRENCE_CUTOFF_TYPE = "occurrence_cutoff_type";
    public static final String OCCURRENCE_CUTOFF_VALUE = "occurrence_cutoff_value";
    
	/*the config file*/
	private String configFile;

    /* The configuration file turned to object. */
    private XMLConfiguration configuration;
    private File inputFile;
	private File meganFile;
	private File taxnodesFile;
	private TaxonomyDaoInMemory taxDB;
	
	/*occurrence cutoff defaults to 1*/
	private TaxNodeLevelFilter nodeFilter;
	private double occurrenceCutoffValue;
	private TaxonomyExperiment taxonomyExperiment;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
//		for( TaxonomyRank tr : TaxonomyRank.values() ){
//			if( TaxonomyRank.isShortLineage(tr)){
//				System.out.print(tr.name() + ", ");
//			}
//		}
//		System.exit(0);
        if (args.length != 1) {
            System.err.println("Usage: java -jar xml2megan_converter.jar configFile");
            System.exit(1);
        }
        XmlToMeganConverter xtmc = new XmlToMeganConverter( args[0] );
		try {
			xtmc.start( );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * constructs with config file
	 * @param configFile
	 */
	public XmlToMeganConverter(String configFile){
		this.configFile = configFile;
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception{
		/*read configuration*/
		loadConfiguration();
		
		/**/
		processSettings();
		
		/*load taxDB*/
		this.taxDB = new TaxonomyDaoInMemory( taxnodesFile, null);
		
		/*parse xml tree*/
		TaxTreeXmlReader ttxr = new TaxTreeXmlReader(taxDB);
		this.taxonomyExperiment = ttxr.start(new String[]{ inputFile.getAbsolutePath(), "exp"});
		//System.out.println(te.toString());
		
		/*to output*/
		writeMeganFile();
	}
	
	/**
	 * writes to megan output
	 * @param te
	 * @throws FileNotFoundException 
	 * @throws EmptyTreeException 
	 */
	private void writeMeganFile() throws FileNotFoundException, EmptyTreeException {
		PrintWriter pw = new PrintWriter(this.meganFile);
		TaxNode root = taxonomyExperiment.getTree().getRoot();
		for( TaxonomyRank tr : this.nodeFilter.getTaxonomyLevels().keySet() ){
			
			int levelCount = taxonomyExperiment.getTree().getTaxonomyLevelOccurrenceCount(tr);
			//System.out.println( "RANK " + tr.toString() + " levelcount=" + levelCount );
			nodeFilter.getTaxonomyLevels().put(tr, levelCount);
		}
		//te.getTree().get
		toMeganFile(pw, root);
		pw.close();
	}
	
	/**
	 * 
	 * @param pw
	 * @param tn
	 */
	private void toMeganFile(PrintWriter pw, TaxNode tn ){
		if( tn.getTaxID() == 1 || this.nodeFilter.filterNode(tn) ){
			pw.println(tn.getTaxID() + ", " + tn.getOccurenceCount() );
			//System.out.println( " ====>> " + tn.toString() );
		}
		if( tn.hasChildren() ){
			Iterator<TaxNode> tnIt = tn.getChildren();
			while( tnIt.hasNext() ){
				toMeganFile( pw, tnIt.next() );
			}
		}
		//return
		else return;
	}

	/**
	 * processes the configuration settings
	 * @throws IOException 
	 */
	private void processSettings() throws IOException{
		/*create taxonomy database*/
		String taxnodesFileName = configuration.getString( TAXNODES_FILE );
		if( taxnodesFileName == null || taxnodesFileName.length() == 0 ) throw new IOException("please provide a taxnodes file in tag <taxnodes_file>");
		this.taxnodesFile = new File( taxnodesFileName );
		if( ! taxnodesFile.exists() || ! taxnodesFile.canRead() ) throw new IOException("can not read from taxnodes file: " + taxnodesFileName);
		
		/*input data source processing*/
		inputFile = new File( configuration.getString( XML_TREE_FILE ) );
		if( inputFile == null || ! inputFile.canRead() ){
			throw new IOException("[XmlToMeganConverter()] can not read from input file");
		}
		
		/*create the output report file*/
		meganFile = new File( configuration.getString( MEGAN_FORMAT_FILE ) );
		//System.out.println(reportFile.getAbsolutePath());
		if( meganFile.exists() && !meganFile.delete() ) throw new IOException("can not delete existing report file: " + meganFile.getAbsolutePath());
		if( !(meganFile.createNewFile() && meganFile.canWrite() ) ) throw new IOException("can not write to report file: " + meganFile.getAbsolutePath());
	
		/*process the selected tax levels*/
		HashMap<TaxonomyRank, Integer> taxonomyLevels = new HashMap<TaxonomyRank, Integer>();
		String taxLevels = configuration.getString( TAXLEVELS );
		if( taxLevels == null || taxLevels.length() == 0){
			for( TaxonomyRank tr : TaxonomyRank.values() ){
				taxonomyLevels.put(tr,0);
			}
		}
		else if( taxLevels.equalsIgnoreCase("SHORT_LINEAGE")){
			for( TaxonomyRank tr : TaxonomyRank.values() ){
				if( TaxonomyRank.isShortLineage(tr)){
					taxonomyLevels.put(tr,0);
				}
			}
		}
		else{
			String[] taxLevelsArr = taxLevels.split(";");
			for( String taxlevel : taxLevelsArr ){
				try{
					TaxonomyRank tr = TaxonomyRank.valueOf(taxlevel);
					taxonomyLevels.put(tr,0);
				}catch (IllegalArgumentException e) {
					throw new IOException("can not parse taxonomy level: " + taxlevel);
				}
			}
		}
		
		/*OCCURRENCE_CUTOFF		COUNT, LEVEL_PERCENTAGE or OVERALL_PERCENTAGE*/
		String occCutoffValueStr = configuration.getString( OCCURRENCE_CUTOFF_VALUE, ""+0 );
		occurrenceCutoffValue = Double.parseDouble(occCutoffValueStr);

		String cutoffType = configuration.getString( OCCURRENCE_CUTOFF_TYPE, "COUNT" );
		if(cutoffType.equalsIgnoreCase("COUNT")){
			if( occurrenceCutoffValue < 0 )throw new IllegalArgumentException("illegal cutoff occurence level: " + occurrenceCutoffValue);
			nodeFilter = new TaxNodeLevelCountFilter(taxonomyLevels);
		}
		else if(cutoffType.equalsIgnoreCase("LEVEL_PERCENTAGE")){
			if( occurrenceCutoffValue < 0 || occurrenceCutoffValue > 100 )throw new IllegalArgumentException("illegal cutoff occurence level: " + occurrenceCutoffValue);
			nodeFilter = new TaxNodeLevelPercentageFilter(taxonomyLevels);
		}
		else if(cutoffType.equalsIgnoreCase("OVERALL_PERCENTAGE")){
			if( occurrenceCutoffValue < 0 || occurrenceCutoffValue > 100 )throw new IllegalArgumentException("illegal cutoff occurence level: " + occurrenceCutoffValue);
			nodeFilter = new TaxNodeOverallPercentageFilter(taxonomyLevels);
		}

	}
	
	/**
     * The loadConfiguration method loads the configuration for the application from file.
     * If this configuration cannot be loaded, a ConfigurationException 
     * will be thrown.
     * It will also start the logger if it has yet been started
     * @throws Exception
     */
    private void loadConfiguration( ) throws Exception {
    	//System.out.println("loading configuration file");
        try {
            File xmlConfigFile = new File( configFile );
            if ( ! xmlConfigFile.exists() || ! xmlConfigFile.canRead() ) {
            	System.out.println( "unable to read configuration file: " + configFile );
                throw new Exception( "[XmlToMeganConverter()] unable to read configuration file: " + configFile);
            }
            configuration = new XMLConfiguration( xmlConfigFile );
            
        } catch (Exception e) { // configuration could not be loaded
			throw new Exception("[XmlToMeganConverter] unable to load configuration file: " + configFile + "; cause of exception:" + e.getCause() );
        }
    }
    
    /**
     * abstract class to be used here for filtering
     * @author michiel
     */
    private abstract class TaxNodeLevelFilter implements TaxNodeFilter{
    	private HashMap<TaxonomyRank, Integer> taxonomyLevels = new HashMap<TaxonomyRank, Integer>();
    	
    	public TaxNodeLevelFilter( HashMap<TaxonomyRank, Integer> taxonomyLevels ){
    		this.taxonomyLevels = taxonomyLevels;
    	}
    	
    	public HashMap<TaxonomyRank, Integer> getTaxonomyLevels(){
    		return taxonomyLevels;
    	}
    	
    }
    
    /**
     * simple count filter
     */
    private class TaxNodeLevelCountFilter extends TaxNodeLevelFilter{
		public TaxNodeLevelCountFilter(HashMap<TaxonomyRank, Integer> taxonomyLevels) {
			super(taxonomyLevels);
		}
		@Override
		public boolean filterNode(TaxNode node) {
			
			return (getTaxonomyLevels().containsKey(node.getRank()) 
					&& node.getCumulativeChildNumber() >= occurrenceCutoffValue);				
		}
    }
    
    /**
     * percentage filter filters for level %
     */
    public class TaxNodeLevelPercentageFilter extends TaxNodeLevelFilter{
		public TaxNodeLevelPercentageFilter(HashMap<TaxonomyRank, Integer> taxonomyLevels) {
			super(taxonomyLevels);
		}
		@Override
		public boolean filterNode(TaxNode node) {
			if( ! getTaxonomyLevels().containsKey(node.getRank() ) ) return false;
			assert getTaxonomyLevels().get(node.getRank()) > 0 : ("requesting node " + node + ": " + getTaxonomyLevels().toString());
			
			double percentage = ((double)node.getCumulativeChildNumber()/getTaxonomyLevels().get(node.getRank()))*100 ;
			//System.out.println("percentage of node " + node + " with level count=" + getTaxonomyLevels().get(node.getRank()) + ": " + percentage);
			return ( getTaxonomyLevels().containsKey(node.getRank())
					&& ( percentage >= occurrenceCutoffValue) );				
		}
    }
    
    /**
     * percentage filter filters on root level %
     */
    public class TaxNodeOverallPercentageFilter extends TaxNodeLevelFilter{
		public TaxNodeOverallPercentageFilter(HashMap<TaxonomyRank, Integer> taxonomyLevels) {
			super(taxonomyLevels);
		}
		@Override
		public boolean filterNode(TaxNode node) {
			if( ! getTaxonomyLevels().containsKey(node.getRank() ) ) return false;
			try {
				//System.out.println("requesting node " + node + ": " + getTaxonomyLevels().toString());
				int rootCount = taxonomyExperiment.getTree().getRoot().getCumulativeChildNumber();
				assert rootCount > 0;
				
				double percentage = ((double)node.getCumulativeChildNumber()/rootCount)*100;
				//System.out.println("percentage of node " + node + " with rootcount=" + rootCount + ": " + percentage);
				return ( getTaxonomyLevels().containsKey(node.getRank())
						&& (percentage >= occurrenceCutoffValue ) );				
			} catch (EmptyTreeException e) {
				e.printStackTrace();
			}
			return false;
		}
		
    }
}
