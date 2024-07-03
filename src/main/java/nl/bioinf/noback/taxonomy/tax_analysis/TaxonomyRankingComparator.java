package nl.bioinf.noback.taxonomy.tax_analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxNode;

/**
 * Creates a comparative listing of taxonomy count ranking  
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.1
 */
public class TaxonomyRankingComparator {
	private HashMap<String, RankComparison> rankComparisons;
	
	private TaxonomyRank rank;
	private TaxonomyRank subRank;
	
	public TaxonomyRankingComparator(TaxonomyRank rank, TaxonomyRank subRank){
		this.rankComparisons = new HashMap<String, RankComparison>();
		this.rank = rank;
		this.subRank = subRank;
	}
	
	/**
	 * adds ranking data for a given node and experiment
	 * @param lineage
	 * @param ranking
	 * @param experimentId
	 */
	public void addRankCountData( Lineage lineage, int ranking, String experimentId ){
		/*keep a list of sequential experiments added*/
		if( RankComparison.experimentIDs.size()==0 
				|| (!RankComparison.experimentIDs.get(RankComparison.experimentIDs.size()-1).equals(experimentId)) ){
			RankComparison.experimentIDs.add(experimentId);
			
		}
		TaxNode tn = lineage.getExternalNode();
		/*process the lineage*/
		if( rankComparisons.containsKey(tn.getScientificName() ) ){
			rankComparisons.get(tn.getScientificName()).addExperimentData(experimentId, ranking, tn.getCumulativeChildNumber());
		}
		else{
			RankComparison rc = new RankComparison(lineage, subRank);
			rc.addExperimentData(experimentId, ranking, tn.getCumulativeChildNumber());
			rankComparisons.put(tn.getScientificName(), rc );
		}
	}
	
	/**
	 * returns a sorted list of RankComparison objects, sorted on ranking of first, second etc experiment
	 * @return rankComparisons
	 */
	public List<RankComparison> getRankComparisons() {
		List<RankComparison> list = new ArrayList<RankComparison>();
		list.addAll( this.rankComparisons.values() );
		Collections.sort(list);
		return list;
	}

	/**
	 * returns the rank, the primary rank to show
	 * @return the rank
	 */
	public TaxonomyRank getRank() {
		return rank;
	}

	/**
	 * returns the sub-rank, the secondary rank to show
	 * @return the subRank
	 */
	public TaxonomyRank getSubRank() {
		return subRank;
	}

	/**
	 * returns the experimentIDs in the order in which they were added
	 * @return experimentIDs
	 */
	public List<String> getExperimentIDs() {
		List<String> l = new ArrayList<String>();
		l.addAll(RankComparison.experimentIDs);
		return l;
	}


	
}
