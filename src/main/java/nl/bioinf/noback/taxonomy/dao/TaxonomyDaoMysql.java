/**
 *
 */
package nl.bioinf.noback.taxonomy.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.model.CorruptedLineageException;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxNode;

/**
 * @author MA Noback (m.a.noback@pl.hanze.nl)
 * @version 0.1
 */
public class TaxonomyDaoMysql implements TaxonomyDao {

    private static TaxonomyDaoMysql uniqueInstance;
    private Connection connection = null;
    private String dbUsername;
    private String database;
    private String host;
    private String password;

    private PreparedStatement taxIdPreparedStatement;
    /**
     * for performance, the retrieved lineages are cache in memory
     */
    private HashMap<Integer, Lineage> lineageCache;
    /**
     * for heap space safety reasons, a maximum cache size is set
     */
    private int cacheSize = 20000;
    private int cachedLineages = 0;
    private PreparedStatement callLineagePreparedStatement;
    /**
     * the gi_numbers table to get gi numbers from (protein / RNA gi numbers)
     */
    private String giTable;

    @SuppressWarnings("unused")
    private int cachedReturns;

    /**
     * private constructor can only be called from within this class: getINstance()
     *
     * @param database
     * @param host
     * @param username
     * @param password
     * @throws DatabaseException
     */
    private TaxonomyDaoMysql(String database, String host, String username, String password, String giTable)
            throws DatabaseException {
        this.lineageCache = new HashMap<Integer, Lineage>();
        this.database = database;
        this.host = host;
        this.dbUsername = username;
        this.password = password;
        this.giTable = giTable;

        connect();
    }

    private void connect() throws DatabaseException {
        try {
            String url = "jdbc:mysql://" + this.host + "/" + this.database;
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(url, dbUsername, password);

            String ps = "SELECT tax_id FROM " + giTable + " WHERE gi_number = ?;";
            //System.out.println(this.getClass().getSimpleName() + " prepared stament:" + ps);
            taxIdPreparedStatement = connection.prepareStatement(ps);
            callLineagePreparedStatement = connection.prepareStatement("CALL lineage( ? );");

        } catch (Exception e) {
            throw new DatabaseException("unable to connect to TaxDB at this time");
        }
    }

    /**
     * Eagerly created Singleton pattern. Get the single instance of this class
     *
     * @return SequenceToolsWebDaoMySQLImpl instance
     */
    public static TaxonomyDaoMysql getInstance(String database, String host, String username, String password, String giTable) throws DatabaseException {
        if (uniqueInstance == null) {
            uniqueInstance = new TaxonomyDaoMysql(database, host, username, password, giTable);
        }
        return uniqueInstance;
    }

    /* (non-Javadoc)
	 * @see nl.bioinf.noback.taxonomy.dao.TaxonomyDao#disconnect()
     */
    public void disconnect() throws DatabaseException {
        try {
            connection.close();
            if (taxIdPreparedStatement != null) {
                taxIdPreparedStatement.close();
                taxIdPreparedStatement = null;
            }
            if (callLineagePreparedStatement != null) {
                callLineagePreparedStatement.close();
                callLineagePreparedStatement = null;
            }

        } catch (SQLException e) {
            //e.printStackTrace();
            throw new DatabaseException("closing the TaxDB connection encountered an Exception: " + e.getMessage());
        }
    }

