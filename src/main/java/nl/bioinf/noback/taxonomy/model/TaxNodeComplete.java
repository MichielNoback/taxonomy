package nl.bioinf.noback.taxonomy.model;

//import net.cellingo.sequence_tools.annotation.GenBankDivision;

public class TaxNodeComplete extends TaxNode {
	/*locus-name prefix; not unique*/
	private String emblCode;
	/*GenBank division (PRI, MAM, ENV, ...)*/
	private String gbDivision;
	/*if node inherits division from parent*/
	private boolean inheritedDiv;
	/*the genetic code number; maps directly to constructor of net.cellingo.sequence_tools.gene_analysis.CodonTable*/
	private int codonTable;
	/*if node inherits genetic code from parent*/
	private boolean inheritedCodonTable;
	/*the genetic code number for the mitochondria; maps directly to constructor of net.cellingo.sequence_tools.gene_analysis.CodonTable*/
	private int mtCodonTable;
	/*if node inherits mitochondrial genetic code from parent*/
	private boolean inheritedMtCodonTable;
	/*if name is suppressed in GenBank entry lineage*/
	private boolean genBankHidden;
	/*if this subtree has no sequence data yet*/
	private boolean hiddenSubtree;
	/*node comments*/
	private String comments;

	
	
	
	/**
	 * 
	 */
	public TaxNodeComplete() {
		super();
	}

	/**
	 * @param taxId
	 * @param parentTaxId
	 */
	public TaxNodeComplete(int taxId, int parentTaxId) {
		super(taxId, parentTaxId);
	}

	/**
	 * @param taxId
	 */
	public TaxNodeComplete(int taxId) {
		super(taxId);
	}

	/**
	 * returns the EMBL prefix code
	 * @return the emblCode
	 */
	public String getEmblCode() {
		return emblCode;
	}

	/**
	 * sets the EMBL prefix code
	 * @param emblCode the emblCode to set
	 */
	public void setEmblCode(String emblCode) {
		this.emblCode = emblCode;
	}

	/**
	 * returns the GenBank division (PRI, MAM, ENV, ...)
	 * @return the gbDivision
	 */
	public String getGbDivision() {
		return gbDivision;
	}

	/**
	 * sets the GenBank division (PRI, MAM, ENV, ...)
	 * @param gbDivision the gbDivision to set
	 */
	public void setGbDivision(String gbDivision) {
		this.gbDivision = gbDivision;
	}

	/**
	 * returns if node inherits division from parent
	 * @return the inheritedDiv
	 */
	public boolean isInheritedDiv() {
		return inheritedDiv;
	}

	/**
	 * sets if node inherits division from parent
	 * @param inheritedDiv the inheritedDiv to set
	 */
	public void setInheritedDiv(boolean inheritedDiv) {
		this.inheritedDiv = inheritedDiv;
	}

	/**
	 * returns the genetic code number; maps directly to constructor of 
	 * net.cellingo.sequence_tools.gene_analysis.CodonTable
	 * @return the codonTable
	 */
	public int getCodonTable() {
		return codonTable;
	}

	/**
	 * sets the genetic code number; maps directly to constructor of 
	 * net.cellingo.sequence_tools.gene_analysis.CodonTable
	 * @param codonTable the codonTable to set
	 */
	public void setCodonTable(int codonTable) {
		this.codonTable = codonTable;
	}

	/**
	 * returns if node inherits genetic code from parent
	 * @return the inheritedCodonTable
	 */
	public boolean isInheritedCodonTable() {
		return inheritedCodonTable;
	}

	/**
	 * sets if node inherits genetic code from parent
	 * @param inheritedCodonTable the inheritedCodonTable to set
	 */
	public void setInheritedCodonTable(boolean inheritedCodonTable) {
		this.inheritedCodonTable = inheritedCodonTable;
	}

	/**
	 * returns the genetic code number for the mitochondria; maps 
	 * directly to constructor of net.cellingo.sequence_tools.gene_analysis.CodonTable
	 * @return the mtCodonTable
	 */
	public int getMtCodonTable() {
		return mtCodonTable;
	}

	/**
	 * sets the genetic code number for the mitochondria; maps 
	 * directly to constructor of net.cellingo.sequence_tools.gene_analysis.CodonTable
	 * @param mtCodonTable the mtCodonTable to set
	 */
	public void setMtCodonTable(int mtCodonTable) {
		this.mtCodonTable = mtCodonTable;
	}

	/**
	 * returns if node inherits mitochondrial genetic code from parent
	 * @return the inheritedMtCodonTable
	 */
	public boolean isInheritedMtCodonTable() {
		return inheritedMtCodonTable;
	}

	/**
	 * sets if node inherits mitochondrial genetic code from parent
	 * @param inheritedMtCodonTable the inheritedMtCodonTable to set
	 */
	public void setInheritedMtCodonTable(boolean inheritedMtCodonTable) {
		this.inheritedMtCodonTable = inheritedMtCodonTable;
	}

	/**
	 * returns if name is suppressed in GenBank entry lineage
	 * @return the genBankHidden
	 */
	public boolean isGenBankHidden() {
		return genBankHidden;
	}

	/**
	 * sets if name is suppressed in GenBank entry lineage
	 * @param genBankHidden the genBankHidden to set
	 */
	public void setGenBankHidden(boolean genBankHidden) {
		this.genBankHidden = genBankHidden;
	}

	/**
	 * returns if this subtree has no sequence data yet
	 * @return the hiddenSubtree
	 */
	public boolean isHiddenSubtree() {
		return hiddenSubtree;
	}

	/**
	 * sets if this subtree has no sequence data yet
	 * @param hiddenSubtree the hiddenSubtree to set
	 */
	public void setHiddenSubtree(boolean hiddenSubtree) {
		this.hiddenSubtree = hiddenSubtree;
	}

	/**
	 * returns the comments on this node
	 * @return the comments
	 */
	public String getComments() {
		return comments;
	}

	/**
	 * sets the comments on this node
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

	public TaxNode clone(){
		TaxNodeComplete cloned = new TaxNodeComplete();
		cloned.setCodonTable( this.codonTable );
		cloned.setComments( this.comments );
		cloned.setCumulativeChildNumber( getCumulativeChildNumber() );
		cloned.setEmblCode(emblCode);
		cloned.setGbDivision(gbDivision);
		cloned.setGenBankHidden(genBankHidden);
		cloned.setHiddenSubtree(hiddenSubtree);
		cloned.setInheritedCodonTable(inheritedCodonTable);
		cloned.setInheritedDiv(inheritedDiv);
		cloned.setInheritedMtCodonTable(inheritedMtCodonTable);
		cloned.setMtCodonTable(mtCodonTable);
		try {
			if( getParentNode()!= null ) cloned.setParentNode( getParentNode() );
		} catch (CorruptedLineageException e) {	}
		cloned.setParentTaxID(getParentTaxID() );
		cloned.setRank(getRank());
		cloned.setScientificName( getScientificName());
		cloned.setTaxID(getTaxID());
		
		return cloned;
	}

	
}
