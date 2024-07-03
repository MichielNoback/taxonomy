package nl.bioinf.noback.taxonomy.tax_composition;

public class ParseException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public ParseException() {
		super();
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ParseException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * @param arg0
	 */
	public ParseException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public ParseException(Throwable arg0) {
		super(arg0);
	}


}
