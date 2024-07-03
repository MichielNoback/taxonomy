/**
 * 
 */
package nl.bioinf.noback.taxonomy.mains;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.dao.DatabaseException;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDao;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoMysql;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxNode;
import nl.bioinf.noback.taxonomy.io.ParseException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * @author michiel
 *
 */
public class TaxDbLiteMySqlFiller {
    /**
     * XPaths to the configuration file settings
     */
    public static final String LOGGER_NAME = "log.name";
    public static final String LOG_LEVEL = "log.level";
    public static final String LOG_FILE = "log.file";

    public static final String GI_TAXID_FILE = "gi_taxid_file";
    public static final String SEQUENCE_FILE = "sequence_file";
    
    /*NCBI TaxDB*/
    public static final String DATABASE_NAME = "database.database_name";
    public static final String DATABASE_USER = "database.user";
    public static final String DATABASE_HOST = "database.host";
    public static final String DATABASE_PASSWORD = "database.password";
    /*TaxDBlite*/
    public static final String TDB_LITE_HOST = "tax_db_lite.host";
    public static final String TDB_LITE_NAME = "tax_db_lite.database_name";
    public static final String TDB_LITE_USER = "tax_db_lite.user";
    public static final String TDB_LITE_PASSWORD = "tax_db_lite.password";
    public static final String TDB_LITE_GI_TABLE = "tax_db_lite.gi_table";
    public static final String TDB_LITE_EMPTY_GI_TABLE = "tax_db_lite.empty_gi_table";
    public static final String TDB_LITE_EMPTY_NODES_TABLE = "tax_db_lite.empty_nodes_table";
    
    
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
     * the object used to query the taxonomy database TaxDB
     */
    private TaxonomyDao ncbiTaxDB;
    /**
     * the connection to TaxDBlite
     */
    private Connection taxDBliteConn;
    /**
     * prepared statement for gi_number insert
     */
    private PreparedStatement psGiInsert;
    /**
     * prepared statement for gi_number insert
     */
    private PreparedStatement psNodeInsert;
	/**
	 * counts the number of processed taxIDs (=organisms)
	 */
	private int processedTaxIds = 0;
	/**
	 * counts the number of processed tax nodes (=all lineage nodes)
	 */
	private int processedTaxNodes = 0;
	/**
	 * counts the number of processed gi numbers(=protein sequences)
	 */
	private int processedGiNumbers = 0;
	/**
	 * empty lineages
	 */
	private int emptyLineages = 0;
	/**
	 * map that stores the gi numbers from the sequences file and 
	 * later adds the tax_ids from the gi_numbers file
	 */
	private HashMap<Integer, Integer> giNumbersTaxIds;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if( args.length != 1){
			System.out.println("no configuration file provided!");
			System.out.println("usage: java -jar TaxDbLiteFiller_<version>.jar <configuration file>");
			System.out.println("aborting");
			System.exit( 1 );
		}
		
