/**
 * 
 */
package nl.bioinf.noback.taxonomy.model;

/**
 * General purpose filtering interface
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.1
 */
public interface TaxNodeFilter {
	/**
	 * filters the given taxnode and returns true if the node passes the filter
	 * @param node
	 * @return node passes
	 */
	public boolean filterNode(TaxNode node);
}
