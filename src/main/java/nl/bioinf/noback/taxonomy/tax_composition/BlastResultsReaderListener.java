/**
 * 
 */
package nl.bioinf.noback.taxonomy.tax_composition;

import net.cellingo.sequence_tools.blast.BlastQuery;

/**
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.1
 */
public interface BlastResultsReaderListener {
	/**
	 * Method that will be called on the listener when the data of a 
	 * single query have been processed 
	 * @param query
	 */
	public void queryDataProcessed( BlastQuery query );

}
