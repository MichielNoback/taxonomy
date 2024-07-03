/**
 * 
 */
package nl.bioinf.noback.taxonomy.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;

import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxNode;

/**
 * This is the Derby embedded implementation of TaxDB interaction. 
 * It has to be provided with a location of the database, the user name and password.
 * @author MA Noback (m.a.noback@pl.hanze.nl)
 * @version 1.0
 */
public class TaxonomyDaoDerby implements TaxonomyDao {
	private static TaxonomyDaoDerby uniqueInstance;
	private Connection connection = null;
	/**
	 * gi_prepared statement
	 */
	private PreparedStatement gi_ps;
	/**
	 * node table prepared statement
	 */
	private PreparedStatement node_ps;
	/**
	 * reusable resultset
	 */
	private ResultSet rs;
	/**
	 * for performance, hte retrieved lineages are cache in memory
	 */
	private HashMap<Integer, Lineage> lineageCache;
	/**
	 * for heap space safety reasons, a maximum cache size is set
	 */
	private int cacheSize = 10000;
	/**
	 * the number of cached lineages
	 */
	private int cachedLineages = 0;
	
	private TaxonomyDaoDerby( String database, String username, String password ) throws DatabaseException{
		this.lineageCache = new HashMap<Integer, Lineage>(); 
		connect( database, username, password );
	}

	private void connect( String database, String username, String password ) throws DatabaseException {
		Properties props = new Properties();
        props.put("user", username );
        props.put("password", password );
		String protocol = "jdbc:derby:";

        try{
			String driver = "org.apache.derby.jdbc.EmbeddedDriver";
			Class.forName(driver).newInstance();
			connection = DriverManager.getConnection(protocol + database + ";create=false", props);
			
			/*create prepared statements*/
			gi_ps = connection.prepareStatement( "select tax_id from gi_numbers where gi_number=?" );
			node_ps = connection.prepareStatement( "select * from nodes where tax_id=?" ); 
			
			//System.out.println( "connected to the database" );
        }catch (Exception e){
        	throw new DatabaseException("unable to connect to TaxDB at this time");
        }
	}

	/**
	 * Eagerly created Singleton pattern. Get the single instance of this class
	 * @return SequenceToolsWebDaoMySQLImpl instance
	 */
	public static TaxonomyDaoDerby getInstance( String database, String username, String password ) throws DatabaseException{
		if( uniqueInstance == null ){
			uniqueInstance = new TaxonomyDaoDerby( database, username, password );
		}
		return uniqueInstance;
	}

	/* (non-Javadoc)
	 * @see nl.bioinf.noback.taxonomy.dao.TaxonomyDao#disconnect()
	 */
	public void disconnect() throws DatabaseException {
		try {
			if( rs != null ){
				rs.close();
				rs = null;
			}
			if( gi_ps != null ){
				gi_ps.close();
				gi_ps = null;
			}
			if( node_ps != null ){
				node_ps.close();
				node_ps = null;
			}
			connection.close();
		} catch (SQLException e) {
			//e.printStackTrace();
			throw new DatabaseException( "closing the TaxDB connection encountered an Exception: " + e.getMessage() );
		}
	}
	
	/* (non-Javadoc)
	 * @see nl.bioinf.noback.taxonomy.dao.TaxonomyDao#getLineage(int)
	 */
	public Lineage getLineage(int taxID) throws DatabaseException {
		try{
			
			if( lineageCache.containsKey(taxID) ){
				//System.out.println( "returning cached lineage for tax_id=" + taxID );
				return lineageCache.get( taxID );
			}
			else{
				//System.out.println( "retrieving lineage from TaxDB for tax_id=" + taxID );
				//lineage = getLineage( taxID );
				Lineage lineage = new Lineage();
				boolean root = false;
				while( !root ){
					node_ps.setInt( 1, taxID );
					rs = node_ps.executeQuery();
					if(! rs.next() ){
						throw new DatabaseException("no result for tax_id number " + taxID );
					}
					else{
						//System.out.println( "tax_id=" + trs.getInt("tax_id") + " scientific name=" + trs.getString("scientific_name") );//+ " gi=" + rs.getInt("gi_number")
						taxID = rs.getInt("parent_tax_id");
						
						TaxNode node = new TaxNode();
						node.setTaxID( rs.getInt("tax_id") );
						node.setParentTaxID( rs.getInt("parent_tax_id") );
						node.setScientificName( rs.getString("scientific_name") );
						node.setRank( TaxonomyRank.getTaxonomyRank( rs.getString("rank") ) );
						lineage.addNode(node);
						if(rs.getInt("tax_id") == 1) root = true;
					}
					node_ps.clearParameters();
					//rs.close();
					//rs = null;
				}
				//System.out.println( "lineage retieved for tax_id=" + taxID + ": " + lineage );
				
				if( cachedLineages < cacheSize ){
					//System.out.println( "caching lineage for tax_id=" + taxID );
					lineageCache.put(lineage.getExternalNode().getTaxID(), lineage);
					cachedLineages++;
				}
				else{
					System.out.println( "exceeding cache size for stored lineages!" );
				}
				
				return lineage;
			}

		}catch (Exception e) {
			throw new DatabaseException("could not obtain lineage for tax_id " + taxID + ": " + e.getMessage() );
		}
	}

