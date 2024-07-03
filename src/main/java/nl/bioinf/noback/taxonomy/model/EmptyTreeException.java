/**
 * 
 */
package nl.bioinf.noback.taxonomy.model;

/**
 * @author MA Noback (m.a.noback@pl.hanze.nl)
 * @version 0.1
 */
public class EmptyTreeException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EmptyTreeException(){ }
	
	public EmptyTreeException( String message ){
		super(message);
	}
}
