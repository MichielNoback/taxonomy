/**
 * 
 */
package nl.bioinf.noback.taxonomy.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * This class encapsulates the properties and methods of a Taxonomy Lineage
 * @author MA Noback (m.a.noback@pl.hanze.nl)
 * @version 0.1
 */
public class Lineage {
	
	/*the actual lineage nodes*/
	private List<TaxNode> lineage;
	
	/**
	 * default constructor
	 */
	public Lineage(){
		lineage = new ArrayList<TaxNode>();
	}
	
	/**
	 * constructs with a linked list of TaxNodes
	 */
	public Lineage( List<TaxNode> lineageList ) throws CorruptedLineageException{
		lineage = lineageList;
		checkLineage();
	}
	
	/**
	 * checks the lineage list and throws CorruptedLineageException when it is not OK
	 * @throws CorruptedLineageException
	 */
	private void checkLineage()throws CorruptedLineageException{
		int taxIdCheck = 0;
		for( TaxNode node : lineage ){
			if( node.getTaxID() == 1 ) return; //root reached: all is well
			else if( node.getParentNode() == null ) throw new CorruptedLineageException("node has no parent: " + node);
			else{
				if( taxIdCheck != 0 && taxIdCheck != node.getTaxID() ){
					throw new CorruptedLineageException("parent of node is not correct\n\tnode: " + node + "\n\tparent: " + taxIdCheck);
				}
			}
			taxIdCheck = node.getParentTaxID();
		}
	}
	
	
	/**
	 * adds a node that is parent to a previously added node,
	 * or the outer node (leaf) of the lineage.
	 * @param taxNode
	 * @throws CorruptedLineageException
	 */
	public void addNode( TaxNode taxNode ) throws CorruptedLineageException {
		if( lineage.size() == 0 ){//first node
			lineage.add( taxNode );
		}
		else{
			if( taxNode.isParent( lineage.get( lineage.size()-1 ) ) ){
				lineage.get( lineage.size()-1 ).setParentNode( taxNode );
				lineage.add( taxNode );
			}
			else{
				throw new CorruptedLineageException( "added node " + taxNode.getTaxID()
						+ " is not parent to previously added node; taxID=" + lineage.get( lineage.size()-1 ).getTaxID() 
						+ " parentTaxID=" + lineage.get( lineage.size()-1 ).getParentTaxID() );
			}
		}
	}

	/**
	 * returns the root of this lineage
	 * @return root
	 */
	public TaxNode getRoot() {
		if( lineage.size() == 0 ) return null;
		return lineage.get( lineage.size() - 1 );
	}

	/**
	 * returns the outermost node of the lineage 
	 * @return external node
	 */
	public TaxNode getExternalNode(){
		return lineage.get( 0 );
	}
	
	/**
	 * returns an iterator of this lineage from the root up.
	 * @return root-up iterator 
	 */
	public List<TaxNode> getListFromRoot(){
		ArrayList<TaxNode> reversed = new ArrayList<TaxNode>();
		int size = lineage.size();
		for( int i=0; i<size; i++ ){
			reversed.add( lineage.get( size-(i+1) ) ); 
		}
		return reversed;
	}
	
	/**
	 * returns an iterator that traverses the lineage from the root up
	 * @return
	 */
	public Iterator<TaxNode> iterateFromRoot(){
		return new ReverseLineageIterator<TaxNode>(); 
	}
	
	/**
	 * returns an iterator of this lineage from the outer leaf down.
	 * @return leaf-down iterator 
	 */
	public List<TaxNode> getListFromLeaf(){
		return lineage;
	}
	
	/**
	 * returns the intersection of two lineages: the common TaxNodes from the root up.
	 * @param otherLineage
	 * @return intersection
	 * @throws CorruptedLineageException 
	 */
	public Lineage getIntersection( Lineage otherLineage ) throws CorruptedLineageException{
		/*get intersection of the two lineages*/
		Lineage intersection = new Lineage();
		List<TaxNode> otherList = otherLineage.getListFromRoot();
		int smaller = (this.getLength() <= otherLineage.getLength() ? this.getLength() : otherLineage.getLength());
		int intersectLevel = 0;
		for( int i=0; i<smaller; i++){
			//System.out.println( "checking level: " + i + "; " + lineage.get( getLength()-(i+1) ) );
			//System.out.println( "checking level: " + i + "; " + otherList.get( i ) );
			if( ! lineage.get( getLength()-(i+1) ).equals( otherList.get(i) ) ){
				//System.out.println( "equal up to level: " + i + "; " + lineage.get( getLength()-(i+1) ) );
				intersectLevel = i;
				break;
			}
			else if( i == smaller-1 ){
				//System.out.println( "end of shortest lineage reached at level: " + i );
				intersectLevel = i+1;
			}
		}
		//System.out.println( "lineages diverge at level: " + intersectLevel + "; " + lineage.get( getLength()-(intersectLevel + 1) ) + "; " + otherList.get(intersectLevel) );
		for( int j=intersectLevel; j>0; j-- ){
			intersection.addNode( lineage.get( getLength()-j) );
		}
		return intersection;
	}
	
	/**
	 * return the highest node of commonality between two Lineages
	 * @param otherLineage
	 * @return highest common node
	 */
	public TaxNode getDivergencePoint( Lineage otherLineage ){
		List<TaxNode> otherList = otherLineage.getListFromRoot();
		int smaller = (this.getLength() <= otherLineage.getLength() ? this.getLength() : otherLineage.getLength());
		for( int i=0; i<smaller; i++){
			if( lineage.get( getLength()-(i+1) ).equals( otherList.get(i) ) ){
				//System.out.println( "equal at level: " + i + "; " + lineage.get( getLength()-(i+1) ) );
			}
			else{
				return lineage.get( getLength()-(i) );
			}
		}
		return null;
	}
	
	/**
	 * Will return a truncated copy of this lineage at (but including) length nodes from the root.
	 * Thus, with a lineage of 20 nodes and this method called with truncate(10),
	 * the first 10 nodes from the root up will be retained 
	 * @param length
	 * @throws IllegalArgumentException when length exceeds the number of nodes in this lineage
	 */
	public Lineage truncate( int length ) throws IllegalArgumentException{
		Lineage trunLineage = new Lineage();
		
		int lLength = this.getLength();
		if( length > lLength ) throw new IllegalArgumentException( "length exceeds the number of nodes in this lineage" );
		
		for( int i=0; i<length; i++ ){
			try {
				trunLineage.addNode( lineage.get( lLength - (length - i) ) );
			} catch (CorruptedLineageException e) {
				/*this should never happens*/
				e.printStackTrace();
			}
		}
		return trunLineage;
	}
	
	/**
	 * returns the length of this lineage, in nodes
	 * @return lineage length
	 */
	public int getLength(){
		return lineage.size();
	}
	
	/**
	 * overrides toString()
	 */
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Lineage: [");
		for( TaxNode tn : lineage ){
			//System.out.println( tn );
			sb.append(tn.toString());
			sb.append(" ");
		}
		sb.append("] ");
		return sb.toString();
	}
	
	/**
	 * custom iterator to iterate from root up
	 * @author michiel
	 *
	 * @param <T>
	 */
	private class ReverseLineageIterator<T> implements Iterator<TaxNode>{
		private int index = 0;
		
		public ReverseLineageIterator(){
			index = getLength();
		}
		
		public boolean hasNext() {
			return index>0;
		}

		public TaxNode next() {
			index--;
			return lineage.get(index);
		}

		public void remove() {	}
		
	}

	
}
