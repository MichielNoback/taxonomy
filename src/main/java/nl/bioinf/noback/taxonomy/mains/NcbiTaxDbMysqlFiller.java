/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.bioinf.noback.taxonomy.mains;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import nl.bioinf.noback.taxonomy.dao.TaxonomyDao;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author michiel
 */
public final class NcbiTaxDbMysqlFiller {

    /*NCBI TaxDB*/
    public static final String DATABASE_NAME = "database.database_name";
    public static final String DATABASE_USER = "database.user";
    public static final String DATABASE_HOST = "database.host";
    public static final String DATABASE_PASSWORD = "database.password";

    /**
     * The configuration settings for the database connection.
     */
    private XMLConfiguration configuration;
    /**
     * the object used to query the taxonomy database TaxDB.
     */
    private TaxonomyDao ncbiTaxDB;
    private Connection connection;
    private PreparedStatement deletedNodesStmnt;
    private PreparedStatement divisionsStmnt;
    private PreparedStatement nodesStmnt;
    private PreparedStatement namesStmnt;
    private PreparedStatement mergedNodesStmnt;
    private ZipFile archive;


    /*TaxDBlite*/
    /**
     * private constructor.
     */
    private NcbiTaxDbMysqlFiller() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        if (args.length != 1) {
            System.out.println("no configuration file provided!");
            System.out.println("usage: java -jar NcbiTaxDbMysqlFiller.jar <configuration file>");
            System.out.println("aborting");
            System.exit(0);
        }

