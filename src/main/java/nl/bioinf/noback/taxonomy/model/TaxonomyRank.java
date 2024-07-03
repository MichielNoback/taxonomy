/**
 * 
 */
package nl.bioinf.noback.taxonomy.model;

import java.util.HashMap;
import java.util.HashSet;

/**
 * This enum represents the different ranks in a taxonomy lineage.
 * It can be used to compare the levels of two TaxNode objects.
 * @author Michiel Noback (www.cellingo.net, michiel@cellingo.net)
 * @version 1.0
 */
public enum TaxonomyRank {
	
	SUPERKINGDOM("superkingdom"),
	KINGDOM("kingdom"),
	SUBKINGDOM("subkingdom"),
	SUPERPHYLUM("superphylum"),
	PHYLUM("phylum"),
	SUBPHYLUM("subphylum"),
	SUPERCLASS("superclass"),
	CLASS("class"),
	SUBCLASS("subclass"),
	INFRACLASS("infraclass"),
	SUPERORDER("superorder"),
	ORDER("order"),
	SUBORDER("suborder"),
	INFRAORDER("infraorder"),
	PARVORDER("parvorder"),
	SUPERFAMILY("superfamily"),
	FAMILY("family"),
	SUBFAMILY("subfamily"),
	TRIBE("tribe"),
	SUBTRIBE("subtribe"),
	GENUS("genus"),
	SUBGENUS("subgenus"),
	SPECIES_GROUP("species group"),
	SPECIES_SUBGROUP("species subgroup"),
	SPECIES("species"),
	VARIETAS("varietas"),
	FORMA("forma"),
	SUBSPECIES("subspecies"),
	
	NO_RANK("no rank");
	
	/*set containing the members that should be included in a short lineage representation*/
	private static HashSet<TaxonomyRank> shortLineage;
	/*map that holds the rank values for comparing lineage nodes */
	private static HashMap<TaxonomyRank, Integer> rankValues;
	/*map that can be used to get the enum based n regular string representation*/
	private static HashMap<String, TaxonomyRank> rankNCBIstrings;
	private String type;
	
	static{
		rankNCBIstrings = new HashMap<String, TaxonomyRank>();
		rankNCBIstrings.put( "no rank", NO_RANK );
		rankNCBIstrings.put( "subspecies", SUBSPECIES );
		rankNCBIstrings.put( "forma", FORMA );
		rankNCBIstrings.put( "varietas", VARIETAS );
		rankNCBIstrings.put( "species", SPECIES );
		rankNCBIstrings.put( "species subgroup", SPECIES_SUBGROUP );
		rankNCBIstrings.put( "species group", SPECIES_GROUP );
		rankNCBIstrings.put( "subgenus", SUBGENUS );
		rankNCBIstrings.put( "genus", GENUS );
		rankNCBIstrings.put( "subtribe", SUBTRIBE );
		rankNCBIstrings.put( "tribe", TRIBE );
		rankNCBIstrings.put( "subfamily", SUBFAMILY );
		rankNCBIstrings.put( "family", FAMILY );
		rankNCBIstrings.put( "superfamily", SUPERFAMILY );
		rankNCBIstrings.put( "parvorder", PARVORDER );
		rankNCBIstrings.put( "infraorder", INFRAORDER );
		rankNCBIstrings.put( "suborder",SUBORDER );
		rankNCBIstrings.put( "order", ORDER );
		rankNCBIstrings.put( "superorder", SUPERORDER );
		rankNCBIstrings.put( "infraclass", INFRACLASS );
		rankNCBIstrings.put( "subclass", SUBCLASS );
		rankNCBIstrings.put( "class", CLASS );
		rankNCBIstrings.put( "superclass", SUPERCLASS );
		rankNCBIstrings.put( "subphylum", SUBPHYLUM );
		rankNCBIstrings.put( "phylum", PHYLUM );
		rankNCBIstrings.put( "superphylum", SUPERPHYLUM );
		rankNCBIstrings.put( "subkingdom", SUBKINGDOM );
		rankNCBIstrings.put( "kingdom", KINGDOM );
		rankNCBIstrings.put( "superkingdom", SUPERKINGDOM );

		rankValues = new HashMap<TaxonomyRank, Integer>();
		rankValues.put( NO_RANK, 0 );
		rankValues.put( SUBSPECIES, 1 );
		rankValues.put( FORMA, 2 );
		rankValues.put( VARIETAS, 3 );
		rankValues.put( SPECIES, 4 );
		rankValues.put( SPECIES_SUBGROUP, 5 );
		rankValues.put( SPECIES_GROUP, 6 );
		rankValues.put( SUBGENUS, 7 );
		rankValues.put( GENUS, 8 );
		rankValues.put( SUBTRIBE, 9 );
		rankValues.put( TRIBE, 10 );
		rankValues.put( SUBFAMILY, 11 );
		rankValues.put( FAMILY, 12 );
		rankValues.put( SUPERFAMILY, 13 );
		rankValues.put( PARVORDER, 14 );
		rankValues.put( INFRAORDER, 15 );
		rankValues.put( SUBORDER, 16 );
		rankValues.put( ORDER, 17 );
		rankValues.put( SUPERORDER, 18 );
		rankValues.put( INFRACLASS, 19 );
		rankValues.put( SUBCLASS, 20 );
		rankValues.put( CLASS, 21 );
		rankValues.put( SUPERCLASS, 22 );
		rankValues.put( SUBPHYLUM, 23 );
		rankValues.put( PHYLUM, 24 );
		rankValues.put( SUPERPHYLUM, 25 );
		rankValues.put( SUBKINGDOM, 26 );
		rankValues.put( KINGDOM, 27 );
		rankValues.put( SUPERKINGDOM, 28 );

		shortLineage = new HashSet<TaxonomyRank>();
		shortLineage.add( SUPERKINGDOM );
		shortLineage.add( KINGDOM );
		shortLineage.add( PHYLUM );
		shortLineage.add( SUBPHYLUM );
		shortLineage.add( CLASS );
		shortLineage.add( SUPERORDER );
		shortLineage.add( ORDER );
		shortLineage.add( SUBORDER );
		shortLineage.add( PARVORDER );
		shortLineage.add( FAMILY );
		shortLineage.add( GENUS );
		shortLineage.add( SPECIES );
		
	}
	
	private TaxonomyRank(String type){
		this.type = type;
	}
	
	/**
	 * get the rank value of a TaxonomyRank enumeration. For sorting purposes
	 * @param taxRank
	 * @return rank value
	 */
	public static int getRankValue( TaxonomyRank taxRank ){
		return rankValues.get(taxRank);
	}
	
	/**
	 * get the rank value of a this enumeration. For sorting purposes
	 * @return rank value
	 */
	public int getRankValue(){
		return rankValues.get( this );
	}

	/**
	 * get the enum based on the NCBI string representation
	 * @param stringRepr
	 * @return taxonomyRank
	 */
	public static TaxonomyRank getTaxonomyRank( String stringRepr ){
		if(rankNCBIstrings.containsKey(stringRepr)){
			return rankNCBIstrings.get(stringRepr);
		}
		return null;
	}
	
	/**
	 * returns the presence of the taxRank in the short lineage set
	 * @param taxRank
	 * @return taxRank is present in short lineage
	 */
	public static boolean isShortLineage( TaxonomyRank taxRank ){
		return shortLineage.contains(taxRank);
	}
	
	/**
	 * returns the presence of the taxRank in the short lineage set
	 * @return taxRank is present in short lineage
	 */
	public boolean isShortLineage( ){
		return shortLineage.contains( this );
	}

	public String toString(){
		return type;
	}
}
