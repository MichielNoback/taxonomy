/**
 * 
 */
package nl.bioinf.noback.taxonomy.mains;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import net.cellingo.sequence_tools.blast.BlastQuery;
import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.dao.DatabaseException;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDao;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoDerby;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoInMemory;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoMysql;
import nl.bioinf.noback.taxonomy.model.CorruptedLineageException;
import nl.bioinf.noback.taxonomy.model.EmptyTreeException;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxNode;
import nl.bioinf.noback.taxonomy.model.TaxTree;
import nl.bioinf.noback.taxonomy.tax_composition.BlastCategory;
import nl.bioinf.noback.taxonomy.tax_composition.BlastResultTaxonomyAnalyser;
import nl.bioinf.noback.taxonomy.tax_composition.BlastResultsReader;
import nl.bioinf.noback.taxonomy.tax_composition.BlastResultsReaderListener;
import nl.bioinf.noback.taxonomy.tax_composition.TaxonomyCompositionAnalyserSettings;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.2
 */
public class TaxonomyCompositionAnalyser implements BlastResultsReaderListener{
    /**
     * XPaths to the configuration file settings
     */
    public static final String LOGGER_NAME = "log.name";
    public static final String LOG_LEVEL = "log.level";
    public static final String LOG_FILE = "log.file";
    public static final String LOG_CONFIGFILE = "log.properties_file";

    public static final String INPUT_FILE = "input_file";
    public static final String INPUT_FILE_HAS_TAXIDS = "input_has_tax_id_field";
    public static final String REPORT_FILE = "report_file";
    public static final String TREE_FILE = "tree_file";

    public static final String DATABASE_TYPE = "database.type";
    public static final String DATABASE_NAME = "database.database_name";
    public static final String DATABASE_GI_TABLE = "database.gi_table";
    public static final String DATABASE_USER = "database.user";
    public static final String DATABASE_HOST = "database.host";
    public static final String DATABASE_PASSWORD = "database.password";
    
    public static final String TAXNODES_FILE = "database.taxnodes_file";
    public static final String GI_NUMBERS_FILE = "database.gi_numbers_file";
         
	/**
	 * the log4j logger object
	 */
	private static Logger logger;
    /**
     * the name of the config file
     */
	private String configFile;
    /**
     * The configuration settings for the database connection.
     */
    private XMLConfiguration configuration;
    /**
     * the program settings object
     */
    private TaxonomyCompositionAnalyserSettings settings;
    /**
     * the object responsible for analyzing the blast results for a 
     * single query and generating a Lineage object from it 
     */
    private BlastResultTaxonomyAnalyser blastResultsAnalyser;
    /**
     * the object used to query the taxonomy database TaxDB
     */
    private TaxonomyDao taxDB;
    /**
     * the tree that merges all lineages
     */
    private TaxTree taxTree;
    /**
     * the number of queries for which a null lineage was returned
     */
    private int nullLineages = 0;
    
	/**
	 */
	public static void main(String[] args) {
		if( args.length != 1){
			//out.println("");
			System.out.println("no configuration file provided!");
			System.out.println("usage: java -jar tax_comp_analyser_<version>.jar <configuration file>");
			System.out.println("aborting");
			System.exit( 1 );
		}
		TaxonomyCompositionAnalyser tca = new TaxonomyCompositionAnalyser( args[0] );
		tca.start();
	}
	