		TaxDbLiteMySqlFiller tdblcreator = new TaxDbLiteMySqlFiller( args[0] );
		tdblcreator.go();
	}
	
	/**
	 * construct with the name of the config file
	 * @param configFile
	 */
	public TaxDbLiteMySqlFiller( String configFile ){
		this.configFile = configFile;
		giNumbersTaxIds = new HashMap<Integer, Integer>();
	}

	
	private void go(){
		try{
			loadConfiguration();
			
			loadLogger();

			logger.info("reading gi numbers from sequences file " + configuration.getString( SEQUENCE_FILE ));
			int sequences = readGiNumbersFromSequenceFile();
			logger.info("end of sequences file; " + sequences + " sequences processed");
			
			logger.info("reading gi_numbers file to fetch tax_ids from file " + configuration.getString( GI_TAXID_FILE ));
			int giNumbers = readTaxIdsFromGiNumbersFile();
			logger.info("end of giNumbers file; " + giNumbers + " giNumbers processed");

			logger.info("checking for unmatched gi_numbers");
			List<Integer> unmatchedGiNumbers = checkUnmatchedGiNumbers();
			if(unmatchedGiNumbers.size() > 0){
				logger.error("there are " + unmatchedGiNumbers.size() + " unmatched GiNumbers " +
						"(gi number in sequence file without presence in gi_taxid file) ");
				if( unmatchedGiNumbers.size() < 25 ){
					logger.debug("UNMATCHED GI_NUMBERS: " + unmatchedGiNumbers.toString());
				}
				else logger.debug(unmatchedGiNumbers.size() + " is too many gi_numbers to print here");
			}
			else{
				logger.info("there are ; " + unmatchedGiNumbers.size() + " unmatchedGiNumbers ");
			}
			
			
			logger.info("connecting to TaxDBlite");
			connectTaxDBlite();
			
			logger.info("connecting to TaxDB");
			connectNCBITaxDB();

			logger.info("starting filling taxDBlite");
			int taxIdsProcessed = enterDataInTaxDBlite();
			logger.info("end filling taxDBlite; " + taxIdsProcessed + " taxIds processed");
			
			logger.info("data processing finished.\n" 
					+ processedGiNumbers + " gi numbers, " 
					+ processedTaxIds + " tax id's and " 
					+ processedTaxNodes + " taxonomy nodes were entered into taxDBlite" 
					+ emptyLineages + " empty lineages were returned ");
			
			
		}catch (Exception e) {
			e.printStackTrace();
			logger.fatal( "an error occurred. aborting.\nplease check the log of this run\navailable info:\n" + e.getMessage());
		}
		finally{
			try {
				ncbiTaxDB.disconnect();
                if (psGiInsert != null) {
                	psGiInsert.close();
                	psGiInsert = null;
                }
                if (psNodeInsert != null) {
                	psNodeInsert.close();
                	psNodeInsert = null;
                }
			} catch (Exception e) {
				logger.fatal( e.getMessage() );
			}
		}
	}

	/**
	 * inserts a gi_number and tax_id combination into TaxDBlite.gi_numbers
	 * @param giNumber
	 * @param taxId
	 * @throws Exception
	 */
	private void processGiNumber( int giNumber, int taxId ) throws Exception{
		logger.debug( "processing gi=" + giNumber );
        psGiInsert.setInt(1, giNumber);
        psGiInsert.setInt(2, taxId);
        psGiInsert.executeUpdate();
        
        processedGiNumbers++;
	}
	
	/**
	 * process a new tax_id. Insert the node belonging to the particular taxID and all nodes
	 * that are its ancestor as long as they do not already exist in the table nodes.
	 * @param taxId
	 * @throws Exception
	 */
	private void processNewTaxId( int taxId ) throws Exception{
		logger.debug( "processing tax_id=" + taxId );
		TaxNode node = getTaxDBliteNode( taxId );
		
		if( node == null ){
			logger.debug( "new tax_id is being processed" );
			
			/*get the NCBI lineage belonging to this taxID*/
			try{
				Lineage lineage = ncbiTaxDB.getLineage( taxId );
				processedTaxIds++;
				if( lineage.getLength() != 0 ){
					logger.debug( lineage.toString() );
					/*iterate over lineage nodes and check their existence in taxDBlite*/
					for( TaxNode lNode :  lineage.getListFromLeaf() ){
						if( ! insertTaxDBliteNode(lNode) ){
							/*the node already existed: all other nodes in this lineage are assumed to exist*/
							break;
						}
						processedTaxNodes++;
					}
				}
			}catch (Exception e) {
				emptyLineages++;
			}
		}
	}
	
	/**
	 * Inserts a new TaxNode into taxDBlite, but only if it does not already exist.
	 * If the node does not already exist, it is inserted and the method will return true; 
	 * Otherwise, this method will return false.
	 * This method will get the data from the NCBI TaxDB.
	 * @param node
	 * @return a new node was inserted
	 * @throws SQLException 
	 */
	private boolean insertTaxDBliteNode( TaxNode node ) throws Exception{
		logger.debug( "inserting new TaxNode" );
		
		/*check existence*/
		TaxNode tblNode = getTaxDBliteNode( node.getTaxID() );
		if( tblNode != null ){
			/*it exists in taxDBlite*/
			if( tblNode.getTaxID() == node.getTaxID() && tblNode.getParentTaxID() == node.getParentTaxID() ){
				/*it exists and is identical*/
				return false;
			}
			else throw new DatabaseException( "data corruption error when checking NCBI taxDB node against taxDBlite node" );
		}
		else{
			/*node does not yet exist; create it now*/
			psNodeInsert.setInt( 1, node.getTaxID() );
			psNodeInsert.setInt( 2, node.getParentTaxID() );
			psNodeInsert.setString( 3, node.getRank().toString() );
			psNodeInsert.setString( 4, node.getScientificName() );
			psNodeInsert.executeUpdate();
			
			ResultSet rs = null;
	        Statement s = taxDBliteConn.createStatement();
	        rs = s.executeQuery( "SELECT * FROM nodes WHERE tax_id=" + node.getTaxID() );
	        
	        if( ! rs.next() ){
	        	throw new DatabaseException( "TaxNode insert was not succesfull" );
	        }
	        else{
	        	logger.debug("tax_id=" + rs.getInt("tax_id") + " is inserted with name " + rs.getString("scientific_name") );
	        }
	    	rs.close();
	    	rs = null;
	        s.close();
	        s = null;
	        
			return true;
		}
	}
	
	/**
	 * check whether the given tax_id exists in the database
	 * @return tax_id exists
	 * @throws SQLException 
	 */
	private TaxNode getTaxDBliteNode( int taxId ) throws SQLException{
		TaxNode node = null;
		ResultSet rs = null;
        Statement s = taxDBliteConn.createStatement();
        rs = s.executeQuery( "SELECT * FROM nodes WHERE tax_id=" + taxId );
        if( rs.next() ){
        	node = new TaxNode();
        	
			if( rs.getInt("tax_id") == 0 ) throw new SQLException("empty node");
			
			node.setTaxID(rs.getInt("tax_id"));
			node.setParentTaxID(rs.getInt("parent_tax_id"));
			node.setRank( TaxonomyRank.getTaxonomyRank( rs.getString("rank") ) );
			node.setScientificName( rs.getString("scientific_name") );

/*			System.out.println( "ptax_id=" + rs.getInt("parent_tax_id") 
					+ " tax_id=" + rs.getInt("tax_id") + " rank=" + rs.getString("rank") 
					+ " TaxonomyRank=" + TaxonomyRank.getTaxonomyRank( rs.getString("rank") ) );
*/        }
        else{
        	logger.debug( "tax_id=" + taxId + " does not exist " );
        }
    	rs.close();
    	rs = null;
        s.close();
        s = null;
		
        return node;
	}

	/**
	 * goes through 
	 * @throws Exception
	 */
	private int enterDataInTaxDBlite( ) throws Exception{
		int taxIdsProcessed = 0;
		for( Entry<Integer, Integer> entry : giNumbersTaxIds.entrySet()){
			if( entry.getValue() != 0){//the gi number has a tax_id
				/*enter into gi numbers table*/
				processGiNumber( entry.getKey(), entry.getValue() );
			}
		}
		/*process all unique taxIDs*/
		HashSet<Integer> taxIds = new HashSet<Integer>();
		taxIds.addAll( giNumbersTaxIds.values() );
		
		/*enter al tax_ids*/
		for( int taxId : taxIds ){
			if(taxId != 0){
				taxIdsProcessed++;
				processNewTaxId( taxId );
			}
		}
		return taxIdsProcessed;
	}
	
	
	/**
	 * reads the input file with gi_numbers and tax_id s and processes it
	 * @throws Exception
	 */
