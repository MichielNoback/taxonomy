/**
 * 
 */
package nl.bioinf.noback.taxonomy.tax_analysis;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import nl.bioinf.noback.taxonomy.model.EmptyTreeException;
import nl.bioinf.noback.taxonomy.model.TaxonomyExperiment;

/**
 * @author michiel
 *
 */
public abstract class TaxTreeAnalyser {

	public static TaxTreeAnalyser getInstance( AnalysisType type, Properties analysisProperties ) {
		switch(type){
			case TAX_LEVEL_OVERLAP: return new TaxTreeLevelOverlapAnalyser(analysisProperties, type);
			case TAX_LEVEL_OVERVIEW: return new TaxTreeLevelOverlapAnalyser(analysisProperties, type);
			default: return null;
		}
	}
	
	/**
	 * performs subclass-specific analysis pairwise on all experiments in the list and appends results to the given report file
	 * @param reportFile
	 * @param taxExperiments
	 * @throws IOException 
	 */
	public abstract void analyse( File reportFile, List<TaxonomyExperiment> taxExperiments ) throws IOException, EmptyTreeException;
	
}
