/**
 * 
 */
package nl.bioinf.noback.taxonomy.mains;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.dao.DatabaseException;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDao;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoMysql;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxNode;

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
public class TaxDbLiteDerbyCreator {
    /**
     * XPaths to the configuration file settings
     */
    public static final String LOGGER_NAME = "log.name";
    public static final String LOG_LEVEL = "log.level";
    public static final String LOG_FILE = "log.file";

    public static final String INPUT_FILE = "input_file";
    /*NCBI TaxDB*/
    public static final String DATABASE_NAME = "database.database_name";
    public static final String GI_TABLE = "gi_table";
    public static final String DATABASE_USER = "database.user";
    public static final String DATABASE_HOST = "database.host";
    public static final String DATABASE_PASSWORD = "database.password";
    /*TaxDBlite*/
    public static final String TDB_LITE_LOCATION = "tax_db_lite.location";
    public static final String TDB_LITE_NAME = "tax_db_lite.name";
    public static final String TDB_LITE_USER = "tax_db_lite.user";
    public static final String TDB_LITE_PASSWORD = "tax_db_lite.password";
    
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
     * the input data file coupling gi numbers and tax_id s
     */
	private File inputFile;

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
	 * @param args
	 */
	public static void main(String[] args) {
		if( args.length != 1){
			System.out.println("no configuration file provided!");
			System.out.println("usage: java -jar TaxDbLiteFiller_<version>.jar <configuration file>");
			System.out.println("aborting");
			System.exit( 1 );
		}
		
		TaxDbLiteDerbyCreator tdblcreator = new TaxDbLiteDerbyCreator( args[0] );
		tdblcreator.go();
	}
	
