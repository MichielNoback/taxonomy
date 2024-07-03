/**
 * 
 */
package nl.bioinf.noback.taxonomy.tax_composition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import net.cellingo.sequence_tools.blast.BlastHit;
import net.cellingo.sequence_tools.blast.BlastHsp;
import net.cellingo.sequence_tools.blast.BlastProgram;
import net.cellingo.sequence_tools.blast.BlastQuery;
import net.cellingo.sequence_tools.blast.HspProperty;
import net.cellingo.utils.conversion.ValueType;

/**
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.1
 */
public class BlastResultsReader {
	
	private File inputFile;
	private boolean headerProcessed = false;
	/*properties as read from the header line*/
	private ArrayList<HspProperty> properties;
	/*the reader that accesses the file*/
	private BufferedReader br = null;
	/*the listener object for streaming parsing; to be called with each new query results*/
	private BlastResultsReaderListener listener;

	
	/**
	 * Constructs with the file to be parsed. 
	 */
	public BlastResultsReader( File inputFile, BlastResultsReaderListener listener ) {
		this.inputFile = inputFile;
		this.listener = listener;
	}
	
	/**
	 * method that read and processes the header line of the file
	 */
	private ArrayList<HspProperty> readHeader()throws Exception{
		
		properties = new ArrayList<HspProperty>();
		
		try {
			br =  new BufferedReader(new FileReader(inputFile));
			String line = br.readLine();
			
			if( line.startsWith("#SOURCE=") ){
				String bpStr = line.substring(8);
				BlastProgram bp = BlastProgram.valueOf(bpStr);
				BlastHsp.setBlastProgram(bp);
				/*read next line which will be regular header*/
				line = br.readLine();
			}
			
			if( ! line.startsWith("QUERY_ID")){
				throw new Exception( "input file does not have a header line" );
			}
			
			String[] elements = line.split("\t");
			
			for( int i=1; i < elements.length; i++ ){
				String element = elements[i];
				HspProperty property = HspProperty.valueOf( element );
				//System.out.println( "HspProperty=" + property );
				properties.add( property );
			}
			headerProcessed = true;
			/*return the list*/
			return properties;
			
		}catch (Exception e) {
			e.printStackTrace();
			throw new Exception( "could not open or read from input file; " + e.getMessage() );
		}
		
	}
	
	/**
	 * Will return a list of Hsp properties that were read from the 
	 * header line of the blast results input file.
	 * @return list of HspProperties
	 */
	public ArrayList<HspProperty> getDataFields()throws Exception{
		
		if( ! headerProcessed )	properties = readHeader();
		
		return properties;
	}

	/**
	 * reads the file and passes each read query result to the listener 
	 * @throws Exception
	 */
	public void readFile() throws Exception{
		//read first line if not already done
		if( ! headerProcessed )	properties = readHeader();

		//read the rest of the file
		if( br == null ) throw new Exception( "could not read from input file" );
		
		BlastQuery blastQuery = null;
		String previousQueryId = "";
		int queryNumber = 0;
		int hitNumber = 0;
		String line;
		
		while( (line = br.readLine()) != null ){
			//System.out.println( line );
			String[] elements = line.split("\t");
			String queryID = elements[0];
			
			if( ! queryID.equals( previousQueryId ) ){ //new query being parsed
				previousQueryId = queryID; //set the "previous" name
				
				if( queryNumber > 0 ) listener.queryDataProcessed( blastQuery ); //pass the old query to the listener object
				
				//System.out.println( "new query: " + queryID );
				blastQuery = new BlastQuery( queryID ); //create a new one
				hitNumber = 0;
				queryNumber++;
			}
			hitNumber++;
			BlastHit hit = new BlastHit( );
			BlastHsp hsp = new BlastHsp( );
			
			//TODO is this relocation correct?
//			hit.addHsp( hsp );
//			blastQuery.addBlastHit( hit );
			
			/*process the other elements of the line*/
			for( int i=0; i<properties.size(); i++){
				HspProperty property = properties.get(i);
				String propValue = elements[i+1]; //first one was QUERY_ID
				
				//System.out.println( property + ": " + propValue );
				
				/*process query length*/
				if( property == HspProperty.QUERY_LENGTH ){
					int queryLength = Integer.parseInt( propValue );
					if( hitNumber == 1 ) blastQuery.setQueryLength( queryLength );
					hsp.setIntegerPropertyValue(HspProperty.QUERY_LENGTH, queryLength);
				}
				/*process hit level properties*/
				else if( property == HspProperty.HIT_ID ) hit.setHitID( propValue );
				else if( property == HspProperty.HIT_ACCESSION ) hit.setHitAccession( propValue );
				else if( property == HspProperty.HIT_DEFINITION ) hit.setHitDefinition( propValue );
				else if( property == HspProperty.HIT_LENGTH ) hit.setHitLength( Integer.parseInt(propValue) );
				else{
					/*process HSP level properties; assume one HSP per HIT*/
					if( HspProperty.getValueType(property) == ValueType.DOUBLE ) 
						hsp.setDoublePropertyValue(property, Double.parseDouble( propValue ) );
					else if( HspProperty.getValueType(property) == ValueType.INTEGER ) 
						hsp.setIntegerPropertyValue(property, Integer.parseInt( propValue ) );
					else if( HspProperty.getValueType(property) == ValueType.STRING ) 
						hsp.setStringPropertyValue(property, propValue );
					//else property ignored; complex types cannot be processed here
				}
			}
			
			//TODO remove this test
//			if(hsp.getDoublePropertyValue( HspProperty.HSP_ALIGN_PERCENTAGE ) > 90 
//					&& hsp.getDoublePropertyValue( HspProperty.HSP_EVALUE ) < 0.01){//
				hit.addHsp( hsp );
				blastQuery.addBlastHit( hit );
//			}
		}
		
		/*pass last query*/
		if( blastQuery != null ) listener.queryDataProcessed( blastQuery );
		
		if(br != null)	br.close();
		
	}
	
	
}
