/**
 * 
 */
package nl.bioinf.noback.taxonomy.tax_analysis;

/**
 * @author michiel
 *
 */
public enum AnalysisType {
	TAX_LEVEL_OVERLAP("summary analysis of taxonomy and analysis of pairwise tree overlap over all available taxonomy levels"),
	TAX_LEVEL_OVERVIEW("summary analysis of taxonomic levels");
	private String type;
	
	private AnalysisType(String type){
		this.type = type;
	}
	
	public String toString(){
		return this.name() + " (" + type + ")";
	}
}