        NcbiTaxDbMysqlFiller ntbFiller = new NcbiTaxDbMysqlFiller();
        ntbFiller.start(args[0]);
    }

    private void start(String configFile) {
        try {
            loadConfiguration(configFile);
            connectNCBITaxDB();

            File zipFile = new File(configuration.getString("zipfile_location"));
            this.archive = new ZipFile(zipFile);

            processDivisions();
            processDeletedNodes();
            processNodes();
            processNames();
            processMergedNodes();
            
            disconnectNCBITaxDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * processes division.dmp.
     */
    private void processDivisions() {
        System.out.print("Processing divisions...");
        int lineCount = 0;
        try {
            ZipEntry nodesFile = archive.getEntry("division.dmp");
            InputStream is = archive.getInputStream(nodesFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                lineCount++;
                String[] elements = line.split("\\t+\\|\\t+");
                int id = Integer.parseInt(elements[0]);
                String code = elements[1];
                String name = elements[2];
                String comments = elements[3];
                comments = comments.length() < 2 ? null : comments;
                divisionsStmnt.setInt(1, id);
                divisionsStmnt.setString(2, code);
                divisionsStmnt.setString(3, name);
                divisionsStmnt.setString(4, comments);
                divisionsStmnt.executeUpdate();
            }
        } catch (IOException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, "error at line " + lineCount, ex);
        } catch (SQLException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, "error at line " + lineCount, ex);
        }
        System.out.println(lineCount + " divisions processed");
    }

    /**
     * processes delnodes.dmp.
     */
    private void processDeletedNodes() {
        System.out.print("Processing deleted nodes...");
        int lineCount = 0;
        try {
            ZipEntry nodesFile = archive.getEntry("delnodes.dmp");
            InputStream is = archive.getInputStream(nodesFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                lineCount++;
                String[] elements = line.split("\\t?\\|\\t?");
                int id = Integer.parseInt(elements[0]);
                deletedNodesStmnt.setInt(1, id);
                deletedNodesStmnt.executeUpdate();
                //if (lineCount == 10) break;
            }
        } catch (IOException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, "error at line " + lineCount, ex);
        } catch (SQLException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, "error at line " + lineCount, ex);
        }
        System.out.println(lineCount + " deleted nodes processed");
    }

    /**
     * processes nodes.dmp.
     */
    private void processNodes() {
        System.out.print("Processing nodes...");
        int lineCount = 0;
        try {
            ZipEntry nodesFile = archive.getEntry("nodes.dmp");
            InputStream is = archive.getInputStream(nodesFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                lineCount++;
                String[] elements = line.split("\\t\\|\\t");
                int id = Integer.parseInt(elements[0]);
                int parentId = Integer.parseInt(elements[1]);
                String rank = elements[2];
                String divisionCode = elements[3];
                int inheritedDiv = Integer.parseInt(elements[4]);
                int genCodeId = Integer.parseInt(elements[5]);

                nodesStmnt.setInt(1, id);
                nodesStmnt.setInt(2, parentId);
                nodesStmnt.setString(3, rank);
                nodesStmnt.setString(4, divisionCode);
                nodesStmnt.setInt(5, inheritedDiv);
                nodesStmnt.setInt(6, genCodeId);

                nodesStmnt.executeUpdate();
            }
        } catch (IOException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, "error at line " + lineCount, ex);
        } catch (SQLException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, "error at line " + lineCount, ex);
        }
        System.out.println(lineCount + " nodes processed");
    }

    /**
     * processes nodes.dmp.
     */
    private void processNames() {
        //Set<String> nameClasses = new TreeSet<String>();
        System.out.print("Processing names...");
        int lineCount = 0;
        try {
            ZipEntry nodesFile = archive.getEntry("names.dmp");
            InputStream is = archive.getInputStream(nodesFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                lineCount++;
                String[] elements = line.split("\\t\\|\\t");
                int id = Integer.parseInt(elements[0]);
                String name = elements[1];
                String uniqueName = elements[2];
                if (uniqueName.length() > 2) {
                    namesStmnt.setString(3, uniqueName);
                } else {
                    namesStmnt.setNull(3, java.sql.Types.VARCHAR);
                }

                String nameClass = elements[3];
                nameClass = nameClass.substring(0, nameClass.length() - 2);
//                nameClasses.add(nameClass);
                namesStmnt.setInt(1, id);
                namesStmnt.setString(2, name);
                namesStmnt.setString(4, nameClass);

                namesStmnt.executeUpdate();
            }
        } catch (IOException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, "error at line " + lineCount, ex);
        } catch (SQLException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, "error at line " + lineCount, ex);
        }
        System.out.println(lineCount + " names processed");
        //System.out.println("nameClasses = " + nameClasses);
    }

    /**
     * processes merged.dmp.
     */
    private void processMergedNodes() {
        //Set<String> nameClasses = new TreeSet<String>();
        System.out.print("Processing merged nodes...");
        int lineCount = 0;
        try {
            ZipEntry nodesFile = archive.getEntry("merged.dmp");
            InputStream is = archive.getInputStream(nodesFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                lineCount++;
                String[] elements = line.split("\\t\\|\\t");
                int oldId = Integer.parseInt(elements[0]);
                int newId = Integer.parseInt(elements[1].substring(0, elements[1].length() - 2));
                mergedNodesStmnt.setInt(1, oldId);
                mergedNodesStmnt.setInt(2, newId);

                mergedNodesStmnt.executeUpdate();
                if(lineCount == 10) break;
            }
        } catch (IOException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, "error at line " + lineCount, ex);
        } catch (SQLException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, "error at line " + lineCount, ex);
        }
        System.out.println(lineCount + " merged nodes processed");
    }

    /**
     * establish a connection to the Taxonomy database (local NCBI mirror).
     */
    private void connectNCBITaxDB() {
        /*connect to TaxDB*/
        try {
            String url = "jdbc:mysql://" + configuration.getString(DATABASE_HOST)
                    + "/" + configuration.getString(DATABASE_NAME);
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(
                    url,
                    configuration.getString(DATABASE_USER),
                    configuration.getString(DATABASE_PASSWORD));

            String query = "INSERT INTO deleted_nodes (tax_id) VALUES (?);";
            this.deletedNodesStmnt = connection.prepareStatement(query);

            query = "INSERT INTO divisions (division_id, division_code, division_name, comments) VALUES (?, ?, ?, ?);";
            this.divisionsStmnt = connection.prepareStatement(query);

            query = "INSERT INTO nodes (tax_id, parent_tax_id, rank, division_code, inherited_div, genetic_code_id)"
                    + " VALUES (?, ?, ?, ?, ?, ?);";
            this.nodesStmnt = connection.prepareStatement(query);

            query = "INSERT INTO names (tax_id, name, unique_name, name_class)"
                    + " VALUES (?, ?, ?, ?);";
            this.namesStmnt = connection.prepareStatement(query);

            query = "INSERT INTO merged_nodes (old_tax_id, new_tax_id)"
                    + " VALUES (?, ?);";
            this.mergedNodesStmnt = connection.prepareStatement(query);

        } catch (SQLException e) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, null, e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * closes the connection to DB.
     */
    private void disconnectNCBITaxDB() {
        try {
            deletedNodesStmnt.close();
            divisionsStmnt.close();
            nodesStmnt.close();
            namesStmnt.close();
            mergedNodesStmnt.close();
            connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(NcbiTaxDbMysqlFiller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * The loadConfiguration method loads the configuration for the application from file. If this configuration cannot
     * be loaded, a ConfigurationException will be thrown. It will also start the logger if it has yet been started
     *
     * @param configFile the configFile
     * @throws Exception e
     */
    private void loadConfiguration(final String configFile) throws Exception {
        try {
            File xmlConfigFile = new File(configFile);
            if (!xmlConfigFile.exists() || !xmlConfigFile.canRead()) {
                System.out.println("unable to read configuration file: " + configFile);
                throw new Exception("[NcbiTaxDbMysqlFiller.loadConfiguration()] unable to read configuration file: "
                        + configFile);
            }
            configuration = new XMLConfiguration(xmlConfigFile);
        } catch (Exception e) { // configuration could not be loaded
            throw new Exception("[NcbiTaxDbMysqlFiller.loadConfiguration()] unable to load configuration file: "
                    + configFile + "; cause of exception:" + e.getCause());
        }
    }

}
