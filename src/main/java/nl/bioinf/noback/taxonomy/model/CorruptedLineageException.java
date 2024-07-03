/**
 * 
 */
package nl.bioinf.noback.taxonomy.model;

/**
 * @author MA Noback (m.a.noback@pl.hanze.nl)
 * @version 0.1
 */
public class CorruptedLineageException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public CorruptedLineageException() {	}

	/**
	 * @param arg0
	 */
	public CorruptedLineageException(String arg0) {
		super(arg0);
	}

}
