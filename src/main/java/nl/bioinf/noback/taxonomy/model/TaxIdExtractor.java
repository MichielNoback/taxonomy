package nl.bioinf.noback.taxonomy.model;

import java.text.ParseException;

public interface TaxIdExtractor {
	/**
	 * given a String line, extract the TaxId from it and return it
	 * @param line
	 * @return taxID
	 */
	public Pair extractTaxIdCount( String line ) throws ParseException;
	
	public class Pair{
		public int taxID;
		public int count;
		
		public Pair(int taxID, int count){
			this.taxID = taxID;
			this.count = count;
		}
	}
}