	@Override
	public Lineage getLineage(String organismName) throws DatabaseException {
		throw new DatabaseException("this method has not been implemented yet!");
	}


	/* (non-Javadoc)
	 * @see nl.bioinf.noback.taxonomy.dao.TaxonomyDao#getGiLineage(int)
	 */
	public Lineage getGiLineage( int giNumber ) throws DatabaseException {
		try {
			//ls = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
			//ls.setFetchSize(50); 
			
			gi_ps.setInt(1, giNumber);
			
			rs = gi_ps.executeQuery();
			//ResultSet grs = s.getResultSet();
			if(! rs.next() ){
				throw new DatabaseException("no result for gi number " + giNumber );
			}
			else{
				//System.out.println( "tax_id=" + grs.getInt("tax_id")  );
				int taxId = rs.getInt("tax_id");
				return getLineage( taxId );
			}
		} catch (Exception e) {
			//e.printStackTrace();
        	throw new DatabaseException("unable to obtain lineage for " + giNumber + " from TaxDB at this time. Extra info: " + e.getMessage() );
		}
	}
	
	/**
	 * for testing purposes only
	 * @param args
	 */
	public static void main( String[] args ){
		try {
			TaxonomyDaoDerby td = TaxonomyDaoDerby.getInstance("/data/docent/tax_db_lite/TaxDBlite","user","aquila69");
			td.test();
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * creates a lineage stored procedure and tests it
	 */
	@SuppressWarnings("unused")
	private void createGiLineageProcedure(){
		Statement s = null;
		ResultSet rs = null;
		try {
			s = connection.createStatement ();
			//s.executeQuery("DROP PROCEDURE gi_lineage IF EXISTS");
			//s.executeQuery(" DELIMITER //" +
			s.executeQuery(" CREATE PROCEDURE gi_lineage( IN q_gi_number INT )" +
					" BEGIN" +
					" select * from nodes where tax_id<10 " + 
					" END //" +
					" DELIMITER ;");
			
			s.executeQuery("call gi_lineage(10)");
			rs = s.getResultSet ();
			if(! rs.next() ){
				System.out.println("no result");
			}
			else{
				System.out.println( "tax_id=" + rs.getInt("tax_id") + " name=" + rs.getString("scientific_name") );			
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(s!=null){
				try {
					s.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				s = null;
			}
			if(rs!=null){
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				rs = null;
			}
		}
/*			DECLARE n_tax_id INT;
			DECLARE n_parent_tax_id INT;
			DECLARE n_rank varchar(100);
			DECLARE n_division_id smallint;
			DECLARE n_inherited_div boolean;
			DECLARE n_scientific_name varchar(200);
			DECLARE curr_tax_id INT;
			
			DROP TEMPORARY TABLE IF EXISTS node_lineage;
			
			CREATE TEMPORARY TABLE IF NOT EXISTS node_lineage(
				`tax_id` int, `parent_tax_id` int, `rank` varchar(100), `division_id` smallint, `inherited_div` boolean, `scientific_name` varchar(200)
			);
			
			SELECT tax_id INTO curr_tax_id FROM gi_numbers WHERE gi_number = q_gi_number;
			
			WHILE curr_tax_id != 1 DO
				
				SELECT tax_id, parent_tax_id, rank, division_id, inherited_div, scientific_name
				INTO n_tax_id, n_parent_tax_id, n_rank, n_division_id, n_inherited_div, n_scientific_name
				FROM nodes WHERE tax_id = curr_tax_id;
				
				INSERT INTO node_lineage (tax_id, parent_tax_id, rank, division_id, inherited_div, scientific_name) VALUES (n_tax_id, n_parent_tax_id, n_rank, n_division_id, n_inherited_div, n_scientific_name);
				
				SELECT parent_tax_id INTO curr_tax_id FROM nodes WHERE tax_id = curr_tax_id;
			END WHILE;
			
			SELECT tax_id, parent_tax_id, rank, division_id, inherited_div, scientific_name
			INTO n_tax_id, n_parent_tax_id, n_rank, n_division_id, n_inherited_div, n_scientific_name
			FROM nodes WHERE tax_id = curr_tax_id;
			
			INSERT INTO node_lineage (tax_id, parent_tax_id, rank, division_id, inherited_div, scientific_name) VALUES (n_tax_id, n_parent_tax_id, n_rank, n_division_id, n_inherited_div, n_scientific_name);
			
			SELECT * FROM node_lineage;
*/		
	}
	
	/**
	 * tests the database and connection
	 */
	private void test(){
		try {
			Statement s = connection.createStatement();
			s.executeQuery("select * from nodes where tax_id<10");
			ResultSet rs = s.getResultSet ();
			if(! rs.next() ){
				System.out.println("no result");
			}
			else{
				System.out.println( "tax_id 32767: tax_id=" + rs.getInt("tax_id") + " name=" + rs.getString("scientific_name") );			
			}
			s.close();
			s = null;
			rs.close();
			rs = null;
			
			//createGiLineageProcedure();
			getGiLineage( 158333234 );
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}
}
