/**
 * 
 */
package nl.bioinf.noback.taxonomy.tax_composition;

import net.cellingo.sequence_tools.blast.BlastHit;
import nl.bioinf.noback.taxonomy.model.Lineage;

/**
 * simple interface specifying a sinngle method that should be implemented for fetching a 
 * lineage based on a BlastHit
 * @author michiel
 */
public interface LineageFetcher {
	public Lineage fetchLineage( BlastHit hit );
}
