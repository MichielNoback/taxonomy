package nl.bioinf.noback.phylogeny.phylotree;

public class NewickFormatException extends Exception {
	String message;
	public NewickFormatException(String string) {
		message = string;
	}
	public String getMessage(){
		return message;
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
