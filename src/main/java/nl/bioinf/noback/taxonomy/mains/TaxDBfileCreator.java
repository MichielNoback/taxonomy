/**
 * 
 */
package nl.bioinf.noback.taxonomy.mains;

import nl.bioinf.noback.taxonomy.io.ParseException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.model.CorruptedLineageException;
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
public class TaxDBfileCreator {
    /**
     * XPaths to the configuration file settings
     */
    public static final String LOGGER_NAME = "log.name";
    public static final String LOG_LEVEL = "log.level";
    public static final String LOG_FILE = "log.file";
    
    public static final String SEQUENCES_FILE = "sequences_file";
    public static final String GI_NUMBERS_FILE = "gi_numbers_file";
    public static final String NODES_FILE = "nodes_file";
    public static final String NAMES_FILE = "names_file";
    public static final String TAXDB_NODES_FILE = "taxdb_nodes_file";
    public static final String TAXDB_GI_FILE = "taxdb_gi_file";
    

	private String configFile;
	private XMLConfiguration configuration;
	private Logger logger;
	/**
	 * map that stores the gi numbers from the sequences file and 
	 * later adds the tax_ids from the gi_numbers file
	 */
	private HashMap<Integer, Integer> giNumbersTaxIds;
    /**
     * map that stores the relevant taxnodes with key of tax_id
     */
	private HashMap<Integer, TaxNode> taxNodes;
    
	public TaxDBfileCreator( String configFile ) {
		this.configFile = configFile;
		giNumbersTaxIds = new HashMap<Integer, Integer>();
		taxNodes = new HashMap<Integer, TaxNode>();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if( args.length != 1){
			System.out.println("no configuration file provided!");
			System.out.println("usage: java -jar TaxDBfileCreator_<version>.jar <configuration file>");
			System.out.println("aborting");
			System.exit( 1 );
		}
		
		TaxDBfileCreator tdbfc = new TaxDBfileCreator( args[0] );
		tdbfc.go();
	}
	
	private void go(){
		
		try{
			loadConfiguration();
			
			loadLogger();

			logger.info("reading gi numbers from sequences file " + configuration.getString( SEQUENCES_FILE ));
			int sequences = readGiNumbersFromSequenceFile();
			logger.info("end of sequences file; " + sequences + " sequences processed");
			
			logger.info("reading gi_numbers file to fetch tax_ids " + configuration.getString( GI_NUMBERS_FILE ));
			int giNumbers = readTaxIdsFromGiNumbersFile();
			logger.info("end of giNumbers file; " + giNumbers + " giNumbers processed");

			logger.info("checking for unmatched gi_numbers");
			List<Integer> unmatchedGiNumbers = checkUnmatchedGiNumbers();
			if(unmatchedGiNumbers.size() > 0){
				logger.error("there are ; " + unmatchedGiNumbers.size() + " unmatched GiNumbers " +
						"(gi number in sequence file without presence in gi_taxid file) ");
				if( unmatchedGiNumbers.size() < 25 ){
					logger.debug("UNMATCHED GI_NUMBERS: " + unmatchedGiNumbers.toString());
				}
				else logger.debug(unmatchedGiNumbers.size() + ": too many gi_numbers to print here");
			}
			else{
				logger.info("there are " + unmatchedGiNumbers.size() + " unmatched Gi numbers ");
			}

			logger.info("starting processing of taxonomy nodes data");
			int totalNodes = readNodesFile();
			logger.info("end of nodes file; " + totalNodes + " nodes processed");
			
			logger.info("starting filtering of taxonomy nodes");
			int matchedNodes = filterNodes();
			logger.info("end of filtering nodes nodes; " + matchedNodes + " nodes matched");
			
			logger.info("starting fetching node names");
			int foundNames = fetchNodeNames();
			logger.info("end fetching node names; " + foundNames + " node names found");
						
			logger.info("starting writing TaxDB Nodes file " + configuration.getString( TAXDB_NODES_FILE ));
			int writtenNodes = writeTaxDBnodesFile();
			logger.info("end writing TaxDBfile; " + writtenNodes + " node written to file");

			logger.info("starting writing TaxDB GI file " + configuration.getString( TAXDB_GI_FILE ));
			int writtenGInumbers = writeTaxDBgiFile();
			logger.info("end writing TaxDB GI file; " + writtenGInumbers + " gi numbers written to file");

			logger.info("data processing finished.\n" );
//					+ processedGiNumbers + " gi numbers, " 
//					+ processedTaxIds + " tax id's and " 
//					+ processedTaxNodes + " taxonomy nodes were entered into taxDBlite" );
			
		}catch (IOException e) {
			logger.fatal( e.getClass().getSimpleName() + ": " + e.getMessage());
		}catch (ParseException e) {
			logger.fatal( e.getClass().getSimpleName() + ": " +e.getMessage());
		}
		catch(CorruptedLineageException e){
			logger.fatal( e.getClass().getSimpleName() + ": " +e.getMessage());
		}
		catch (Exception e) {
			logger.fatal( "an error occurred. aborting.\nplease check the log of this run\navailable info:\n" + e.getMessage());
		}
	}

