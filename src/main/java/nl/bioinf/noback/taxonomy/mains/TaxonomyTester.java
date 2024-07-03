/**
 * 
 */
package nl.bioinf.noback.taxonomy.mains;

import java.util.ArrayList;

import nl.bioinf.noback.taxonomy.dao.TaxonomyDaoMysql;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxTree;

/**
 * @author MA Noback (m.a.noback@pl.hanze.nl)
 * @version 0.1
 */
public class TaxonomyTester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TaxonomyTester tt = new TaxonomyTester();
		tt.start();
	}
	
	public void start(){
		try {
			/*connect to TaxDB*/
			TaxonomyDaoMysql db = TaxonomyDaoMysql.getInstance( "TaxDB", "mysql.bin", "michiel", "19aquila69", "gi_numbers" );
			
			/*get a lineage*/
			Lineage lineage9906 = db.getLineage(9906);
			System.out.println( lineage9906.toString() );
			/*get another lineage*/
			Lineage lineage9606 = db.getLineage(9606);
			System.out.println( lineage9606.toString() );
			/*get another lineage*/
			Lineage lineage3702 = db.getLineage(3702);
			System.out.println( lineage3702.toString() );

			/*get intersection of the two lineages*/
/*			List<TaxNode> list9906 = lineage9906.getListFromRoot();
			List<TaxNode> list9606 = lineage9606.getListFromRoot();
			
			int smaller = (lineage9906.getLength() <= lineage9606.getLength() ? lineage9906.getLength() : lineage9606.getLength());
			//System.out.println( "smaller=" + smaller + " 9906=" + lineage9906.getLength() + " 9606=" + lineage9606.getLength());
			for( int i=0; i<smaller; i++){
				if( list9906.get(i).equals( list9606.get(i) ) ){
					System.out.println( "equal at level: " + i + "; " + list9906.get(i));
				}
				else{
					System.out.println( "lineages diverge at level: " + i + "; " + list9906.get(i) + "; " + list9606.get(i));
					break;
				}
			}
*/

/*			System.out.println("intersection=" + lineage9906.getIntersection(lineage9606));
			System.out.println("divergence=" + lineage9906.getDivergencePoint(lineage9606));
*/
/*			Iterator<TaxNode> tni = lineage9906.iterateFromRoot();
			while( tni.hasNext() ){
				System.out.println( tni.next() );
			}
*/			
			
			ArrayList<Integer> taxIDs = new ArrayList<Integer>();
			taxIDs.add(9906);
			taxIDs.add(9906); //human two times
			taxIDs.add(9606);
			taxIDs.add(3702);
			taxIDs.add(7955);
			taxIDs.add(5833);
			taxIDs.add(8355);
			taxIDs.add(562);
			taxIDs.add(10090);
			taxIDs.add(4932);
			taxIDs.add(10116);
			taxIDs.add(4577);
			taxIDs.add(4896);

			TaxTree tree = new TaxTree();
			for( int taxID : taxIDs ){
				tree.addLineage( db.getLineage( taxID ) );
			}
			
			//System.out.println( tree.getRoot() );
			System.out.println( tree );
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
