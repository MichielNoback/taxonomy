/**
 * 
 */
package nl.bioinf.noback.taxonomy.tax_composition;

/**
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 1.0
 */
public enum BlastCategory {
	NO_MATCHES("1: no matches"),
	SINGLE_PERFECT_MATCH("2: one perfect match"),
	MULTIPLE_PERFECT_MATCHES("3: multiple perfect matches"),
	SINGLE_PERFECT_AND_IMPERFECT_MATCHES("3: one perfect and one or more imperfect matches"),
	MULTIPLE_PERFECT_AND_IMPERFECT_MATCHES("4: multiple perfect and one or more imperfect matches"),
	SINGLE_IMPERFECT_MATCH("5: one imperfect match"),
	MULTIPLE_IMPERFECT_MATCHES("6: multiple imperfect matches");
	
	private String type;
	
	private BlastCategory( String type ){
		this.type = type;
	}
	
	public String toString(){
		return type;
	}
}