	/**
	 * writes gi numbers and tax_ids to file 
	 * @return giWritten
	 * @throws IOException
	 */
	private int writeTaxDBgiFile() throws IOException{
		int giWritten = 0;
		int giProcessed = 0;
		
		PrintWriter pw = null;
		try {
			File outputFile = new File( configuration.getString( TAXDB_GI_FILE ) );
			/*read from file*/
			if( ! ( outputFile.createNewFile() && outputFile.canWrite() ) ){
				throw new IOException("can not write to output file " + outputFile.getName() );
			}

			pw = new PrintWriter(outputFile);
			
			/*iterate over gi numbers*/
			for( Entry<Integer, Integer> entry : giNumbersTaxIds.entrySet() ){
				giProcessed++;
				if( entry.getValue() != 0 ){
					giWritten++;
					pw.println(entry.getKey() + "\t" + entry.getValue() );
				}
			}
		}finally{
			if(pw != null ){
				pw.flush();
				pw.close();
			}
		}
		logger.debug( giProcessed + " nodes processed" );
		
		return giWritten;
	}

	
	/**
	 * writes the taxDB file
	 * @return nodesWritten
	 * @throws Exception 
	 * @throws ParseException
	 */
	private int writeTaxDBnodesFile() throws Exception{
		int nodesWritten = 0;
		int nodesProcessed = 0;
		
		PrintWriter pw = null;
		try {
			File outputFile = new File( configuration.getString( TAXDB_NODES_FILE ) );
			/*read from file*/
			if( ! ( outputFile.createNewFile() && outputFile.canWrite() ) ){
				throw new IOException("can not write to output file " + outputFile.getName() );
			}

			pw = new PrintWriter(outputFile);
			
			/*write root*/
			TaxNode root = taxNodes.get(1);
			if(root != null){
				nodesWritten++;
				pw.println(root.getTaxID() + "\t" + root.getParentTaxID() + "\t" + root.getRank() + "\t" + "root" );
			}
			else{
				throw new Exception("root is not present for writing!");
			}
			
			/*iterate over taxnodes*/
			for( TaxNode tn : taxNodes.values() ){
				nodesProcessed++;
				if( tn.getParentNode() != null ){
					nodesWritten++;
					pw.println(tn.getTaxID() + "\t" + tn.getParentTaxID() + "\t" + tn.getRank() + "\t" + tn.getScientificName() );
				}
			}
		}finally{
			if(pw != null ){
				pw.flush();
				pw.close();
			}
		}
		logger.debug( nodesProcessed + " nodes processed" );
		
		return nodesWritten;
	}
	