//	private void _readTaxData() throws Exception{
//		int lineNumber = 1;
//		BufferedReader br;
//		try {
//			this.inputFile = new File( configuration.getString( GI_TAXID_FILE ) );
//			/*read from file*/
//			if( (! inputFile.exists() ) || (! inputFile.canRead() ) ){
//				throw new Exception("can not read from input file " + inputFile.getName() );
//			}
//
//			br =  new BufferedReader(new FileReader(inputFile));
//			//read header line: gi_number	tax_id
//			String line;
//			line = br.readLine();
//			String[] elements = line.split("\t");
//			if( ! ( elements[0].equals("gi_number")  && ( elements[1].equals("tax_id") ) ) ){
//				throw new Exception("file does not have the right format: [gi_number]\t[tax_id]\n");
//			}
//			
//			int previousTaxId = -1;
//			
//			while( (line = br.readLine()) != null ){
//				lineNumber++;
//				elements = line.split("\t");
//				int gi_number = Integer.parseInt( elements[0] );
//				int tax_id = Integer.parseInt( elements[1] );
//				
//				if( tax_id != previousTaxId ){//new organism
//					previousTaxId = tax_id;
//					processNewTaxId( tax_id );
//				}
//				processGiNumber( gi_number, tax_id );
//			}
//		}catch (Exception e) {
//			e.printStackTrace();
//			throw new Exception( "file processing failed at line " + lineNumber + ": " + e.getMessage() );
//		}
//	}
	
	/**
	 * establish a connection to the Taxonomy database (local NCBI mirror)
	 */
	private void connectNCBITaxDB() throws Exception{
		/*connect to TaxDB*/
		try {
			this.ncbiTaxDB = TaxonomyDaoMysql.getInstance( 
					configuration.getString( DATABASE_NAME ),
					configuration.getString( DATABASE_HOST ),
					configuration.getString( DATABASE_USER ),
					configuration.getString( DATABASE_PASSWORD ),
					"gi_numbers");
            logger.info("connected to NCBI TaxDB database");
		} catch (DatabaseException e) {
			//e.printStackTrace();
			logger.fatal( "unable to connect to the database " );
			throw new Exception( "unable to connect to the database" );
		}
	}

	/**
	 * establish a connection to the Taxonomy lite database
	 */
	private void connectTaxDBlite() throws Exception{
		/*connect to TaxDB*/
		try{
	        	String url = "jdbc:mysql://" + configuration.getString( TDB_LITE_HOST ) + "/" + configuration.getString( TDB_LITE_NAME );
	        	Class.forName ("com.mysql.jdbc.Driver").newInstance ();
	        	this.taxDBliteConn = DriverManager.getConnection
	        			(url,
	        			configuration.getString( TDB_LITE_USER ),
	        			configuration.getString( TDB_LITE_PASSWORD ));

	            logger.info("connected to TaxDBlite database");

	            if( configuration.getBoolean( TDB_LITE_EMPTY_GI_TABLE ) ){
	            	PreparedStatement delete = taxDBliteConn.prepareStatement( "truncate " + configuration.getString(TDB_LITE_GI_TABLE) + ";" );
	            	delete.execute();
	            	delete.close();
		            logger.info("TaxDBlite database deleted " + configuration.getString(TDB_LITE_GI_TABLE) );
	            }
	            
	            if( configuration.getBoolean( TDB_LITE_EMPTY_NODES_TABLE ) ){
	            	PreparedStatement delete = taxDBliteConn.prepareStatement( "truncate nodes;" );
	            	delete.execute();
	            	delete.close();
	            	logger.info("TaxDBlite database nodes table deleted " );
	            }

	            psGiInsert = taxDBliteConn.prepareStatement( "insert into " 
	            		+ configuration.getString(TDB_LITE_GI_TABLE) + " values (?, ?)" );
	            psNodeInsert = taxDBliteConn.prepareStatement( "insert into nodes values (?, ?, ?, ?)" );
	            
	        }catch (Exception e){
	        	//e.printStackTrace();
				logger.fatal( "unable to connect to database " + configuration.getString( TDB_LITE_NAME ) );
	        	throw new DatabaseException("unable to connect to TaxDB at this time");
	    }
	}

	/**
	 * checks how many gi numbers are without tax_id
	 * @return
	 */
	private List<Integer> checkUnmatchedGiNumbers(){
		List<Integer> unmatched = new ArrayList<Integer>();
		for( Entry<Integer, Integer> entry : giNumbersTaxIds.entrySet()){
			if( entry.getValue() == 0){
				unmatched.add(entry.getKey());
				
			}
		}
		return unmatched;
	}
	
	/**
	 * processes the dump file that holds the link between gi numbers and tax_ids
	 * @return giNumbersProcessed
	 * @throws IOException
	 * @throws ParseException
	 */
	private int readTaxIdsFromGiNumbersFile() throws IOException, ParseException{
		int lineNumber = 0;
		//int matched = 0;
		BufferedReader br = null;
		try {
			File inputFile = new File( configuration.getString( GI_TAXID_FILE ) );
			/*read from file*/
			if( (! inputFile.exists() ) || (! inputFile.canRead() ) ){
				throw new IOException("can not read from input file " + inputFile.getName() );
			}

			br =  new BufferedReader(new FileReader(inputFile));
			/*gi_number	tax_id*/
			String line;
			String[] elements;
			
			/*skip first line*/
			line = br.readLine();
			
			while( (line = br.readLine()) != null ){
				lineNumber++;
				
				elements = line.split("\t");
				try{
					int gi = Integer.parseInt(elements[0]);
					int taxId = Integer.parseInt(elements[1]);
					
					if( giNumbersTaxIds.containsKey(gi) ){
						//matched++;
						giNumbersTaxIds.put(gi, taxId);
						//System.out.println(this.getClass().getSimpleName() + " taxId entered: " + taxId + " for gi " + gi);
					}
				}catch (Exception e) {
					throw new ParseException("error parsing file " + inputFile.getName() 
							+ ": line does not have correct format: " + line + " at line " + lineNumber);
				}
			}
		}catch (IOException e) {
			throw new IOException( "file processing failed at line " + lineNumber + ": " + e.getMessage() );
		}
		finally{
			if(br != null )	br.close();
		}
		return lineNumber;
	}
	
	/**
	 * processes the sequences file and extracts gi numbers
	 * @return sequencesProcessed
	 * @throws IOException
	 * @throws ParseException
	 */
	private int readGiNumbersFromSequenceFile() throws IOException, ParseException{
		int lineNumber = 0;
		int sequenceNumber = 0;
		BufferedReader br = null;
		try {
			File inputFile = new File( configuration.getString( SEQUENCE_FILE ) );
			/*read from file*/
			if( (! inputFile.exists() ) || (! inputFile.canRead() ) ){
				throw new IOException("can not read from input file " + inputFile.getName() );
			}

			br = new BufferedReader(new FileReader(inputFile));
			/*>gi|158333249|ref|YP_001514421.1| response regulator [Acaryochloris marina MBIC11017]*/
			String line;
			String[] elements;
			
			while( (line = br.readLine()) != null ){
				lineNumber++;
				
				if( line.startsWith(">" ) ){
					sequenceNumber++;
					
					elements = line.split("\\|");
					
					if(! elements[0].equals(">gi")){
						throw new ParseException("error parsing file " + inputFile.getName() 
								+ ": description line does not have correct format: " + line + " at line " + lineNumber);
					}
					
					int gi_number = Integer.parseInt( elements[1] );
					giNumbersTaxIds.put(gi_number, 0);
				}
			}
		}catch (IOException e) {
			throw new IOException( "file processing failed at line " + lineNumber + ": " + e.getMessage() );
		}
		finally{
			if(br != null )	br.close();
		}
		return sequenceNumber;
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
				throw new IOException("[TaxDbLiteCreator.loadLogger()]: unable to configure logger" + "; cause of exception:" + e.getCause());
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
        try {
            File xmlConfigFile = new File( configFile );
            if ( ! xmlConfigFile.exists() || ! xmlConfigFile.canRead() ) {
            	System.out.println( "unable to read configuration file: " + configFile );
                throw new Exception( "[TaxDbLiteCreator.loadConfiguration()] unable to read configuration file: " + configFile);
            }
            configuration = new XMLConfiguration( xmlConfigFile );
        } catch (Exception e) { // configuration could not be loaded
			throw new Exception("[TaxDbLiteCreator.loadConfiguration()] unable to load configuration file: " + configFile + "; cause of exception:" + e.getCause() );
        }
    }

}