    /* (non-Javadoc)
	 * @see nl.bioinf.noback.taxonomy.dao.TaxonomyDao#getLineage(int)
     */
    public Lineage getLineage(int taxID) throws DatabaseException {

        try {
            if (lineageCache.containsKey(taxID)) {
                cachedReturns++;
//				if( cachedReturns % 1000 == 0 ){
//					System.out.println( this.getClass().getSimpleName() + " returned " + cachedReturns + " cached lineages"  );
//				}
                return lineageCache.get(taxID);
            } else {
                //System.out.println(  this.getClass().getSimpleName() + "retrieving lineage from TaxDB for tax_id=" + taxID );
                callLineagePreparedStatement.setInt(1, taxID);
                ResultSet lrs = callLineagePreparedStatement.executeQuery();
                //ResultSet lrs = ls.getResultSet ();

                //System.out.println( this.getClass().getSimpleName() +  "retrieved lineage from TaxDB for tax_id=" + taxID );
                Lineage lineage = processTaxDbLineage(lrs, taxID);

                //System.out.println( "processed lineage from TaxDB for tax_id=" + taxID + " end node=" + lineage.getExternalNode());
                callLineagePreparedStatement.clearParameters();

                if (cachedLineages < cacheSize) {
                    //System.out.println( "caching lineage for tax_id=" + taxID );
                    lineageCache.put(lineage.getExternalNode().getTaxID(), lineage);
                    cachedLineages++;

//					if( cachedLineages % 20 == 0 ){
//						System.out.println( this.getClass().getSimpleName() + " lineage cache size=" + cachedLineages );
//					}
                } else if (cachedLineages == cacheSize) {
                    System.out.println("[" + this.getClass().getSimpleName() + "] exceeding cache size for stored lineages!"
                            + "\nrevertnig to MySQL querying all requests");
                }
                return lineage;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DatabaseException("unable to obtain lineage for " + taxID + " from TaxDB at this time");
        }

    }

    /* (non-Javadoc)
	 * @see nl.bioinf.noback.taxonomy.dao.TaxonomyDao#getGiLineage(int)
     */
    public Lineage getGiLineage(int giNumber) throws DatabaseException {
        //System.out.println(this.getClass().getSimpleName() + ".getGiLineage(): gi=" + giNumber );

        Lineage lineage = null;
        try {
            //ls = connection.createStatement();
            taxIdPreparedStatement.setInt(1, giNumber);
            ResultSet tidrs = taxIdPreparedStatement.executeQuery();
            if (!tidrs.next()) {
                throw new DatabaseException("gi number does not exist");
            } else {

                int taxID = tidrs.getInt("tax_id");

                //System.out.println(this.getClass().getSimpleName() + ".getGiLineage(): tax_id=" + taxID );
                lineage = getLineage(taxID);

//				if( lineage == null ){
//					System.out.println(this.getClass().getSimpleName() + ".getGiLineage(): null lineage" );
//				}
//				else{
//					System.out.println(this.getClass().getSimpleName() + ".getGiLineage(): lineage=" + lineage );
//				}
//				System.exit(0);
            }

            //ls = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
            //ls.setFetchSize(50); 
        } catch (Exception e) {
            //System.out.println( e.getMessage() );
            //e.printStackTrace();
            throw new DatabaseException("unable to obtain lineage for " + giNumber + " from TaxDB at this time. Extra info: " + e.getMessage());
        }

        return lineage;
    }

    @Override
    public Lineage getLineage(String organismName) throws DatabaseException {
        throw new DatabaseException("this method has not been implemented yet!");
    }

    /*	public Lineage getGiLineage( int giNumber ) throws DatabaseException {
			Statement ls;
			Lineage lineage;
			
			try {
				//ls = connection.createStatement();
				
				ls = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
				ls.setFetchSize(50); 
				
				ls.executeQuery("CALL gi_lineage(" + giNumber + ");");
				
				ResultSet lrs = ls.getResultSet ();
				
				lineage = processTaxDbLineage( lrs, giNumber );
				System.out.println( lineage );
				System.out.println( "tax_id of outer lineage node: " + lineage.getExternalNode().getTaxID() );
				
				ls.close();
				
			} catch (Exception e) {
				//System.out.println( e.getMessage() );
				//e.printStackTrace();
	        	throw new DatabaseException("unable to obtain lineage for " + giNumber + " from TaxDB at this time. Extra info: " + e.getMessage() );
			}
			
			return lineage;
	}
     */
    private Lineage processTaxDbLineage(ResultSet lrs, int taxId) throws DatabaseException {
        Lineage lineage = new Lineage();
        try {
            while (lrs.next()) {
                /*tax_id, parent_tax_id, rank, division_id, inherited_div, scientific_name*/
                TaxNode tn = new TaxNode();
                if (lrs.getInt("tax_id") == 0) {
                    throw new DatabaseException("empty lineage for tax_id " + taxId);
                }

                tn.setTaxID(lrs.getInt("tax_id"));
                tn.setParentTaxID(lrs.getInt("parent_tax_id"));

                //System.out.println( "ptax_id=" + lrs.getInt("parent_tax_id") + " tax_id=" + lrs.getInt("tax_id") + " rank=" + lrs.getString("rank") + " TaxonomyRank=" + TaxonomyRank.getTaxonomyRank( lrs.getString("rank") ) );
                tn.setRank(TaxonomyRank.getTaxonomyRank(lrs.getString("rank")));
                //tn.setGbDivision( GenBankDivision. lrs.getString("") )
                //tn.setInheritedDiv( lrs.getBoolean("inherited_div") );
                tn.setScientificName(lrs.getString("scientific_name"));

                lineage.addNode(tn);
            }

        } catch (SQLException e) {
            //System.out.println( e.getMessage() );
            //e.printStackTrace();
            throw new DatabaseException("unable to obtain lineage for tax_id" + taxId + " from TaxDB at this time");
        } catch (CorruptedLineageException e) {
            //e.printStackTrace();
            throw new DatabaseException("unable to obtain lineage for tax_id " + taxId + " from TaxDB; a lineage data corruption occurred");
        }
        return lineage;
    }

}
