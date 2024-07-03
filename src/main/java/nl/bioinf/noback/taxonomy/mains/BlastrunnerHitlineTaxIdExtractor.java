package nl.bioinf.noback.taxonomy.mains;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.bioinf.noback.taxonomy.model.TaxIdExtractor;

public class BlastrunnerHitlineTaxIdExtractor implements TaxIdExtractor {
	private Pattern extractPattern = Pattern.compile("TAXID=(\\d+)\\t");
	
	@Override
	public Pair extractTaxIdCount(String line) throws ParseException {
		try{
			//System.out.println("@@@taxID=" );
			//lactoF	ref|DQ007321|TAXID=330834	16S ribosomal RNA [Tetrasphaera jenkinsii]	DQ007321	21	1	21	0.00214218	17	20	20	21
			Matcher m = extractPattern.matcher(line);
			boolean found = m.find(); 
			if(found){
				//System.out.println("@@@taxID=" + m.group(1));
				return new Pair(Integer.parseInt(m.group(1)), 1);
			}
			else{
				//System.out.println("###taxID not extracted from line " + line);
				throw new ParseException("failed to extract taxID from line (" + line + ")", 0 );
			}
		}
		catch(IllegalStateException e){
			throw new ParseException("failed to extract taxID from line (" + line + ")", 0 );
		}
		catch (NumberFormatException e) {
			throw new ParseException("failed to extract taxID from line (" + line + ")", 0 );
		}
	}

	public static void main(String[] args){
		BlastrunnerHitlineTaxIdExtractor t = new BlastrunnerHitlineTaxIdExtractor();
		try {
			Pair idCount = t.extractTaxIdCount("lactoF	ref|DQ007321|TAXID=330834	16S ribosomal RNA [Tetrasphaera jenkinsii]	DQ007321	21	1	21	0.00214218	17	20	20	21");
			System.out.println("taxID=" + idCount.taxID);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
