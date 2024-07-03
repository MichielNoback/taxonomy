/**
 * 
 */
package nl.bioinf.noback.taxonomy.tax_composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import net.cellingo.sequence_tools.blast.BlastHit;
import net.cellingo.sequence_tools.blast.BlastHsp;
import net.cellingo.sequence_tools.blast.BlastQuery;
import net.cellingo.sequence_tools.blast.HspProperty;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDao;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoInMemory;
import nl.bioinf.noback.taxonomy.model.CorruptedLineageException;
import nl.bioinf.noback.taxonomy.model.Lineage;

/**
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.1
 */
public class BlastResultTaxonomyAnalyser {

	/**
	 * the taxDB object to retrieve lineages from
	 */
	private TaxonomyDao taxDB;
	/**
	 * the logger object
	 */
	private Logger logger;
	/**
	 * for efficiency purposes; keep track of the perfect hits in the current
	 */
	private ArrayList<BlastHit> currentperfectHits;
	/**
	 * keep track of numbers of different blast categories
	 */
	private HashMap<BlastCategory, Integer> blastCategoryNumbers;
	/**
	 * the maximum e-value to be analyzed when the minimum e-value is 0
	 */
	//private double eValueCutoff = 1E-10;
	/**
	 * the e-value of a hit can have as maximum "minimumEvalue * eValueMultiplier"    
	 */
	//private int eValueMultiplier = 10;
	/**
	 * the total number of hsps processed
	 */
	private int hitCount = 0;
	/**
	 * the total number of queries processed
	 */
	private int queryCount = 0;
	/**
	 * the number of gi numbers for which a lineage could not be retrieved
	 */
	private int failedLineageCount = 0;
	/**
	 * flag to indicate whether the Tax_id is present (and should be parsed) from the Hit_ID field
	 */
	private boolean parseTaxIdFromHitId;
	/**
	 * flag to indicate whether the gi number should be parsed and used for fetching the lineages
	 */
	private boolean useGiNumbersFile;
	/**
	 * strategy pattern implementer of the fetcher method for lineages
	 */
	private LineageFetcher lineageFetcher;
	
	//private HashMap<Integer, HashMap<String,Double>> truncHisto = new HashMap<Integer, HashMap<String,Double>>();
	
	/**
	 * construct with a Taxonomy data access object. Will parse a gi number from the Hit_id field 
	 * @param taxDB the taxonomy data access object
	 */
	public BlastResultTaxonomyAnalyser( TaxonomyDao taxDB, Logger logger  ) {
		this.taxDB = taxDB;
		parseTaxIdFromHitId = false;
		useGiNumbersFile = false;
		this.logger = logger;
		init();
	}
	
	/**
	 * Construct with a Taxonomy data access object and specify whether the hitID contains a TAXID field.
	 * parseTaxIdFromHitId is a flag that indicates whether the taxid should be used instead of gi number. 
	 * If this flag is set to true, the Hit_id field should contain a tax_id in this form:
	 * ref|AB087499|TAXID=200415 
	 * This flag overrdides the setting for flag useGiNumbers. If parseTaxIdFromHitId is true, useGiNumbers will not be considered.
	 * If parseTaxIdFromHitId is false and useGiNumbers is true, the gi-number will be parsed from the Hit-id field. 
	 * If both parseTaxIdFromHitId and useGiNumbers are set to false, an attempt will be made to parse the organism field from the 
	 * blast hit and use this one for fetching the lineages
	 * @param taxDB the taxonomy data access object
	 * @param parseTaxIdFromHitId
	 * @param useGiNumbersFile
	 * @param logger
	 */
	public BlastResultTaxonomyAnalyser( TaxonomyDao taxDB, boolean parseTaxIdFromHitId, boolean useGiNumbersFile, Logger logger ) {
		this.taxDB = taxDB;
		this.parseTaxIdFromHitId = parseTaxIdFromHitId;
		this.useGiNumbersFile = useGiNumbersFile;
		this.logger = logger;
		init();
		
		System.out.println("parsing TaxIDs from defline: " + parseTaxIdFromHitId + "; using gi numbers file: " + useGiNumbersFile + "; using TaxDB: " + taxDB.getClass().getSimpleName());
	}
	
