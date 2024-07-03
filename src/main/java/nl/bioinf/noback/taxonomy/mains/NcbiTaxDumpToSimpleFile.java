/**
 * 
 */
package nl.bioinf.noback.taxonomy.mains;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;

import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.model.TaxNode;
import nl.bioinf.noback.taxonomy.io.ParseException;

/**
 * This class reads in the nodes.dmp and names.dmp files from NCBI Taxonomy database
 * and converts them to a simple flat file of the form
 * 			tax_id \t parent tax_id \t rank \t name
 *  eg		2	131567	superkingdom	Bacteria
 *  It takes two arguments: the first is where the data directory is where the dump files are located.
 *  The second is the output file name 
 * @author michiel
 */
public class NcbiTaxDumpToSimpleFile {
	private File nodesFile;
	private File namesFile;
	private File outFile;
	private LinkedHashMap<Integer, TaxNode> nodes = new LinkedHashMap<Integer, TaxNode>();

	/**
	 * @param args (dump files dir) (output file)
	 */
	public static void main(String[] args) {
		if(args.length != 2 ){
			System.err.println("Usage: " + NcbiTaxDumpToSimpleFile.class.getSimpleName() + " <taxdump files dir> <output file>");
			System.exit(0);
		}
		
		NcbiTaxDumpToSimpleFile ntdsf = new NcbiTaxDumpToSimpleFile();
		ntdsf.go( args );
	}
	
	private void go( String[] args ){
		try {
			/*process file arguments, check and prepare*/
			processArgs( args );
			
			/*read in the nodes file*/
			System.out.println( "reading nodes file " + nodesFile.getAbsolutePath() );
			readNodes();
						
			/*read the names file and fetch scientific names*/
			System.out.println( "reading names file " + namesFile.getAbsolutePath() );
			readNames();

//			int c = 0;
//			for(Integer taxId : nodes.keySet() ){
//				c++;
//				System.out.println( nodes.get(taxId) );
//				if( c>25 ) break;
//			}

			System.out.println( "writing output file " + outFile.getAbsolutePath() );
			writeNodes();
			
			System.out.println( "finished." );
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * write nodes to output file
	 * @throws IOException
	 */
	private void writeNodes() throws IOException{
		//1	1	no rank	root
		//2	131567	superkingdom	Bacteria
		PrintWriter pw = new PrintWriter( outFile );
		for( Integer taxId : nodes.keySet() ){
			TaxNode tn = nodes.get(taxId);
			pw.format("%d\t%d\t%s\t%s\n", tn.getTaxID(), tn.getParentTaxID(), tn.getRank(), tn.getScientificName());
		}
		pw.close();
	}
	
	/**
	 * read the names and process them
	 * @throws IOException
	 * @throws ParseException
	 */
	private void readNames() throws IOException, ParseException{
		FileReader fr = new FileReader( namesFile);
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader( fr );
		String line;
		int lineNumber = 0;
		while( (line = br.readLine()) != null ){
			lineNumber++;
			//2	|	Bacteria	|	Bacteria <prokaryote>	|	scientific name	|
			String[] elmts = line.split( "\t\\|\t" );
			try{
				if( elmts.length > 3 && elmts[3].contains("scientific name") ){
					/*found a scientific name*/
					int taxId = Integer.parseInt( elmts[0] );
					TaxNode tn = nodes.get(taxId);
					if( tn == null ) throw new ParseException( "could not retrieve node for TaxID " + taxId );
					tn.setScientificName( elmts[1] );
				}
			}catch (NumberFormatException e) {
				throw new ParseException("could not parse element at line " + lineNumber + ": " + e.getMessage() );
			}
		}
		br.close();
		fr.close();
	}

	/**
	 * read in the nodes file
	 * @throws IOException
	 * @throws ParseException
	 */
	private void readNodes() throws IOException, ParseException{
		FileReader fr = new FileReader( nodesFile);
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader( fr );
		String line;
		int lineNumber = 0;
		while( (line = br.readLine()) != null ){
			lineNumber++;
			//2	|	131567	|	superkingdom	|		|	0	|	0	|	11	|	0	|	0	|	0	|	0	|	0	|		|
			String[] elmts = line.split( "\t\\|\t" );
			TaxNode tn = new TaxNode();
			try{
				//System.out.println( Arrays.toString(elmts) );
				int taxId = Integer.parseInt( elmts[0] );
				tn.setTaxID(taxId);
				
				int parentTaxId = Integer.parseInt( elmts[1] );
				tn.setParentTaxID(parentTaxId);
				
				tn.setRank(TaxonomyRank.getTaxonomyRank( elmts[2] ) );
				
			}catch (NumberFormatException e) {
				throw new ParseException("could not parse element at line " + lineNumber + ": " + e.getMessage() );
			}
			
			//if(lineNumber <= 25) System.out.println( tn );
			nodes.put(tn.getTaxID(), tn);
		}
		br.close();
		fr.close();
		
	}
	
	/**
	 * process file arguments, check and prepare
	 * @param args
	 * @throws IOException
	 */
	private void processArgs( String[] args ) throws IOException{
		File dumpDir = new File( args[0] );
		if( ! dumpDir.isDirectory() ) throw new IOException( "dump directory is not a directory: " + args[0] );
		if( ! dumpDir.exists() ) throw new IOException( "dump directory does not exist: " + args[0] );
		
		nodesFile = new File( dumpDir, "nodes.dmp" );
		if( ! nodesFile.exists() ) throw new IOException( "nodes file does not exist: nodes.dmp" );
		
		namesFile  = new File( dumpDir, "names.dmp" );
		if( ! namesFile.exists() ) throw new IOException( "names file does not exist: names.dmp" );
		
		outFile = new File( args[1] );
		if( outFile.exists() && ! outFile.delete() ) throw new IOException( "could not delete pre-existing output file: " + args[1] );
		
	}

}
