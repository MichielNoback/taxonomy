package nl.bioinf.noback.taxonomy.mains;

import java.io.File;
import java.util.HashMap;

import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoInMemory;
import nl.bioinf.noback.taxonomy.model.TaxNode;

public class TaxDBstatsAnalyser {
    public boolean VERBOSE = true;
	private TaxonomyDaoInMemory taxDB;
	private HashMap<Integer, TaxNode> taxNodes; 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if( args.length != 1){
			//out.println("");
			System.out.println("no TaxDB file provided!");
			System.out.println("usage: java -jar TaxDBstatsAnalyser_<version>.jar <TaxDB file>");
			System.out.println("aborting");
			System.exit( 1 );
		}
		TaxDBstatsAnalyser tdsa = new TaxDBstatsAnalyser();
		tdsa.start(args[0]);
	}


	private void start( String taxDBfile ){
		try {
			loadTaxDB(taxDBfile);
			createStats();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void loadTaxDB(String taxDBfileName) throws Exception{
		File taxDBfile = new File(taxDBfileName );
		
		this.taxDB = new TaxonomyDaoInMemory( taxDBfile, null);
		if( VERBOSE ){
			System.out.println("[ " + this.getClass().getSimpleName() + " ] taxDB loaded: ");
			taxDB.countNodes();
		}
		
		this.taxNodes = taxDB.getTaxNodes();
	}
	
	
	private void createStats() {
		HashMap<TaxonomyRank, Integer> levelCounts = new HashMap<TaxonomyRank, Integer>();
		/*init the map*/
		for( TaxonomyRank rank : TaxonomyRank.values() ){
			levelCounts.put(rank, 0);
		}
		/*iterate the taxDB*/
		//int count = 0;
		for( TaxNode tn : this.taxNodes.values()){
			//count++;
			TaxonomyRank tr = tn.getRank();
			levelCounts.put( tr, levelCounts.get(tr)+1 );
			
			//if(count==10) break;
		}
		//checksum
		int total = 0;
		for( TaxonomyRank tr : levelCounts.keySet() ){
			total += levelCounts.get(tr);
			System.out.println(tr + "\t" + levelCounts.get(tr));
		}
		System.out.println("Total number of nodes: " + total);
	}



}
