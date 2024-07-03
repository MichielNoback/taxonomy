package nl.bioinf.noback.taxonomy.model;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Encapsulates TaxTree experiments: produced from TaxTree xml file 
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.1
 */
public class TaxonomyExperiment {
	private String id;
	private String description;
	private Calendar date;
	private TaxTree tree;
	private String displayName;
	
	/**
	 * @param id
	 * @param description
	 * @param date
	 * @param tree
	 */
	public TaxonomyExperiment(String displayName, String id, String description, Calendar date,	TaxTree tree) {
		super();
		this.displayName = displayName;
		this.id = id;
		this.description = description;
		this.date = date;
		this.tree = tree;
	}
	
	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @return the date
	 */
	public Calendar getDate() {
		return date;
	}
	/**
	 * @return the tree
	 */
	public TaxTree getTree() {
		return tree;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		DateFormat df = DateFormat.getDateInstance();
		String d = df.format(this.date.getTime());
		return "TaxonomyExperiment [id=" + id + ", description=" + description + ", date=" + d + ", tree has " + tree.size() + " nodes]";
	}

	/**
	 * returns a List of top occurring nodes; call delegated to taxTree 
	 * @param rank
	 * @param occurrenceCutoff
	 * @param cumulative 
	 * @return
	 */
	public List<TaxNode> getTopOccurringNodes(TaxonomyRank rank, int occurrenceCutoff, boolean cumulative) {
		return tree.getTopOccurrence(rank, occurrenceCutoff, cumulative);
	}

	/**
	 * returns the total cumulative count of a given taxonomy level
	 * @param taxonomyRank
	 * @return cumulativeCount
	 */
	public int getTaxonomyLevelOccurrenceCount(TaxonomyRank taxonomyRank) {
		return tree.getTaxonomyLevelOccurrenceCount(taxonomyRank);
	}

	/**
	 * returns a String representation of the lineage for the goven taxNode object, from the root up
	 * @param te
	 * @param tn
	 * @return lineage String
	 */
	public String getLineageString( TaxNode tn ){
		StringBuilder lineageSb = new StringBuilder( "[" );
			Lineage l = tree.getLineage( tn );
			for( TaxNode t : l.getListFromRoot() ){
				if( ! (t.getScientificName().equals("root") || t.getScientificName().equals("cellular organisms") ) ){
					lineageSb.append( t.getScientificName() + ";");
				}
			}
			lineageSb.deleteCharAt(lineageSb.length()-1);
		lineageSb.append("]");
		return lineageSb.toString();
	}

}