	/**
	 * construct with the name of the config file
	 * @param configFile
	 */
	public TaxonomyCompositionAnalyser( String configFile ){
		this.configFile = configFile;
	}

	
	public void start(){
		try {
			loadConfiguration();
			
			loadLogger();
			
			processSettings();

			//System.exit(0);
			logger.fatal("starting analysis of file " + settings.getInputFile() );

			logger.info("connecting to TaxDB");
			connectTaxDB();
			
			logger.info("instantiating blast result taxonomy analyser and taxonomic tree objects");
			boolean useGiNumbersFile = true;
			if(settings.getGiNumbersFile() == null) useGiNumbersFile = false;
			
			blastResultsAnalyser = new BlastResultTaxonomyAnalyser( taxDB, settings.isInputHasTaxIdField(), useGiNumbersFile, logger );
			//blastResultsAnalyser.setLogger( logger );
			taxTree = new TaxTree();
			
			logger.info("starting blast results processing");
			processBlastResults();
			
			logger.debug("generating report file");
			createReportFile();
			
			//processBlastCategories( blastResultsAnalyser.getBlastCategoryNumbers() );
			logger.info( "total number of hits processed:   " + blastResultsAnalyser.getHitCount() );
			//logger.info( "average number of hits per query: " + ( (double)blastResultsAnalyser.getHitCount() / queryNumber ) );
			
			logger.debug("processing the taxonomic tree of blast results");
			createTreeFile( );
			
			//blastResultsAnalyser.printTruncHistoGram();
			
			taxDB.disconnect();
			logger.fatal("Analysis finished. Exiting.");
			
		}
		catch (Exception e) {
			e.printStackTrace();
			/*all program exceptions that occur during execution converge here*/
			logger.fatal("An error occurred. Aborting.\nThe following information is available:\n" + e.getMessage() + "\nSee log for details" );
			System.exit( 0 );
		}
	}
	
