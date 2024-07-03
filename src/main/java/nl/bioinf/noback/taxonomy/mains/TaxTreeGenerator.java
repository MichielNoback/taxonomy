/**
 * 
 */
package nl.bioinf.noback.taxonomy.mains;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;

import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoInMemory;
import nl.bioinf.noback.taxonomy.mains.BlastrunnerHitlineTaxIdExtractor;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxIdExtractor;
import nl.bioinf.noback.taxonomy.model.TaxIdExtractor.Pair;
import nl.bioinf.noback.taxonomy.model.TaxTree;

import org.apache.commons.configuration.XMLConfiguration;


/**
 * This class is the main that forms an app that can be used to create a taxonomic tree from a simple text file
 * @author michiel
 *
 */
public class TaxTreeGenerator {
    /**
     * XPaths to the configuration file settings defined
     */
	public static final String VERBOSITY = "verbose";
    public static final String INPUT_TYPE = "input_type";
    public static final String INPUT_FILE = "input_file";
    public static final String TREE_FILE = "tree_file";
    public static final String TAXNODES_FILE = "taxnodes_file";

    public boolean VERBOSE = true; 
	private String configFile;
	private XMLConfiguration configuration;
	private File inputFile;
	private File treeFile;
	private String inputType;
	private TaxIdExtractor taxIdExtractor;
	private TaxonomyDaoInMemory taxDB;
	private TaxTree tree;
	private File nodesFile;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if( args.length != 1){
			//out.println("");
			System.out.println("no configuration file provided!");
			System.out.println("usage: java -jar tax_tree_generator_<version>.jar <configuration file>");
			System.out.println("aborting");
			System.exit( 1 );
		}
		TaxTreeGenerator ttg = new TaxTreeGenerator( args[0] );
		ttg.start();

	}


	/**
	 * construct with the name of the config file
	 * @param configFile
	 */
	public TaxTreeGenerator( String configFile ){
		this.configFile = configFile;
	}

	
	public void start(){
		try {
			if( VERBOSE ){
				System.out.println("[ " + this.getClass().getSimpleName() + " ] processing settings");
			}
			loadConfiguration();
			processSettings();
			loadTaxDB();
			readInputFile();
			writeTreeFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void writeTreeFile() throws Exception{
		tree.preOrderToXmlFile(tree.getRoot(), this.treeFile, "taxtree from file");
	}
	
	private void readInputFile() throws Exception{
		if( VERBOSE ){
			System.out.println("[ " + this.getClass().getSimpleName() + " ] start processing: " + inputType);
		}
		this.tree = new TaxTree();
		int count = 0;
		int failed = 0;

		try{
			BufferedReader br = new BufferedReader(new FileReader(this.inputFile));
			String line;
			while( (line = br.readLine()) != null ){
				count++;
				try{
					//System.out.println( "***" + line);
					Pair taxIdCount = taxIdExtractor.extractTaxIdCount(line);
					//System.out.println("+++" + taxIdCount.taxID + "; " + taxIdCount.count);
					Lineage l = taxDB.getLineage( taxIdCount.taxID );
					for( int i=0; i<taxIdCount.count; i++ ){
						tree.addLineage(l);
					}

				}catch (Exception e) {
					System.err.println("lineage failed: " + e.getMessage());
					failed++;
				}
			}
			br.close();
		}catch (Exception e) {
			Exception ne = new Exception("[TaxTreeGenerator] an error occurred during file processing: " + e.getMessage() );
			ne.setStackTrace(e.getStackTrace());
			throw ne;
		}
		if( VERBOSE ){
			System.out.println( "[ " + this.getClass().getSimpleName() + " ] end file processing: " + count + " lines read; " + failed + " lines failed to return lineage");
		}
	}
	
	private void loadTaxDB() throws Exception{
		this.taxDB = new TaxonomyDaoInMemory( nodesFile, null);
		if( VERBOSE ){
			System.out.println("[ " + this.getClass().getSimpleName() + " ] taxDB loaded: ");
			taxDB.countNodes();
		}
	}
	
	/**
	 * processes the settings and generates a BlastFilter object
	 */
	private void processSettings() throws Exception{
		
		this.VERBOSE = configuration.getBoolean(VERBOSITY);
		if( VERBOSE ){
			System.out.println("[ " + this.getClass().getSimpleName() + " ] verbose: ON");
		}
		
		this.inputType = configuration.getString( INPUT_TYPE );
		if( VERBOSE ){
			System.out.println("[ " + this.getClass().getSimpleName() + " ] input type: " + inputType);
		}
		
		if( inputType.equals("TAX_ID_LIST") ){
			this.taxIdExtractor = new TaxIdExtractor() {
				@Override
				public Pair extractTaxIdCount(String line) throws ParseException {
					try{
						int taxID = Integer.parseInt(line);
						return new Pair(taxID, 1);
					}catch (Exception e) {
						throw new ParseException("failed to extract taxID from hitID (" + line + ") from BlastHit " + line, 0 );
					}
				}
			};
		}
		else if( inputType.equals("NAME_TAXID_COUNT") ){
			this.taxIdExtractor = new TaxIdExtractor() {
				@Override
				public Pair extractTaxIdCount(String line) throws ParseException {
					try{
						String[] elemnts = line.split("\t");
						//System.out.println("+++" + elemnts[1] + "; " + elemnts[2]);
						int taxID = Integer.parseInt( elemnts[1] );
						int count = Integer.parseInt( elemnts[2] );
						return new Pair(taxID, count);
					}catch (Exception e) {
						throw new ParseException("failed to extract taxID from hitID (" + line + ") from BlastHit " + line, 0 );
					}
				}
			};
		}
		else if( inputType.equals("BLASTRUNNER_OUTPUT") ){
			this.taxIdExtractor = new BlastrunnerHitlineTaxIdExtractor();
		}
		else{
			throw new Exception("[TaxTreeGenerator] Unknown analysis type:__" + inputType + "__");
		}

		
		/*input data source processing*/
		this.inputFile = new File( configuration.getString( INPUT_FILE ) );
		/*read from file*/
		if( (! inputFile.exists() ) || (! inputFile.canRead() ) ){
			throw new Exception("[TaxTreeGenerator] can not read from input file " + inputFile.getName() );
		}
		if( VERBOSE ){
			System.out.println("[ " + this.getClass().getSimpleName() + " ] input file: " + inputFile);
		}

		this.nodesFile = new File( configuration.getString( TAXNODES_FILE ) );
		/*read from file*/
		if( (! nodesFile.exists() ) || (! nodesFile.canRead() ) ){
			throw new Exception("[TaxTreeGenerator] can not read from nodes file " + nodesFile.getName() );
		}
		if( VERBOSE ){
			System.out.println("[ " + this.getClass().getSimpleName() + " ] nodes file: " + nodesFile);
		}

		/*set the output file*/
		this.treeFile = new File( configuration.getString( TREE_FILE ) );
		if( VERBOSE ){
			System.out.println("[ " + this.getClass().getSimpleName() + " ] tree output file: " + treeFile);
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