	/*init block*/
	private void init(){
		currentperfectHits = new ArrayList<BlastHit>();
		blastCategoryNumbers = new HashMap<BlastCategory, Integer>();
		blastCategoryNumbers.put(BlastCategory.NO_MATCHES, 0);
		blastCategoryNumbers.put(BlastCategory.SINGLE_PERFECT_MATCH, 0);
		blastCategoryNumbers.put(BlastCategory.SINGLE_IMPERFECT_MATCH, 0);
		blastCategoryNumbers.put(BlastCategory.SINGLE_PERFECT_AND_IMPERFECT_MATCHES, 0);
		blastCategoryNumbers.put(BlastCategory.MULTIPLE_PERFECT_MATCHES, 0);
		blastCategoryNumbers.put(BlastCategory.MULTIPLE_PERFECT_AND_IMPERFECT_MATCHES, 0);
		blastCategoryNumbers.put(BlastCategory.MULTIPLE_IMPERFECT_MATCHES, 0);
		
		createLineageFetcher();
	}
	
	/**
	 * creates the required implementer of the lineage fetcher
	 */
	private void createLineageFetcher(){
		if( parseTaxIdFromHitId ){
			logger.info("fetching lineages using TaxId parsing");
			lineageFetcher = new LineageFetcher(){
				@Override
				public Lineage fetchLineage(BlastHit hit) {
					try {
						return taxDB.getLineage( extractTaxId(hit) );
					} catch (Exception e) {
						/*This lineage ignored*/
						failedLineageCount++;
						logger.warn( e.getMessage() );
					}
					return null;
				}
			};
		}
		else if( ! useGiNumbersFile ){
			logger.info("fetching lineages using gi number parsing");
			lineageFetcher = new LineageFetcher(){
				@Override
				public Lineage fetchLineage(BlastHit hit) {
					try {
						return taxDB.getGiLineage( extractGiNumber(hit) );
					} catch (Exception e) {
						/*This lineage ignored*/
						failedLineageCount++;
						logger.warn( e.getMessage() );
					}
					return null;
				}
			};
		}
		else{
			logger.info( "fetching lineages using organism parsing" );
			/*fetch lineage based on organism name*/
			lineageFetcher = new LineageFetcher(){
				@Override
				public Lineage fetchLineage(BlastHit hit) {
					try {
						String org = hit.getOrganism();
						
						return taxDB.getLineage( org );
					} catch (Exception e) {
						//e.printStackTrace();
						
						/*This lineage ignored*/
						failedLineageCount++;
						logger.warn( e.getMessage() );
					}
					return null;
				}
			};
		}
	}
	
	/**
	 * analyses the blast results of a query and returns the corresponding 
	 * longest possible lineage associated with it
	 * @param blastQuery
	 * @return lineage
	 */
	public Lineage analyseBlastResult( BlastQuery blastQuery ){
		/*count queries*/
		queryCount++;
		
		//TODO remove after testing
		TaxonomyDaoInMemory.queryCount++;
		
		/*clear previous data*/
		currentperfectHits.clear();
		
		Lineage lineage = null;
		/*determine category of hit*/
		try{
			BlastCategory category = determineBlastCategory( blastQuery );
			blastCategoryNumbers.put( category, blastCategoryNumbers.get(category)+1 );
			//System.out.println( "query = " + blastQuery.getQueryId() + "; blastCategory = " + category + " with " + blastQuery.getHspNumber() + " hits" );
			lineage = determineLineage( blastQuery, category );
			//if( lineage == null ){
				//System.err.println("ERROR NULL lineage!");
				//System.exit(1);
				
			//}
			if(lineage.getLength() == 0 ) logger.error( this.getClass().getSimpleName() + ": " + category + " returned an empty lineage" );

		}catch( NoSuchFieldException nsfe){
			nsfe.printStackTrace();
			
			logger.fatal("ERROR: " + Arrays.toString(nsfe.getStackTrace()));
			logger.fatal("ERROR: " + nsfe.getMessage());
			logger.fatal("ABORTING");
			System.exit(1);
		}
		catch (Exception e){
			e.printStackTrace();
			logger.warn("unable to determine blast category for query " + blastQuery.getQueryId());
			return null;
		}
		return lineage;
	}
	
	/**
	 * determines the lineage for a blastresult based on the nearest neighbor 
	 * @param blastQuery
	 * @param category
	 * @return lineage
	 */
	@SuppressWarnings("unused")
	private Lineage determineNearestNeighborLineage(BlastQuery blastQuery, BlastCategory category){
		Iterator<BlastHit> hits = blastQuery.getBlastHits();
		
		/*analyse only the top hit to obtain a lineage*/
		BlastHit hit = hits.next();
		try{
			Lineage lineage = lineageFetcher.fetchLineage(hit);
			//System.out.println( "retreived lineage" + lineage.getExternalNode() );
			//Fraction of lineage taken = fraction aligned * ( fraction identity + (fraction similarity / 2))
			return lineage;
		}catch (Exception e) {
			failedLineageCount++;
			logger.warn( e.getMessage() );
			return null;
		}
	}

