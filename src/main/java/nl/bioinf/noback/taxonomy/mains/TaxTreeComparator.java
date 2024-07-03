package nl.bioinf.noback.taxonomy.mains;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoInMemory;
import nl.bioinf.noback.taxonomy.io.TaxTreeXmlReader;
import nl.bioinf.noback.taxonomy.model.TaxonomyExperiment;
import nl.bioinf.noback.taxonomy.tax_analysis.AnalysisType;
import nl.bioinf.noback.taxonomy.tax_analysis.TaxTreeAnalyser;
import nl.bioinf.noback.taxonomy.io.ParseException;

import org.apache.commons.configuration.XMLConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Performs comparisons between taxonomic trees and produces textual reports of the analyses
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.1
 */
public class TaxTreeComparator {
    public static final String REPORT_FILE = "report_file";
    public static final String ANALYSIS_TYPE = "analysis_type";
    public static final String ANALYSIS_ATTRIBUTES = "analysis_attributes";
    public static final String TAXNODES_FILE = "taxnodes_file";
    
    
	/*the config file*/
	private String configFile;

    /* The configuration file turned to object. */
    private XMLConfiguration configuration;
    
    /*the type of analysis to perform on the trees*/
    private AnalysisType analysisType;
    
    /*a simp*/
    Properties analysisProperties;
//    Map<String, String> analysisAttributes
    
    /* The taxtree experiment objects, both as map and list for convenient id-based access and easy iterating*/
    //private HashMap<String, TaxonomyExperiment> experimentsMap = new HashMap<String, TaxonomyExperiment>();
    private ArrayList<TaxonomyExperiment> experimentsList = new ArrayList<TaxonomyExperiment>();
	private File reportFile;
	private Properties defaultAnalysisProperties;
	private File taxnodesFile;
	private TaxonomyDaoInMemory taxDB;
    
	public TaxTreeComparator(String configFile) {
		this.configFile = configFile;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java TaxTreeComparator configFile");
            System.exit(1);
        }
		TaxTreeComparator ttc = new TaxTreeComparator( args[0] );
		try {
			ttc.start( );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void start() throws Exception{
		
		/*createDefaultProperties*/
		createDefaultProperties();
		
		/*read configuration*/
		loadConfiguration();
		
		/**/
		processSettings();
		
		/*load taxDB*/
		this.taxDB = new TaxonomyDaoInMemory( taxnodesFile, null);
		
		/*get experiment filenames*/
		List<String[]> expFiles = getExperimentFileNames();
		
		/*parse the experiment files containing the tax trees*/
		parseExperimentFiles( expFiles );
		
		/*do analysis*/
		TaxTreeAnalyser tta = TaxTreeAnalyser.getInstance( analysisType, analysisProperties );
		tta.analyse(reportFile, experimentsList);
		
	}
	
	/**
	 * processes the XML tree experiment files in given list
	 * @param expFiles as file, displayname
	 * @throws ParseException
	 */
	private void parseExperimentFiles(List<String[]> expFiles) throws ParseException{
		TaxTreeXmlReader ttxr = new TaxTreeXmlReader(taxDB);
		for( String[] exp : expFiles ){
			//System.out.println("expFileName " + expFileName);
			
			TaxonomyExperiment te = ttxr.start(exp);
			//System.out.println("te " + te);
			//experimentsMap.put( te.getId(), te );
			experimentsList.add( te );
		}
	}
	
	/**
	 * parses the XMLconfiguration for file names of tax exp files
	 * @return fNamesList
	 */
	private List<String[]> getExperimentFileNames(){
		List<String[]> fNames = new ArrayList<String[]>();
		Document doc = configuration.getDocument();
		NodeList fNodes = doc.getElementsByTagName("tax_experiment");
		for( int i=0; fNodes.item(i) != null; i++ ){
			Node fNode = fNodes.item(i);
			NodeList fChildNodes = fNode.getChildNodes();//ElementsByTagName("tax_exp_file");
			String[] set = new String[2];
			
			//System.out.println("fNode " + fNode);
			for( int j=0; fChildNodes.item(j) != null; j++ ){
				Node fChildNode = fChildNodes.item(j);
				
				if( fChildNode.getNodeName().equals("file")){
					fNames.add(set);
					set[0] = fChildNode.getTextContent();
				
				}
				else if( fChildNode.getNodeName().equals("name")){
					set[1] = fChildNode.getTextContent();
				}
			}
		}
		//for( String[] pair : fNames ){
		//	System.out.println( Arrays.toString(pair) );
		//}
		//System.exit(0);
		
		return fNames;
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
		
		/*create the output report file*/
		reportFile = new File( configuration.getString( REPORT_FILE ) );
		//System.out.println(reportFile.getAbsolutePath());
		if( reportFile.exists() && !reportFile.delete() ) throw new IOException("can not delete existing report file: " + reportFile.getAbsolutePath());
		if( !(reportFile.createNewFile() && reportFile.canWrite() ) ) throw new IOException("can not write to report file: " + reportFile.getAbsolutePath());
		
		/*get the requested type of analysis*/
		String analysisTypeStr = configuration.getString(ANALYSIS_TYPE);
		try{
			analysisType = AnalysisType.valueOf(analysisTypeStr);
		}catch (Exception e) {
			throw new IOException("unknown type of analysis requested: " + analysisTypeStr);
		}
		
		/*get the analysis type attributes */
		analysisProperties = new Properties(defaultAnalysisProperties);
		String analysisAttr = configuration.getString(ANALYSIS_ATTRIBUTES);
		if( analysisAttr != null && analysisAttr.length() > 0){
			//analysisAttributes = new HashMap<String, String>();
			String[] attributes = analysisAttr.split(";");
			for( String attribute : attributes ){
				String[] propPair = attribute.split("=");
				analysisProperties.put(propPair[0], propPair[1]);
			}
		}
//		if( analysisProperties != null && analysisProperties.getProperty("node_count_cutoff") != null ){
//			System.out.println("setting node_count_cutoff to unlimited");
//		}
		//System.out.println("analysisProperties " + analysisProperties);
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
                throw new Exception( "[TaxonomyCompositionAnalyser.loadConfiguration()] unable to read configuration file: " + configFile);
            }
            configuration = new XMLConfiguration( xmlConfigFile );
            
        } catch (Exception e) { // configuration could not be loaded
			throw new Exception("[TaxonomyCompositionAnalyser.loadConfiguration()] unable to load configuration file: " + configFile + "; cause of exception:" + e.getCause() );
        }
    }

    /**
     * creates the default Properties object
     */
    private void createDefaultProperties(){
    	this.defaultAnalysisProperties = new Properties();
    	defaultAnalysisProperties.setProperty("node_count_cutoff", Integer.MAX_VALUE+"");
    	defaultAnalysisProperties.setProperty("cumulative", "true");
    	
    }
}
