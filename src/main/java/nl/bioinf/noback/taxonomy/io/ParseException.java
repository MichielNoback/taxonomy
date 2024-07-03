package nl.bioinf.noback.taxonomy.io;

/**
 * exception to be thrown when an error occusr during parsing of a file
 * @author michiel
 *
 */
public class ParseException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ParseException() {
	}

	public ParseException(String message) {
		super(message);
	}

	public ParseException(Throwable cause) {
		super(cause);
	}

	public ParseException(String message, Throwable cause) {
		super(message, cause);
	}

}