	/**
	 * determines the longest possible lineage for a blastresult based on the predetermined blastcategory 
	 * @param blastQuery
	 * @param category
	 * @return lineage
	 */
	private Lineage determineLineage(BlastQuery blastQuery, BlastCategory category){
		logger.debug( "determining lineage of " + category + "; Query= " + blastQuery.getQueryId() );
		if( category == BlastCategory.SINGLE_PERFECT_MATCH 
				|| category == BlastCategory.SINGLE_PERFECT_AND_IMPERFECT_MATCHES ){
			
			BlastHit hit = currentperfectHits.get( 0 );
			
			return lineageFetcher.fetchLineage(hit);
		}
		else if( category == BlastCategory.MULTIPLE_PERFECT_MATCHES 
				|| category == BlastCategory.MULTIPLE_PERFECT_AND_IMPERFECT_MATCHES ){
			/*get lineages of each perfect hit and get the longest common lineage*/
			Lineage commonLineage = null;
			
			//TODO remove hitNumber; is only a testing counter
			int hitNumber = 0;
			for( BlastHit hit : currentperfectHits ){
				hitNumber++;
				/*get lineage linked to gi number*/
				try{
					Lineage lineage = lineageFetcher.fetchLineage(hit);

					if( commonLineage == null ) commonLineage = lineage;
					else{
						commonLineage = lineage.getIntersection( commonLineage );
					}
					/*useless to continue if in commonLineage only the [root] node remains*/
					if( commonLineage.getLength() < 2 ){
						logger.debug( "return shortest possible lineage at hit number: " + hitNumber );
						return commonLineage;
					}
				}catch (CorruptedLineageException e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
			/*return the longest common lineage*/
			return commonLineage;
		}
//		else if( category == BlastCategory.MULTIPLE_IMPERFECT_MATCHES ){
//			/*get lineages of each imperfect hit and get the longest common lineage*/
//			Lineage commonLineage = null;
//			int hitNumber = 0;
//			Iterator<BlastHit> hits = blastQuery.getBlastHits();
//			/*minimum e-value to decide when no more hits have to be analyzed*/
//			double minimumEvalue = 0;
//			while( hits.hasNext() ){
//				BlastHit hit = hits.next();
//				hitNumber++;
//				BlastHsp hsp = hit.getBestHsp();
//				
//				try{
//					/*determine e-value and determine whether this hit is to be analyzed*/
//					double eValue = hsp.getDoublePropertyValue( HspProperty.HSP_EVALUE );
//					if( hitNumber == 1 ) minimumEvalue = eValue;
//					if( (minimumEvalue == 0 && eValue < eValueCutoff) 
//							|| (eValue < eValueMultiplier*minimumEvalue ) ){
//						/*get lineage linked to gi number*/
//						Lineage lineage = lineageFetcher.fetchLineage(hit);
//
//						if( commonLineage == null ) commonLineage = lineage;
//						else{
//							commonLineage = lineage.getIntersection( commonLineage );
//						}
//						/*useless to continue if in commonLineage only the [cellular organisms]--[root] nodes remain*/
//						if( commonLineage.getLength() < 3 ){
//							logger.debug( "return shortest possible lineage at hit number: " + hitNumber );
//							return commonLineage;
//						}
//					}
//					else{
//						logger.debug( "hit analysis stopped at number " + hitNumber + " of " 
//								+ blastQuery.getHitNumber() + ": minimum Evalue=" + minimumEvalue + "; current Evalue=" + eValue );
//						return commonLineage;
//					}
//				}catch (Exception e) {
//					/*This lineage ignored*/
//					failedLineageCount++;
//					logger.warn( "error fetching lineage " + e.getMessage() + "; " + e.getCause() );
//					
//				}				
//			}
//			/*return the longest common lineage*/
//			return commonLineage;
//		}
		else if( category == BlastCategory.SINGLE_IMPERFECT_MATCH 
				|| category == BlastCategory.MULTIPLE_IMPERFECT_MATCHES){
			Iterator<BlastHit> hits = blastQuery.getBlastHits();
			
			/*analyse only the top hit to obtain a lineage*/
			BlastHit hit = hits.next();
			
			try{
				Lineage lineage = lineageFetcher.fetchLineage(hit);
				//System.out.println( "retreived lineage" + lineage.getExternalNode() );
				//Fraction of lineage taken = fraction aligned * ( fraction identity + (fraction similarity / 2))
				BlastHsp hsp = hit.getBestHsp();
				//int queryLength = blastQuery.getQueryLength();
				int alignLength = hsp.getIntegerPropertyValue( HspProperty.HSP_ALIGN_LENGTH );
				//System.out.println("alignLength="+alignLength);
				int identical = hsp.getIntegerPropertyValue( HspProperty.HSP_IDENTITIES );
				//System.out.println("identical="+identical);
				int similar = hsp.getIntegerPropertyValue( HspProperty.HSP_POSITIVES );
				//System.out.println("similar="+similar);
				
				double fractionAligned = hsp.getPropertyValue(HspProperty.HSP_ALIGN_PERCENTAGE)/100;//(double)alignLength/(double)queryLength;////(double)alignLength/(double)queryLength;
				//System.out.println("fractionAligned="+fractionAligned);

				double identity = (double)identical/(double)alignLength;
				double similarity = ((double)similar/(double)alignLength) - identity;
				//System.out.println(  "queryLength=" + queryLength + ";  alignLength=" + alignLength + "; identical=" + identical + "; similar=" + similar );
				//System.out.println( "fractionAligned=" + fractionAligned + ";  identity=" + identity + "; similarity=" + similarity);
				
				double lineageFraction = fractionAligned * ( identity + ( similarity / 2 )  );
				int truncatedLength = (int)(Math.ceil(lineageFraction * lineage.getLength() ));
				
//				HashMap<String,Double> histo = null;
//				if( truncHisto.containsKey(lineage.getLength() ) ){
//					histo = truncHisto.get( lineage.getLength() );
//				}else{
//					histo = getHistoElement();
//					truncHisto.put( lineage.getLength(), histo );
//				}
//				histo.put("number", ( histo.get("number")+1) );
//				histo.put("length", ( histo.get("length")+lineage.getLength() ) );
//				histo.put("truncatedLength", ( histo.get("truncatedLength")+truncatedLength ) );
//				histo.put("truncationLength", ( histo.get("truncationLength")+(lineage.getLength()-truncatedLength) ) );
//				histo.put("trauncationFraction", ( histo.get("trauncationFraction")+lineageFraction ) );

				
				//System.out.println( "lineageFraction=" + lineageFraction + " double new length=" + (lineageFraction * lineage.getLength()) );
				
//				if(lineage.getLength() <= 2){
//					System.out.println( "query " + hit.getHitID() + " short lineage: \n" + lineage );
//					System.out.println( "lineageFraction=" + lineageFraction + " lineage length=" + lineage.getLength() + " new lineage length=" + truncatedLength);
//				}
				//Lineage truncatedLineage = lineage.truncate( truncatedLength );
				//System.out.println("returning truncated copy of length: " + truncatedLineage.getLength());
				
				return lineage.truncate( truncatedLength );
			}catch (Exception e) {
				/*This lineage ignored*/
				failedLineageCount++;
				logger.warn( e.getMessage() );
				return null;
			}
		}
		return null;
	}
	
//	private HashMap<String,Double> getHistoElement( ){
//		HashMap<String,Double> element = new HashMap<String,Double>();
//		element.put("number", 0.0);
//		element.put("length", 0.0);
//		element.put("truncatedLength", 0.0);
//		element.put("truncationLength", 0.0);
//		element.put("trauncationFraction", 0.0);
//		
//		return element;
//	}
	
//	public void printTruncHistoGram(){
//		ArrayList<Integer> lengths = new ArrayList<Integer>();
//		lengths.addAll( this.truncHisto.keySet() );
//		for( int l : lengths ){
//			System.out.println("LENGTH: " + l);
//			int number = (truncHisto.get(l).get("number")).intValue();
//			System.out.println("\tnumber: " + number);
//			System.out.println("\tlength: "+ (truncHisto.get(l).get("length")/number) );
//			System.out.println("\ttruncatedLength: "+ (truncHisto.get(l).get("truncatedLength")/number) );
//			System.out.println("\ttruncationLength: "+ (truncHisto.get(l).get("truncationLength")/number) );
//			System.out.println("\ttrauncationFraction: "+ (truncHisto.get(l).get("trauncationFraction")/number) );
//		}
//		
//	}
	
	/**
	 * determines the blast category
	 * @param blastQuery
	 * @return blastCategory
	 * @throws Exception
	 */
	private BlastCategory determineBlastCategory(BlastQuery blastQuery) throws NoSuchFieldException, Exception {
		//int imperfectHits = 0;
		int perfectHits = 0;
		
		/*immediately return if no hits present*/
		int hitNumber = blastQuery.getHspNumber();
		hitCount += hitNumber;
		if(hitNumber == 0 ) return BlastCategory.NO_MATCHES;
		
		/*iterate over the blast hits*/
		int queryLength = blastQuery.getQueryLength();
		Iterator<BlastHit> blastHits = blastQuery.getBlastHits();
		while( blastHits.hasNext() ){
			BlastHit hit = blastHits.next();
			/*now proceed with the first HSP of each hit*/
			try{
				BlastHsp hsp = hit.getFirstHsp();
				
				int alignLength = hsp.getIntegerPropertyValue( HspProperty.HSP_ALIGN_LENGTH );
				int identities = hsp.getIntegerPropertyValue( HspProperty.HSP_IDENTITIES );
				
				/*is it a full length perfect match?*/
				if( queryLength == alignLength && queryLength == identities ){
					perfectHits++;
					currentperfectHits.add( hit );
				}
				//else imperfectHits++;
			}
			catch(NoSuchFieldException nsfe){
				throw nsfe;
			}
			catch (Exception e) {
				Exception e1 = new Exception( "unable to determine blast category for query " + blastQuery.getQueryId() );
				e1.setStackTrace(e.getStackTrace());
				throw e1;
//				throw new Exception( "unable to determine blast category for query " + blastQuery.getQueryId() );
			}
		}
		
		/*determine the category and return*/
		if( hitNumber == 1 ){
			if( perfectHits == 1 ) return BlastCategory.SINGLE_PERFECT_MATCH;
			else return BlastCategory.SINGLE_IMPERFECT_MATCH;
		}
		else{
			if( perfectHits == hitNumber ) return BlastCategory.MULTIPLE_PERFECT_MATCHES;
			else{
				if( perfectHits == 0 ) return BlastCategory.MULTIPLE_IMPERFECT_MATCHES;
				if( perfectHits == 1 ) return BlastCategory.SINGLE_PERFECT_AND_IMPERFECT_MATCHES;
				else return BlastCategory.MULTIPLE_PERFECT_AND_IMPERFECT_MATCHES;
			}
		}
	}
	
	/**
	 * parses the hitID field (HIT_ID) and extracts the taxID
	 * @param hit
	 * @return taxID
	 * @throws ParseException
	 */
	public int extractTaxId( BlastHit hit ) throws ParseException, NoSuchFieldException{
		String hitID = hit.getHitID();
		if( hitID.contains("TAXID=") ){
			//ref|AB087499|TAXID=200415
			try{
				return Integer.parseInt( hitID.substring( hitID.indexOf("TAXID=") + 6 ) );
			}
			catch (NumberFormatException e) {
				throw new ParseException( "failed to extract taxID from hitID (" + hitID + ") from BlastHit " + hit );
			}
		}
		else throw new ParseException( "failed to extract taxID from hitID (" + hitID + ") from BlastHit " + hit );
		
	}
	
	/**
	 * parses the hitID field (HIT_ID) and returns the gi number
	 * @param hit
	 * @return gi number
	 * @throws NoSuchFieldException 
	 */
	public int extractGiNumber( BlastHit hit ) throws ParseException, NoSuchFieldException{
		String hitID = hit.getHitID();
//		logger.warn( "failed to parse hit id: " + hitID + ";" + e.getMessage() );

		if( hitID.startsWith("gi") ){
			String[] elements = hitID.split("\\|");
			return Integer.parseInt( elements[1] );
		}
		else throw new ParseException( "failed to extract gi from hitID (" + hitID + ") from BlastHit " );
	}

	/**
	 * @return the blastCategoryNumbers
	 */
	public HashMap<BlastCategory, Integer> getBlastCategoryNumbers() {
		return blastCategoryNumbers;
	}

	/**
	 * @return the overall hit count
	 */
	public int getHitCount() {
		return hitCount;
	}

	/**
	 * @return the overall query count
	 */
	public int getQueryCount() {
		return queryCount;
	}
	
	/**
	 * returns the number of gi numbers for which no lineage
	 * could be retrieved
	 * @return failedLineageCount
	 */
	public int getFailedLineageCount(){
		return failedLineageCount;
	}
	
	/**
	 * sets the log4j logger to write messages to
	 * @param logger
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

}
