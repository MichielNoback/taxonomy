/**
 * 
 */
package nl.bioinf.noback.taxonomy.tax_composition;

import java.io.File;

/**
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.1
  */
public class TaxonomyCompositionAnalyserSettings {
	
	private File inputFile;
	private boolean inputHasTaxIdField;
	private File reportFile; 
	private File treeFile;
	
	private String dbType;
	private String dbName;
	private String dbUser;
	private String dbHost;
	private String dbPassword;
	private String dbGiTable;
	private File nodesFile;
	private File giNumbersFile;

	/**
	 * sets the input file to read from
	 * @param inputFile
	 */
	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}

	/**
	 * returns the input file
	 * @return inputFile
	 */
	public File getInputFile(){
		return inputFile;
	}

	/**
	 * @param inputHasTaxIdField the inputHasTaxIdField to set
	 */
	public void setInputHasTaxIdField(boolean inputHasTaxIdField) {
		this.inputHasTaxIdField = inputHasTaxIdField;
	}

	/**
	 * @return the inputHasTaxIdField
	 */
	public boolean isInputHasTaxIdField() {
		return inputHasTaxIdField;
	}

	/**
	 * sets the report file
	 * @param reportFile
	 */
	public void setReportFile(File reportFile) {
		this.reportFile = reportFile;
	}

	/**
	 * returns the report file
	 * @return reportFile
	 */
	public File getReportFile(){
		return reportFile;
	}
	
	/**
	 * sets the tree file
	 * @param treeFile
	 */
	public void setTreeFile(File treeFile) {
		this.treeFile = treeFile;
	}

	/**
	 * returns the tree file
	 * @return treeFile
	 */
	public File getTreeFile(){
		return treeFile;
	}

	/**
	 * @return the dbType
	 */
	public String getDbType() {
		return dbType;
	}

	/**
	 * @param dbType the dbType to set
	 */
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	/**
	 * @return the dbName
	 */
	public String getDbName() {
		return dbName;
	}

	/**
	 * @param dbName the dbName to set
	 */
	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	/**
	 * @param dbGiTable the dbGiTable to set
	 */
	public void setDbGiTable(String dbGiTable) {
		this.dbGiTable = dbGiTable;
	}

	/**
	 * @return the dbGiTable
	 */
	public String getDbGiTable() {
		return dbGiTable;
	}

	/**
	 * @return the dbUser
	 */
	public String getDbUser() {
		return dbUser;
	}

	/**
	 * @param dbUser the dbUser to set
	 */
	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	/**
	 * @return the dbHost
	 */
	public String getDbHost() {
		return dbHost;
	}

	/**
	 * @param dbHost the dbHost to set
	 */
	public void setDbHost(String dbHost) {
		this.dbHost = dbHost;
	}

	/**
	 * @return the dbPassword
	 */
	public String getDbPassword() {
		return dbPassword;
	}

	/**
	 * @param dbPassword the dbPassword to set
	 */
	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	/**
	 * for file-based TaxDB, set the nodes file
	 * @param nodesFile
	 */
	public void setNodesFile(File nodesFile) {
		this.nodesFile = nodesFile;
	}

	/**
	 * for file-based TaxDB, set the gi_numbers file
	 * @param giNumbersFile
	 */
	public void setGiNumbersFile(File giNumbersFile) {
		this.giNumbersFile = giNumbersFile;
	}

	/**
	 * @return the nodesFile
	 */
	public File getNodesFile() {
		return nodesFile;
	}

	/**
	 * @return the giNumbersFile
	 */
	public File getGiNumbersFile() {
		return giNumbersFile;
	}
	

	
}
