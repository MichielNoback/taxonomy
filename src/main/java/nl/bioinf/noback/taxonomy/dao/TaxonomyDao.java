/**
 * 
 */
package nl.bioinf.noback.taxonomy.dao;

import nl.bioinf.noback.taxonomy.model.Lineage;

/**
 * This interface defines the methods required of an
 * data access object interfacing with the TaxDB database.
 * @author MA Noback (m.a.noback@pl.hanze.nl)
 * @version 0.1
 */
public interface TaxonomyDao {
	/**
	 * returns a Lineage from the given taxID to the root
	 * @param taxID
	 * @return lineage
	 * @throws DatabaseException
	 */
	public Lineage getLineage( int taxID ) throws DatabaseException;
	

	/**
	 * returns a Lineage from the given gi_number-related taxID to the root
	 * @param giNumber
	 * @param giTable
	 * @return lineage
	 * @throws DatabaseException
	 */
	public Lineage getGiLineage( int giNumber ) throws DatabaseException;
	
	/**
	 * returns a lineage based on the given organism name. Throws an exception when the 
	 * name is not present.
	 * @param organismName
	 * @return lineage
	 * @throws DatabaseException
	 */
	public Lineage getLineage( String organismName ) throws DatabaseException;

	/**
	 * disconnects from the database
	 * @throws DatabaseException
	 */
	public void disconnect() throws DatabaseException;
	
}