	/**
	 * fetches the node names for nodes with parent node
	 * @return matchedNames
	 * @throws IOException
	 * @throws ParseException
	 */
	private int fetchNodeNames() throws IOException, ParseException{
		//int matchedNames = 0;
		int matchedNodes = 0;
		int lineNumber = 0;
		
		BufferedReader br = null;
		try {
			File inputFile = new File( configuration.getString( NAMES_FILE ) );
			/*read from file*/
			if( (! inputFile.exists() ) || (! inputFile.canRead() ) ){
				throw new IOException("can not read from input file " + inputFile.getName() );
			}

			br =  new BufferedReader(new FileReader(inputFile));
			String line;
			String[] elements;
			
			while( (line = br.readLine()) != null ){
				lineNumber++;
				
				/*tax_d  | name_txt	| unique_name  | name class |*/
				/*2	|	Bacteria	|	Bacteria <prokaryote>	|	scientific name	|*/
				elements = line.split("\\|");
				try{
					int taxId = Integer.parseInt( elements[0].trim() );
					String name = elements[1].trim();
					String nameClass = elements[3].trim();
					
					//System.out.println("tax_id=" + taxId + " name=" + name + " nameClass=" +nameClass);
					
					if( nameClass.equals("scientific name") && taxNodes.containsKey(taxId) ){
						//matchedNames++;
						
						//System.out.println("tax_id=" + taxId + " name=" + name + " nameClass=" +nameClass);
						
						TaxNode tn = taxNodes.get(taxId);
						if(tn.getParentNode() != null){
							//System.out.println("tax_id=" + taxId + " name=" + name + " nameClass=" + nameClass + " is relevant" );
							tn.setScientificName(name);
							matchedNodes++;
						}
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
		return matchedNodes;
	}
	
	/**
	 * filters the nodes set and returns how many are matched: to be written to out
	 * @return matchedNodes
	 * @throws IOException
	 * @throws ParseException
	 */
	private int filterNodes() throws CorruptedLineageException{
		int matchedNodes = 0;
		int unmatchedTaxIds = 0;
		
		/*iterate the taxIds from giNumbersTaxIds and process them as follows:
		 * 1) get the TaxNode from the taxnodes set
		 * 2) if it has no parent ref defined, fetch it 
		 * 3) set this ref on the first TaxNode
		 * 4) continue until 
		 * 		a) there is a TaxNode with a parent ref
		 * 		b) the parent is the root (tax_id == 1; parent_tax_id==1)
		 * */
		//int count = 0;
		for( Entry<Integer, Integer> entry : giNumbersTaxIds.entrySet()){
			if( entry.getValue() != 0){//there is a valid tax_id
				//count++;
				
				//System.out.println(this.getClass().getSimpleName() + " gi=" + entry.getKey() + " tax_id=" + entry.getValue());
				
				int taxId = entry.getValue();
				if( taxNodes.containsKey( taxId ) ){
					TaxNode tn = taxNodes.get(taxId);
					TaxNode parent = null;
					
					//System.out.println(this.getClass().getSimpleName() + " TaxNode=" + tn);
					
					while( tn.getParentNode() == null && tn.getTaxID() != 1 ){
						
						if( taxNodes.containsKey( tn.getParentTaxID() )){
							matchedNodes++;
							parent = taxNodes.get( tn.getParentTaxID() );
							
						
							//System.out.println(this.getClass().getSimpleName() + "tax_id=" + tn.getTaxID() + " parent=" + parent.getTaxID() );

							try {
								tn.setParentNode(parent);
								tn = parent;
							} catch (CorruptedLineageException e) {
								throw new CorruptedLineageException("a CorruptedLineageException occurred with node " 
										+ tn + " when attempting to set parent " + parent);
							}
						}else{
							unmatchedTaxIds++;
							throw new CorruptedLineageException( "TaxNode without parent =" + tn );
						}
					}
					//if(count>30)System.exit(0);
				}
				else{
					unmatchedTaxIds++;
					logger.error("unmatched tax_id: gi_number=" + entry.getKey() + " tax_id=" + entry.getValue() );
				}		
			}
		}
		if( unmatchedTaxIds > 0 ){
			logger.error( unmatchedTaxIds + " unmatched tax_ids");
		}
		return matchedNodes;
	}
	
	/**
	 * reads the nodes dump file and stores relevant nodes
	 * @return nodesProcessed
	 * @throws IOException
	 * @throws ParseException
	 */
	private int readNodesFile() throws IOException, ParseException{
		int lineNumber = 0;
		BufferedReader br = null;
		try {
			File inputFile = new File( configuration.getString( NODES_FILE ) );
			/*read from file*/
			if( (! inputFile.exists() ) || (! inputFile.canRead() ) ){
				throw new IOException("can not read from input file " + inputFile.getName() );
			}

			br =  new BufferedReader(new FileReader(inputFile));
			String line;
			String[] elements;
			
			while( (line = br.readLine()) != null ){
				lineNumber++;
				
				/*tax_d  | parent tax_id  | rank | more data*/
				/*1	|	1	|	no rank	|		|	8	|*/
				elements = line.split("\\|");
				try{
					int taxId = Integer.parseInt( elements[0].trim() );
					int pTaxId = Integer.parseInt( elements[1].trim() );
					String rank = elements[2].trim();
					
					//System.out.println("tax_id=" + taxId + " p_tax_id=" + pTaxId + " rank=" +rank);
					
					TaxNode tn = new TaxNode(taxId, pTaxId );
					tn.setRank(TaxonomyRank.getTaxonomyRank(rank ) );
										
					taxNodes.put(taxId, tn);
					
					//System.out.println( taxNodes.get(taxId) );
					
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
			File inputFile = new File( configuration.getString( GI_NUMBERS_FILE ) );
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
			File inputFile = new File( configuration.getString( SEQUENCES_FILE ) );
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
    
    /** Get the current line number.
     * @return int - Current line number.
     */
    @SuppressWarnings("unused")
	private static int getLineNumber() {
        return Thread.currentThread().getStackTrace()[2].getLineNumber();
    }

}
