package nl.bioinf.noback.taxonomy.tax_analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxNode;

/**
 * class to represent a comparison row for comparison of node ranking 
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.1	 
 * */
public class RankComparison implements Comparable<RankComparison>{
	protected static ArrayList<String> experimentIDs = new ArrayList<String>();
	private Lineage lineage;
	private TaxonomyRank subRank;
	private HashMap<String, RankingCountPair> pairs = new HashMap<String, RankingCountPair>();
	private boolean uninitialized = true;
	
	public RankComparison(Lineage lineage, TaxonomyRank subRank){
		this.lineage = lineage;
		this.subRank = subRank;
	}
	
	/**
	 * adds experiment-specific data to this data object
	 * @param experimentID
	 * @param ranking
	 * @param count
	 */
	public void addExperimentData(String experimentID, int ranking, int count){
		RankingCountPair p = new RankingCountPair(ranking, count);
		pairs.put(experimentID, p);
		uninitialized = true;
	}
	
	/**
	 * returns the primary name of this comparison; the scientific name of the external node of the lineage
	 * @return primary name
	 */
	public String getPrimaryRankName(){
		return lineage.getExternalNode().getScientificName();
	}
	
	/**
	 * returns the subRank name of this comparison; the scientific name of the subrank node of the lineage
	 * @return subRank name
	 */
	public String getSubRankName(){
		for( TaxNode tn : lineage.getListFromLeaf() ){
			if( tn.getRank() == subRank ) return tn.getScientificName();
		}
		return "NOT FOUND";
	}
	
	/**
	 * returns a list of RankingCountPairs in the order in which they were entered
	 * @param experimentIDs the order in which the count data should be returned
	 * @return
	 */
	public List<RankingCountPair> getRankingCountPairs( ){
		if( uninitialized ){
			initEmptyPairs();
		}

		List<RankingCountPair> countPairs = new ArrayList<RankingCountPair>();
		for( String id : experimentIDs ){
			if( this.pairs.containsKey(id)){
				RankingCountPair rcp = this.pairs.get(id);
				countPairs.add(rcp);
			}
			else{
				//countPairs.add(new RankingCountPair(Integer.MAX_VALUE, 0));
				throw new IllegalArgumentException("the ID " + id + " is not present in this RankComparison series");
			}
		}
		return countPairs;
	}
	
	/**
	 * returns the rank count pair for a given expID
	 * @param expID
	 * @return RankingCountPair
	 */
	public RankingCountPair getRankingCountPair( String expID ){
		if( uninitialized ){
			initEmptyPairs();
		}
		if( this.pairs.containsKey(expID)) return this.pairs.get(expID);
		else{
			assert false : this.getClass().getSimpleName() + " (" + this.getPrimaryRankName() + ") has no RankingCountPair for " + expID;
			return null;
		}
	} 

	/**
	 * initializes rank/count Pair data for experiments without this species 
	 */
	private void initEmptyPairs(){
		//System.out.println(this.getClass().getSimpleName() + " (" + this.getPrimaryRankName() + ") initializing ");
		for( String id : experimentIDs ){
			//System.out.println("\t expID=" + id + "; present: " + this.pairs.containsKey(id));
			if( ! this.pairs.containsKey(id)){
				pairs.put(id, new RankingCountPair(Integer.MAX_VALUE, 0));
			}
		}
		uninitialized = false;
	}
	
	public String toString(){
		StringBuilder ranksb = new StringBuilder();
		//List<RankingCountPair> l = getRankingCountPairs();
		for(String id : experimentIDs ){
			ranksb.append(" ");
			ranksb.append(id);
			ranksb.append(getRankingCountPair(id));
		}
		
		String rep = "\nRankComparison:[" + this.getPrimaryRankName() + " subrank=" + this.getSubRankName() 
			+ " exps={" + ranksb.toString() + "}]";
		return rep;
	}
	
	@Override
	public int compareTo(RankComparison other) {
		if( uninitialized ){
			initEmptyPairs();
		}
		/*will sort on the first experimentID, then the second, third etc.*/
		for( String expID : experimentIDs ){
			assert this.pairs.containsKey(expID) : "expID " + expID + "should have been initialised";
			if( getRankingCountPair(expID).ranking == other.getRankingCountPair(expID).ranking ) continue;
			else return getRankingCountPair(expID).ranking - other.getRankingCountPair(expID).ranking;
		}
		/*equal for all experimentIDs*/
		return 0;
	}
	
	/**
	 * a pair of rank and count
	 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
	 * @version 0.1	 
	 * */
	public class RankingCountPair{
		int ranking;
		int count;

		public RankingCountPair(int ranking, int count){
			this.ranking = ranking;
			this.count = count;
		}
		
		public String toString(){
			String r = (ranking==Integer.MAX_VALUE ? "NA" : ""+ranking );
			return "[rank=" + r + " count=" + count + "]";
		}
	}

}
