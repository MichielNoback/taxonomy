/**
 * 
 */
package nl.bioinf.noback.taxonomy.dao;

/**
 * @author MA Noback (m.a.noback@pl.hanze.nl)
 * @version 0.1
 */
public class DatabaseException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public DatabaseException() {
	}

	/**
	 * @param message
	 */
	public DatabaseException(String message) {
		super(message);
	}


}