	/**
	 * process the found taxonomic tree of blast results
	 * @throws Exception
	 */
	private void createTreeFile( ) throws Exception{
		File out = settings.getTreeFile();
		BufferedWriter bw = new BufferedWriter( new FileWriter( out ) );
		
		/*<experiment>
			<id>1</id>
			<description>test</description>
			<date>2010-2-10</date>*/
		Calendar c = Calendar.getInstance();
				
		bw.write("<experiment>");
		bw.newLine();
		bw.write("\t<id>" + settings.getTreeFile().getName() + "</id>");
		bw.newLine();
		bw.write("\t<description>analysis of " + settings.getInputFile().getName() + "</description>");
		bw.newLine();
		bw.write("\t<date>" + c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH) + "-" + c.get(Calendar.DATE) + "</date>");
		bw.newLine();
		bw.write( taxTree.preOrderToXml( taxTree.getRoot(), true).toString() );
		bw.newLine();
		bw.write("</experiment>");
		bw.close();
	}
	
	/**
	 * generates the summary/report file
	 */
	private void createReportFile(){
		File out = settings.getReportFile();
		HashMap<BlastCategory, Integer> blastCategoryNumbers = blastResultsAnalyser.getBlastCategoryNumbers();
		
		BufferedWriter bw;
		try {
			bw = new BufferedWriter( new FileWriter( out ) );
			Date now = new Date();

			bw.write( "SUMMARY REPORT OF TAXONOMIC ANALYSIS"   );
			bw.newLine();
			bw.newLine();
			bw.write( "DATE = " + now.toString() );//settings.getDbType()
			bw.newLine();
			String db = settings.getDbName();
			if( settings.getDbType().equalsIgnoreCase("file") ) db = settings.getNodesFile().getAbsolutePath();
			bw.write( "DATABASE = " + settings.getDbType() + "::" + db );
			bw.newLine();
			bw.write( "QUERIES PROCESSED = " + blastResultsAnalyser.getQueryCount() );
			bw.newLine();
			bw.write( "HITS PROCESSED = " + blastResultsAnalyser.getHitCount() );
			bw.newLine();
			bw.write( "AVERAGE HITS PER QUERY = " + (  (double)blastResultsAnalyser.getHitCount() / blastResultsAnalyser.getQueryCount()  ) );
			bw.newLine();
			bw.write( "NUMBER OF FAILED LINEAGES FOR HITS (NO KNOWN GI/TAXID/ORGANISM) = " + blastResultsAnalyser.getFailedLineageCount() );
			bw.newLine();
			bw.write( "NUMBER OF FAILED LINEAGES FOR QUERIES (UNKNOWN ERROR) = " + nullLineages );
			bw.newLine();
			bw.newLine();
			bw.write( "NUMBERS OF THE DIFFERENT BLAST CATEGORIES" );
			
			for( BlastCategory cat : BlastCategory.values() ){
				bw.newLine();
				bw.write( "\t" + cat + "\t" + blastCategoryNumbers.get(cat) );
			}
			bw.newLine();
			bw.newLine();
			bw.newLine();
			bw.write( "FIRST 4 LEVELS OF THE TREE [ cumulative number (leaf occurrences) ]" );
			bw.newLine();
			bw.newLine();
			
			try {
				bw.write( taxTree.preOrderToText( taxTree.getRoot(), true, 4, 1 ).toString() );
				//System.out.println( taxTree.preOrderToText( taxTree.getRoot(), true, 3, 0 ).toString() );
			} catch (EmptyTreeException e) {
				bw.write( "[TREE IS EMPTY]" );
				e.printStackTrace();
			}
			
			bw.newLine();
			bw.newLine();
			bw.write( "BRANCHES REPRESENTED >4% [ cumulative number (leaf occurrences) ]" );
			bw.newLine();
			bw.newLine();
			
			try {
				bw.write( taxTree.preOrderToTextAbundance(taxTree.getRoot(), true, 1, taxTree.getRoot().getCumulativeChildNumber(), 4).toString() );
				//System.out.println( taxTree.preOrderToText( taxTree.getRoot(), true, 3, 0 ).toString() );
			} catch (EmptyTreeException e) {
				bw.write( "[TREE IS EMPTY]" );
				e.printStackTrace();
			}
			
			bw.newLine();
			bw.newLine();
			
			bw.write( "TOP 20 SPECIES\nname\tcumulative number\tleaf occurrences\n" );
			TaxonomyRank spRank = TaxonomyRank.SPECIES;
			List<TaxNode> topSpecies = taxTree.getTopOccurrence( spRank, 20, true ); 
			for( TaxNode tn : topSpecies ){
				StringBuilder lineageSb = new StringBuilder( "[" );
				try {
					Lineage l = taxDB.getLineage( tn.getTaxID() );
					for( TaxNode t : l.getListFromRoot() ){
						if( ! (t.getScientificName().equals("root") || t.getScientificName().equals("cellular organisms") ) ){
							lineageSb.append( t.getScientificName() + ";");
						}
					}
					lineageSb.deleteCharAt(lineageSb.length()-1);
				} catch (DatabaseException e) {
					logger.error("error while trying to retrieve lineage for top-ranking node " + tn);
				}
				lineageSb.append("]");
				bw.write(tn.getScientificName() + "\t" + tn.getCumulativeChildNumber() + "\t" + tn.getOccurenceCount() + "\n");//+ "\t" + lineageSb.toString() 
			}
			
			bw.newLine();
			bw.newLine();
			bw.write( "TOP 20 GENERA\nname\tcumulative number\tleaf occurrences\n" );
			TaxonomyRank gRank = TaxonomyRank.GENUS;
			List<TaxNode> topGenera = taxTree.getTopOccurrence( gRank, 20, true ); 
			for( TaxNode tn : topGenera ){
				StringBuilder lineageSb = new StringBuilder( "[" );
				try {
					Lineage l = taxDB.getLineage( tn.getTaxID() );
					for( TaxNode t : l.getListFromRoot() ){
						if( ! (t.getScientificName().equals("root") || t.getScientificName().equals("cellular organisms") ) ){
							lineageSb.append( t.getScientificName() + ";");
						}
					}
					lineageSb.deleteCharAt(lineageSb.length()-1);
				} catch (DatabaseException e) {
					logger.error("error while trying to retrieve lineage for top-ranking node " + tn);
				}
				lineageSb.append("]");
				bw.write(tn.getScientificName() + "\t" + tn.getCumulativeChildNumber() + "\t" + tn.getOccurenceCount()  + "\n");//+ "\t" + lineageSb.toString()
			}
			bw.newLine();
			bw.newLine();
			
			bw.close();

		} catch (IOException e) {
			/*this is not a fatal exception?*/
			logger.error( "could not write to report file: " + settings.getReportFile() 
					+ "\n message is: " + e.getMessage() );
			//e.printStackTrace();
		}
	}
	
	/**
	 * processes the blast results file and registers as a Listener. This will have the method 
	 * queryDataProcessed( BlastQuery query ) called after processing of data of a single blast query
	 * @throws Exception
	 */
	private void processBlastResults() throws Exception{
		
		BlastResultsReader reader = new BlastResultsReader( settings.getInputFile(), this );
		//ArrayList<HspProperty> hspProperties = reader.getDataFields();
		reader.readFile();
	}

    /**
     * Listener method to BlastResultsReader, implements the single method of 
     * BlastResultsReaderListener that will be called when the data of a single 
     * query has been processed. This allows for streaming processing of large files
     */
        @Override
	public void queryDataProcessed( BlastQuery query ) {
		logger.debug( "QUERY     " + query.getQueryId() + " #hits=" + query.getHitNumber() );
		
		Lineage lineage = blastResultsAnalyser.analyseBlastResult( query );
		
		if( lineage == null ){
			nullLineages++;
			logger.warn( "query " + query.getQueryId() + " returned a null lineage" );
		}
		else if( lineage.getLength() == 0 ){
			nullLineages++;
			logger.warn( "query " + query.getQueryId() + " returned an empty lineage" );
		}
		else{
			/*add the lineage to the tree*/
			try {
				logger.debug( "LINEAGE LEAF  " + lineage.getExternalNode().toString());
				taxTree.addLineage(lineage);
			} catch (CorruptedLineageException e) {
				//e.printStackTrace();
				/*this is not a fatal exception*/
				logger.error( "lineage of query " + query.getQueryId() + " returned a CorruptedLineageExcetion:\n" + e.getMessage() );
			}
			/*trying to prevent heap space errors*/
			lineage = null;
		}
	}

	
	/**
	 * establish a connection to the Taxonomy database
	 */
	private void connectTaxDB() throws Exception{
		/*connect to TaxDB*/
		try {
			if( settings.getDbType() == null ){
				throw new Exception( "no database connection type was defined" );
			}
			else if( settings.getDbType().equalsIgnoreCase("file") ){
				//System.out.println( "loading TaxDB from file; using ginumbers file=" + settings.isInputHasTaxIdField() );
				if(settings.isInputHasTaxIdField()){
					taxDB = new TaxonomyDaoInMemory(settings.getNodesFile(), null);//settings.getGiNumbersFile()
					logger.info("connected to file-type database using file " + settings.getNodesFile().getName() );
				}
				else{
					taxDB = new TaxonomyDaoInMemory(settings.getNodesFile(), settings.getGiNumbersFile());
					logger.info("connected to file-type database using nodes file " + settings.getNodesFile().getName() 
							+ " and gi-numbers file " + settings.getGiNumbersFile() );
				}
			}
			else if( settings.getDbType().equalsIgnoreCase("mysql") ){
				taxDB = TaxonomyDaoMysql.getInstance( settings.getDbName(), settings.getDbHost(), settings.getDbUser(), settings.getDbPassword(), settings.getDbGiTable() );
				logger.info("connected to mysql type database");
			}
			else if( settings.getDbType().equalsIgnoreCase("derby") ){
				taxDB = TaxonomyDaoDerby.getInstance( settings.getDbName(), settings.getDbUser(), settings.getDbPassword() );
				logger.info("connected to derby type database");
			}
			else{
				throw new Exception( "unknown database connection type was requested" );
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.fatal( "unable to connect to the database: " + e.getMessage() );
			throw new Exception( "unable to connect to the database: " + e.getMessage() );
		}
	}
	
	/**
	 * processes the settings and generates a BlastFilter object
	 */
	private void processSettings() throws Exception{
		settings = new TaxonomyCompositionAnalyserSettings();

		/*input data source processing*/
		File inputFile = new File( configuration.getString( INPUT_FILE ) );
		/*read from file*/
		if( (! inputFile.exists() ) || (! inputFile.canRead() ) ){
			throw new Exception("[TaxonomyCompositionAnalyser.processSettings()] can not read from input file " + inputFile.getName() );
		}
		settings.setInputFile( inputFile );

		boolean inputHasTaxIdField = configuration.getBoolean(INPUT_FILE_HAS_TAXIDS, false);
		logger.info("parsing TaxIDs from defline of blast database hits as TAXID=12345");
		settings.setInputHasTaxIdField( inputHasTaxIdField );
		
		/*set the output file*/
		File reportFile = new File( configuration.getString( REPORT_FILE ) );
		settings.setReportFile( reportFile );
		
		/*set the output file*/
		File treeFile = new File( configuration.getString( TREE_FILE ) );
		if( ! treeFile.getName().endsWith("xml") ) logger.warn( "the tree file does not have an .xml extension " + treeFile.getName() );
		settings.setTreeFile( treeFile );
		
		/*database settings*/
		String dbType = configuration.getString( DATABASE_TYPE );
		settings.setDbType( dbType );
		
		if( dbType.equalsIgnoreCase("file")){
			File nodesFile = new File( configuration.getString( TAXNODES_FILE ) );
			/*read from file*/
			if( (! nodesFile.exists() ) || (! nodesFile.canRead() ) ){
				throw new Exception("[TaxonomyCompositionAnalyser.processSettings()] can not read from nodes file " + nodesFile.getName() );
			}
			settings.setNodesFile( nodesFile );

			String giNumbersFileName = configuration.getString( GI_NUMBERS_FILE, "NONE" );
			if( ! giNumbersFileName.equals("NONE") ){
				File giNumbersFile = new File( giNumbersFileName );
				/*read from file*/
				if( (! giNumbersFile.exists() ) || (! giNumbersFile.canRead() ) ){
					throw new Exception("[TaxonomyCompositionAnalyser.processSettings()] can not read from gi numbers file " + giNumbersFile.getName() );
				}
				settings.setGiNumbersFile( giNumbersFile );
			}
			
		}else{
			String giTable = configuration.getString( DATABASE_GI_TABLE );
			//System.out.println(configuration.getString( "blabla" ));
			if( giTable == null ) giTable = "gi_numbers";
			
//			if( configuration.containsKey("DATABASE_GI_TABLE") ){
//				giTable = configuration.getString( DATABASE_GI_TABLE );
//		        System.out.println( "##" + giTable + "##");
//		        System.exit(0);
	//
//			}
//	        System.out.println( "__" + giTable + "__");
//	        System.exit(0);
			settings.setDbGiTable(giTable);
			
			String dbName = configuration.getString( DATABASE_NAME );
			settings.setDbName( dbName );
			
			logger.info( "connecting to " + dbType + " type database: " + dbName );
			
			String dbUser = configuration.getString( DATABASE_USER );
			settings.setDbUser(dbUser);
			
			String dbHost = configuration.getString( DATABASE_HOST );
			if(dbHost != null){//only relevant for mySQL type TaxDB
				settings.setDbHost(dbHost);
			}
			
			String dbPassword = configuration.getString( DATABASE_PASSWORD );
			settings.setDbPassword(dbPassword);	
		}
		
	}
	
	/**
	 * This loads the second log4j appender. The first one is the default STDOUT that only logs fatals 
	 * to screen, which It is loaded from class folder log4j (see Referenced Libraries).
	 * This logger will use the file and logging level specified in the xml configuration file 
	 * @throws IOException 
	 */
	private void loadLogger() throws IOException{
		//PropertyConfigurator.configure( configuration.getString(LOG_CONFIGFILE) );
		if (logger == null) { // create logger if it does not exists.
		    try {
			    Layout layout = new PatternLayout("[%5p] %d{yyyy-MM-dd} %d{HH:mm:ss} %c (%F:%M:%L)%n%m%n%n");
				Appender appender = new FileAppender(layout, configuration.getString(LOG_FILE) );
				BasicConfigurator.configure( appender );
			    logger = Logger.getLogger( configuration.getString(LOGGER_NAME) );
				logger.setLevel( Level.toLevel( configuration.getString(LOG_LEVEL) ) );
			} catch (IOException e) {
				throw new IOException("[TaxonomyCompositionAnalyser.loadLogger()]: unable to configure logger" + "; cause of exception:" + e.getCause());
				//e.printStackTrace();
			}
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
                throw new Exception( "[TaxonomyCompositionAnalyser.loadConfiguration()] unable to read configuration file: " + configFile);
            }
            configuration = new XMLConfiguration( xmlConfigFile );
        } catch (Exception e) { // configuration could not be loaded
			throw new Exception("[TaxonomyCompositionAnalyser.loadConfiguration()] unable to load configuration file: " + configFile + "; cause of exception:" + e.getCause() );
        }
    }

}