	/**
	 * construct with the name of the config file
	 * @param configFile
	 */
	public TaxDbLiteDerbyCreator( String configFile ){
		this.configFile = configFile;
	}

	
	private void go(){
		try{
			loadConfiguration();
			
			loadLogger();

			logger.info("deleting existing Derby TaxDBlite database");
			deleteTaxDBlite();
			
			logger.info("creating Derby TaxDBlite database");
			createTaxDbLite();

			logger.info("connecting to TaxDB");
			connectTaxDB();

			logger.info("starting processing of taxonomy data");
			readTaxData();
			
			logger.info("data processing finished.\n" 
					+ processedGiNumbers + " gi numbers, " 
					+ processedTaxIds + " tax id's and " 
					+ processedTaxNodes + " taxonomy nodes were entered into taxDBlite" );
			
			
		}catch (Exception e) {
			logger.fatal( "an error occurred. aborting.\nplease check the log of this run\navailable info:\n" + e.getMessage());
		}
		finally{
			try {
				ncbiTaxDB.disconnect();
                if (taxDBliteConn != null) {
                	taxDBliteConn.close();
                	taxDBliteConn = null;
                }
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
/*        ResultSet rs = null;
        Statement s = taxDBliteConn.createStatement();
        rs = s.executeQuery( "SELECT gi_number, tax_id FROM gi_numbers WHERE gi_number=" + giNumber );
        if (!rs.next()){
        	System.out.println("No rows in ResultSet");
        }
    	System.out.println("inserted: gi=" + rs.getInt("gi_number") + " tax_id=" + rs.getInt("tax_id"));
        
    	psGiInsert.clearParameters();
    	rs.close();
    	rs = null;
        s.close();
        s = null;
*/        
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
			Lineage lineage = ncbiTaxDB.getLineage( taxId );
			processedTaxIds++;
			if( lineage.getLength() != 0 ){
				logger.debug( lineage.toString() );
				/*iterate over lineage nodes and chack their existance in taxDBlite*/
				for( TaxNode lNode :  lineage.getListFromLeaf() ){
					if( ! insertTaxDBliteNode(lNode) ) break;
					processedTaxNodes++;
				}
			}
		}
	}
	
	/**
	 * Inserts a new TaxNode into taxDBlite, but only if it does not already exist.
	 * If the node does not already exist, it is inserted and the method will return true; 
	 * Otherwise, this method will return false.
	 * This method will get the data from the NCBI TaxDB.
	 * @param taxId
	 * @throws SQLException 
	 */
	private boolean insertTaxDBliteNode( TaxNode node ) throws Exception{
		logger.debug( "inserting new TaxNode" );
		
		/*check existence*/
		TaxNode tblNode = getTaxDBliteNode( node.getTaxID() );
		if( tblNode != null ){
			if( tblNode.getTaxID() == node.getTaxID() && tblNode.getParentTaxID() == node.getParentTaxID() ) return false;
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
*/      }
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
	 * reads the input file and processes it
	 * @throws Exception
	 */
	private void readTaxData() throws Exception{
		int lineNumber = 1;
		BufferedReader br;
		try {
			this.inputFile = new File( configuration.getString( INPUT_FILE ) );
			/*read from file*/
			if( (! inputFile.exists() ) || (! inputFile.canRead() ) ){
				throw new Exception("can not read from input file " + inputFile.getName() );
			}

			br =  new BufferedReader(new FileReader(inputFile));
			//read header line: gi_number	tax_id
			String line;
			line = br.readLine();
			String[] elements = line.split("\t");
			if( ! ( elements[0].equals("gi_number")  && ( elements[1].equals("tax_id") ) ) ){
				br.close();
				throw new Exception("file does not have the right format: [gi_number]\t[tax_id]\n");
			}
			
			int previousTaxId = -1;
			
			while( (line = br.readLine()) != null ){
				lineNumber++;
				elements = line.split("\t");
				int gi_number = Integer.parseInt( elements[0] );
				int tax_id = Integer.parseInt( elements[1] );
				
				if( tax_id != previousTaxId ){//new organism
					previousTaxId = tax_id;
					processNewTaxId( tax_id );
				}
				processGiNumber( gi_number, tax_id );
			}
			br.close();
		}catch (Exception e) {
			e.printStackTrace();
			throw new Exception( "file processing failed at line " + lineNumber + ": " + e.getMessage() );
		}
	}
	
	/**
	 * establish a connection to the Taxonomy database (local NCBI mirror)
	 */
	private void connectTaxDB() throws Exception{
		/*connect to TaxDB*/
		try {
			this.ncbiTaxDB = TaxonomyDaoMysql.getInstance( 
					configuration.getString( DATABASE_NAME ),
					configuration.getString( DATABASE_HOST ),
					configuration.getString( DATABASE_USER ),
					configuration.getString( DATABASE_PASSWORD ),
					"gi_numbers");
		} catch (DatabaseException e) {
			//e.printStackTrace();
			logger.fatal( "unable to connect to the database" );
			throw new Exception( "unable to connect to the database" );
		}
	}
	
	/**
	 * creates the Derby taxDBlite database
	 * @throws Exception
	 */
	private void createTaxDbLite() throws Exception{
		Properties props = new Properties(); // connection properties
        // providing a user name and password is optional in the embedded
        // and derbyclient frameworks
        props.put("user", configuration.getString( TDB_LITE_USER ) );
        props.put("password", configuration.getString( TDB_LITE_PASSWORD ) );
        
		String protocol = "jdbc:derby:";
		String location = configuration.getString( TDB_LITE_LOCATION );
		
		if( ! location.endsWith(File.separator) ){
			location = location + File.separator;
		}
		
		String dbName = configuration.getString( TDB_LITE_NAME );
		
        Statement s = null;
        //ResultSet rs = null;
		try{
			String driver = "org.apache.derby.jdbc.EmbeddedDriver";
			Class.forName(driver).newInstance();
			taxDBliteConn = DriverManager.getConnection(protocol + location + dbName + ";create=true", props);
			
            s = taxDBliteConn.createStatement();
            /*create database structure: only gi_numbers and tax_ids*/
            /*create gi_numbers table*/
            s.execute("create table gi_numbers( " +
            		" gi_number int NOT NULL, " +
            		" tax_id int NOT NULL, " +
            		" PRIMARY KEY (gi_number))");
            /*create table nodes*/
            s.execute("CREATE TABLE nodes " +
            		"(tax_id int NOT NULL," +
            		" parent_tax_id int NOT NULL," +
            		" rank varchar(100) NOT NULL default 'no rank'," +
            		" scientific_name varchar(200) default NULL," +
            		" PRIMARY KEY (tax_id)" +
            		//" FOREIGN KEY (parent_tax_id) REFERENCES nodes(tax_id)" +
            		" )");
           
            //System.exit(0);
            psGiInsert = taxDBliteConn.prepareStatement( "insert into gi_numbers values (?, ?)" );
            psNodeInsert = taxDBliteConn.prepareStatement( "insert into nodes values (?, ?, ?, ?)" );
		}catch (Exception e) {
			e.printStackTrace();
			throw new Exception( "an error occorred creating the Derby TaxDBlite database: " + e.getMessage() );
		} finally {
            /*release all open resources to avoid unnecessary memory usage*/
            try {
            	if (s != null) {
                    s.close();
                    s = null;
                }
            } catch (SQLException sqle) {
                printSQLException(sqle);
            }
        }
	}

    /**
     * Prints details of an SQLException chain to <code>System.err</code>.
     * Details included are SQL State, Error code, Exception message.
     *
     * @param e the SQLException from which to print details.
     */
    public static void printSQLException(SQLException e)
    {
        // Unwraps the entire exception chain to unveil the real cause of the
        // Exception.
        while (e != null)
        {
            logger.error("\n----- SQLException -----");
            logger.error("  SQL State:  " + e.getSQLState());
            logger.error("  Error Code: " + e.getErrorCode());
            logger.error("  Message:    " + e.getMessage());
            // for stack traces, refer to derby.log or uncomment this:
            //e.printStackTrace(System.err);
            e = e.getNextException();
        }
    }

    /**
     * deletes the old taxdb
     * @throws Exception
     */
    private void deleteTaxDBlite() throws Exception{
    	String location = configuration.getString( TDB_LITE_LOCATION );
		if( ! location.endsWith(File.separator) ){
			location = location + File.separator;
		}
    	
		String s = null;
        Process ps;
		try {
			ps = Runtime.getRuntime().exec( "rm -f -R " + location );
	        BufferedReader psStdInput = new BufferedReader( new InputStreamReader(ps.getInputStream()) );
	        BufferedReader psStdError = new BufferedReader( new InputStreamReader(ps.getErrorStream()) );

	        /*read the output from the command*/
	        logger.debug("Standard out from removing old TaxDBlite (rm): ");
	        while ((s = psStdInput.readLine()) != null) {
	        	logger.debug("STDOUT from rm: " + s);
	        }
	        /*read any errors from the attempted command*/
	        if( (s = psStdError.readLine()) != null){
	        	logger.error("STDERR from rm: " + s);
		        while ((s = psStdError.readLine()) != null) {
		            logger.error("STDERR from rm: " + s);
		        }
	        }
		} catch (IOException e) {
			//e.printStackTrace();
			throw new Exception("removing old TaxDBlite failed: " + e.getMessage());
		}
		ps.getInputStream().close();
		ps.getErrorStream().close();
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
    	//System.out.println("loading configuration file");
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
