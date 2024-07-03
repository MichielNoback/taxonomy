/**
 * 
 */
package nl.bioinf.noback.taxonomy.tax_analysis;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;


import nl.bioinf.noback.taxonomy.model.TaxonomyRank;
import nl.bioinf.noback.taxonomy.model.EmptyTreeException;
import nl.bioinf.noback.taxonomy.model.Lineage;
import nl.bioinf.noback.taxonomy.model.TaxNode;
import nl.bioinf.noback.taxonomy.model.TaxonomyExperiment;
import nl.bioinf.noback.taxonomy.tax_analysis.RankComparison.RankingCountPair;

/**
 * This implementation of TaxTreeAnalyser will take all pairwise combinations of TaxonomyExperiment
 * objects and compare the contents of the different taxonomic levels between them. All results will 
 * be appended to the given output file.
 * @author M.A. Noback (m.a.noback@pl.hanze.nl) 
 * @version 0.1
 */
public class TaxTreeLevelOverlapAnalyser extends TaxTreeAnalyser {
	/*analysis attributes as a Property mapping*/
	private Properties analysisProperties;
	private Workbook excelWb;
	private ArrayList<OverlapSummary> overlapSummaries = new ArrayList<OverlapSummary>();
	private Map<String, CellStyle> excelStyles;
	private int currentExcelRowNumber = 0;
	//private int currentExcelSheetNumber = 0;
	private Sheet currentExcelSheet = null;
	private AnalysisType analysisType;
	private NumberFormat numberFormatter;
	private TaxonomyRank firstPrimaryRankingComparatorLevel = TaxonomyRank.SPECIES;
	private TaxonomyRankingComparator firstRankingComparator;
	private TaxonomyRank secondPrimaryRankingComparatorLevel = TaxonomyRank.GENUS;
	private TaxonomyRankingComparator secondRankingComparator;
	private TaxonomyRank thirdPrimaryRankingComparatorLevel = TaxonomyRank.FAMILY;
	private TaxonomyRankingComparator thirdRankingComparator;
	private TaxonomyRank fourthPrimaryRankingComparatorLevel = TaxonomyRank.ORDER;
	private TaxonomyRankingComparator fourthRankingComparator;
	
	private TaxonomyRank secondaryRankingComparatorLevel = TaxonomyRank.ORDER;
	private TaxonomyRank secondSecondaryRankingComparatorLevel = TaxonomyRank.PHYLUM;
	
	/**
	 * 
	 * @param analysisProperties
	 * @param analysisType
	 */
	public TaxTreeLevelOverlapAnalyser(Properties analysisProperties, AnalysisType analysisType) {
		this.analysisProperties = analysisProperties;
		this.analysisType = analysisType;
		this.numberFormatter = NumberFormat.getInstance();
		this.numberFormatter.setMaximumFractionDigits(2);
		this.numberFormatter.setMinimumFractionDigits(2);

	}

	@Override
	public void analyse(File reportFile, List<TaxonomyExperiment> taxExperiments ) throws IOException, EmptyTreeException {
		
		/*determine max number of rows for summary*/
		int summaryRows = 0;
		for( TaxonomyRank tr : TaxonomyRank.values() ){
			if( TaxonomyRank.isShortLineage(tr) ) summaryRows++;
		}
		
		/*return if there are no experiments*/
		if( taxExperiments.size() == 0 ){ return; }
		
		/*create an Excel workbook and sheets for output*/
		this.excelWb = new HSSFWorkbook();

		/*create the different styles for the cell types*/
		this.excelStyles = ExcelUtils.createStyles(excelWb);

		this.firstRankingComparator = new TaxonomyRankingComparator(firstPrimaryRankingComparatorLevel, secondaryRankingComparatorLevel);
		this.secondRankingComparator = new TaxonomyRankingComparator(secondPrimaryRankingComparatorLevel, secondaryRankingComparatorLevel);
		this.thirdRankingComparator = new TaxonomyRankingComparator(thirdPrimaryRankingComparatorLevel, secondaryRankingComparatorLevel);
		this.fourthRankingComparator = new TaxonomyRankingComparator(fourthPrimaryRankingComparatorLevel, secondSecondaryRankingComparatorLevel);

		/*with a single experiment, simply analyse its properties*/
		if( taxExperiments.size() == 1 ){ 
			/*reset the row number to 0 for each comparison*/
			currentExcelRowNumber = 0;
			
			/*create a named excel sheet*/
			currentExcelSheet = excelWb.createSheet("tree " + taxExperiments.get(0).getDisplayName() );
			
			summarizeExperiment( taxExperiments.get(0) );
			
			return;
		}
		else{
			/*summarize all experiments*/
			for( int i=0; i<taxExperiments.size(); i++ ){
				/*create a named excel sheet*/
				currentExcelSheet = excelWb.createSheet("tree " + taxExperiments.get(i).getDisplayName() );
				summarizeExperiment( taxExperiments.get(i) );
			}
			/*create ranking comparison sheet*/
			currentExcelSheet = excelWb.createSheet("SPranks");
			createRankingComparison(firstRankingComparator);
			
			currentExcelSheet = excelWb.createSheet("GENUSranks");
			createRankingComparison(secondRankingComparator);
			
			currentExcelSheet = excelWb.createSheet("FAMranks");
			createRankingComparison(thirdRankingComparator);

			currentExcelSheet = excelWb.createSheet("ORDERranks");
			createRankingComparison(fourthRankingComparator);

			/*do pairwise only if requested by analysis type*/
			if( analysisType == AnalysisType.TAX_LEVEL_OVERLAP ){
				doPairwiseComparisons(taxExperiments, summaryRows);
			}
		}
		
		/*write to file*/
		FileOutputStream out = null;
		try {
			out = new FileOutputStream( reportFile );
			excelWb.write(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			out.close();
		}
	}

	/**
	 * creates the ranking comparison excel sheets
	 * @param secondRankingComparator
	 */
	private void createRankingComparison(TaxonomyRankingComparator rankingComparator) {
		//System.out.println(rankingComparator.getRankComparisons().toString());
		/*reset the row number to 0 for each comparison*/
		currentExcelRowNumber = 0;
		
		/*create a named excel sheet*/
		currentExcelSheet.setColumnWidth(0, 9000);
		currentExcelSheet.setColumnWidth(1, 6000);
		currentExcelSheet.setDefaultColumnWidth(12);
		
		/*create a row and write header infor to it*/
		Row r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		r.setHeightInPoints(50);
		createExcelCell( r, 0, ("RANKING COMPARISON OF TREE EXPERIMENTS" ), ExcelUtils.TITLE_STYLE, Cell.CELL_TYPE_STRING );

		/*skip a row*/
		currentExcelRowNumber++;

		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 0, rankingComparator.getRank().toString(), ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 1, rankingComparator.getSubRank().toString(), ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );

		int colNum = 2; 
		for( String id : RankComparison.experimentIDs ){
			createExcelCell( r, colNum, "rank " + id, ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
			createExcelCell( r, colNum+1, "count " + id, ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
			colNum+=2;
		}

		List<RankComparison> rankComps = rankingComparator.getRankComparisons();
		int count = 0;
		for( RankComparison rc : rankComps ){
			count++;
			r = currentExcelSheet.createRow( currentExcelRowNumber++ );
			createExcelCell( r, 0, rc.getPrimaryRankName(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
			createExcelCell( r, 1, rc.getSubRankName(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
			
			colNum = 2; 
			for( String id : RankComparison.experimentIDs ){
				RankingCountPair rcp = rc.getRankingCountPair(id);
				if(rcp.ranking == Integer.MAX_VALUE ){
					createExcelCell( r, colNum, "-", ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
				}
				else{
					createExcelCell( r, colNum, "" + rcp.ranking, ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
				}
				createExcelCell( r, colNum+1, "" + rcp.count, ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
				colNum+=2;
			}
			if( count == 100 ) break;
		}
	}
	
	/**
	 * performs pairwise comparisons over all experiments
	 * @param taxExperiments
	 */
	private void doPairwiseComparisons(List<TaxonomyExperiment> taxExperiments, int summaryRows){
		
		/*perform pairwise comparisons*/
		for( int i=0; i<taxExperiments.size(); i++ ){
			TaxonomyExperiment teOne = taxExperiments.get(i);
			for( int j=i+1; j<taxExperiments.size(); j++ ){
				TaxonomyExperiment teTwo = taxExperiments.get(j);

				/*reset the list of summaries*/
				overlapSummaries = new ArrayList<OverlapSummary>();
				
				/*reset the row number to 0 for each comparison*/
				currentExcelRowNumber = 0;
				
				/*create a named excel sheet*/
				System.out.println("trees compared currently: \"" 
						+ teOne.getDisplayName() + "\" with \"" + teTwo.getDisplayName() + "\"");
				
				currentExcelSheet = excelWb.createSheet(teOne.getDisplayName() + "-" + teTwo.getDisplayName());
				currentExcelSheet.setColumnWidth(0, 5000);
				currentExcelSheet.setColumnWidth(1, 5000);
				currentExcelSheet.setColumnWidth(2, 4000);
				currentExcelSheet.setColumnWidth(3, 3000);
				currentExcelSheet.setColumnWidth(4, 3000);
				currentExcelSheet.setColumnWidth(5, 10000);
				
				/*create a row and write header infor to it*/
				Row r = currentExcelSheet.createRow( currentExcelRowNumber++ );
				r.setHeightInPoints(50);
				createExcelCell( r, 0, ("COMPARING TREE EXPERIMENTS\nFirst:\t" + teOne.getDisplayName() + "(" + teOne.getId() + ")\nSecond:\t"+ teTwo.getDisplayName() + "(" + teTwo.getId() + ")")
						, ExcelUtils.TITLE_STYLE, Cell.CELL_TYPE_STRING );
				
				r = currentExcelSheet.createRow( currentExcelRowNumber++ );
				
				currentExcelRowNumber = summaryRows + 5;
				r = currentExcelSheet.createRow( currentExcelRowNumber++ );
				r.setHeightInPoints(ExcelUtils.TITLE_CELL_HEIGHT);
				createExcelCell( r, 0, "RANK-LEVEL COMPARISON DETAILS", ExcelUtils.TITLE_STYLE, Cell.CELL_TYPE_STRING );

				for( TaxonomyRank tr : TaxonomyRank.values() ){
					if( TaxonomyRank.isShortLineage(tr) ){
						//System.out.println( "[ " + this.getClass().getSimpleName() + "] comparing taxonomy level " + tr);
						compareRanks(tr, teOne, teTwo );
					}
				}
				
				/*reset rows to top of document for summaries*/
				currentExcelRowNumber = 2;
				
				/*write comparison summary on top*/
				r = currentExcelSheet.createRow( currentExcelRowNumber++ );
				
				r.setHeightInPoints(ExcelUtils.TITLE_CELL_HEIGHT);
				createExcelCell( r, 0, "SUMMARY OF COMPARISON", ExcelUtils.TITLE_STYLE, Cell.CELL_TYPE_STRING );
				
				r = currentExcelSheet.createRow( currentExcelRowNumber++ );
				
				createExcelCell( r, 0, "taxonomy rank", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
				createExcelCell( r, 1, "count_" + teOne.getDisplayName(), ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
				createExcelCell( r, 2, "count_" + teTwo.getDisplayName(), ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
				createExcelCell( r, 3, "count_union", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
				createExcelCell( r, 4, "Pearson correlation", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );

				
				for( OverlapSummary ols : overlapSummaries){
					r = currentExcelSheet.createRow( currentExcelRowNumber++ );
					createExcelCell( r, 0, ols.getRank().toString(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
					createExcelCell( r, 1, ""+ols.getCountOne(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
					createExcelCell( r, 2, ""+ols.getCountTwo(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
					createExcelCell( r, 3, ""+ols.getCountUnion(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
					if( ols.getPearsonCorrelation()==Double.MIN_VALUE ){
						createExcelCell( r, 4, "NA", ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
					}else{
						createExcelCell( r, 4, ""+ols.getPearsonCorrelation(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
					}
					//String pc = (ols.getPearsonCorrelation()==Double.MIN_VALUE ? "NA" : ""+ ols.getPearsonCorrelation() );
					//createExcelCell( r, 4, pc, ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
				}

				/*present all pairwise comparisons on separate sheets*/
				//currentExcelSheetNumber++;
			}
		}			
		
		
	}
	
	/**
	 * summarize a single experiment
	 * @param te
	 * @throws EmptyTreeException 
	 */
	private void summarizeExperiment(TaxonomyExperiment te) throws EmptyTreeException {
		/*get required parameters*/
		int occurrencecutoff = Integer.parseInt( analysisProperties.getProperty("node_count_cutoff", "20") );
		boolean cumulative = Boolean.parseBoolean( analysisProperties.getProperty("cumulative", "true") );
		
		/*reset the row number to 0 for each comparison*/
		currentExcelRowNumber = 0;
		
		/*create a named excel sheet*/
		currentExcelSheet.setColumnWidth(0, 3500);
		currentExcelSheet.setColumnWidth(1, 3500);
		currentExcelSheet.setColumnWidth(2, 6000);
//		currentExcelSheet.setColumnWidth(3, 3000);
//		currentExcelSheet.setColumnWidth(4, 3000);
//		currentExcelSheet.setColumnWidth(5, 10000);
		
		/*create a row and write header infor to it*/
		Row r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		r.setHeightInPoints(50);
		createExcelCell( r, 0, ("SUMMARY OF TREE EXPERIMENT: " + te.getDisplayName() ), ExcelUtils.TITLE_STYLE, Cell.CELL_TYPE_STRING );

		/*skip a row*/
		currentExcelRowNumber++;

		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 0, "Description", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 1, te.getDescription() , ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );

		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		DateFormat df = DateFormat.getInstance();
		createExcelCell( r, 0, "Date", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 1, df.format(te.getDate().getTime()) , ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );

		/*skip a row*/
		currentExcelRowNumber++;
		
		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 0, "Rank summary", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );

		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 0, "rank", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 1, "# of nodes", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 2, "summed levelcount", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING);
		createExcelCell( r, 3, "% of total", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING);

		/*root row*/
		TaxNode root = te.getTree().getRoot();
		int rootTotal = root.getCumulativeChildNumber();
		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 0, root.getScientificName(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 1, "1", ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 2, rootTotal+"", ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 3, "100", ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );

		HashMap<TaxonomyRank, Integer> levelTotals = new HashMap<TaxonomyRank, Integer>();
		
		for( TaxonomyRank tr : TaxonomyRank.values() ){
			if( TaxonomyRank.isShortLineage(tr) ){
				List<TaxNode> nodes = te.getTopOccurringNodes( tr, Integer.MAX_VALUE, cumulative );

				int rank = 0;
				for( TaxNode tn : nodes ){
					rank++;
					
					/*process for rank comparisons*/
					if( tr==this.firstPrimaryRankingComparatorLevel ){
						Lineage l = te.getTree().getLineage(tn);
						firstRankingComparator.addRankCountData(l, rank, te.getDisplayName());
					}
					else if( tr==this.secondPrimaryRankingComparatorLevel ){
						Lineage l = te.getTree().getLineage(tn);
						secondRankingComparator.addRankCountData(l, rank, te.getDisplayName());
					}
					else if( tr==this.thirdPrimaryRankingComparatorLevel ){
						Lineage l = te.getTree().getLineage(tn);
						thirdRankingComparator.addRankCountData(l, rank, te.getDisplayName());
					}
					else if( tr==this.fourthPrimaryRankingComparatorLevel ){
						Lineage l = te.getTree().getLineage(tn);
						fourthRankingComparator.addRankCountData(l, rank, te.getDisplayName());
					}
					
				}
				//currentExcelRowNumber++;
				r = currentExcelSheet.createRow( currentExcelRowNumber++ );
				createExcelCell( r, 0, tr.toString(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );

				createExcelCell( r, 1, ""+nodes.size(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
				
				int levelCount = te.getTaxonomyLevelOccurrenceCount(tr);
				levelTotals.put(tr, levelCount);
				createExcelCell( r, 2, ""+levelCount, ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
				
				double percentage = ((double)levelCount / rootTotal) * 100;
				//System.err.println("root total=" + rootTotal + " level=" + tr 
				//		+ " count=" + levelCount + " %=" + percentage + " formatted=" + numberFormatter.format(percentage));
				createExcelCell( r, 3, numberFormatter.format(percentage).replace(',', '.'), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
			}
		}
		
		/*skip a row*/
		currentExcelRowNumber++;
		
		
		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		r.setHeightInPoints(ExcelUtils.TITLE_CELL_HEIGHT);
		createExcelCell( r, 0, "RANKS DETAILS OF " + occurrencecutoff + " TOP OCCURRING NODES", ExcelUtils.TITLE_STYLE, Cell.CELL_TYPE_STRING );
		
		for( TaxonomyRank tr : TaxonomyRank.values() ){
			if( TaxonomyRank.isShortLineage(tr) ){
				/*fetch nodes of tree */
				List<TaxNode> nodes = te.getTopOccurringNodes( tr, occurrencecutoff, cumulative );
				
				if( nodes.size() > 0 ){
					r = currentExcelSheet.createRow( currentExcelRowNumber++ );
					r.setHeightInPoints(ExcelUtils.TITLE_CELL_HEIGHT);
					createExcelCell( r, 0, "TAXONOMY LEVEL " + tr, ExcelUtils.TITLE_STYLE, Cell.CELL_TYPE_STRING );

					r = currentExcelSheet.createRow( currentExcelRowNumber++ );
					createExcelCell( r, 1, "rank", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
					createExcelCell( r, 2, "name", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
					createExcelCell( r, 3, "tax_id", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
					createExcelCell( r, 4, "cumulative count", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
					createExcelCell( r, 5, " %", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
					createExcelCell( r, 6, "cumulative %", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
					createExcelCell( r, 7, "leaf count", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
					createExcelCell( r, 8, "lineage", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );

					int rank = 0;
					double cumPercentage = 0;
					for( TaxNode tn : nodes ){
						rank++;
						/*create summary sheet elements*/
						r = currentExcelSheet.createRow( currentExcelRowNumber++ );
						createExcelCell( r, 1, rank+"", ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
						createExcelCell( r, 2, tn.getScientificName(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
						createExcelCell( r, 3, ""+tn.getTaxID(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
						createExcelCell( r, 4, ""+tn.getCumulativeChildNumber(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
						double percentage = ((double)tn.getCumulativeChildNumber() / levelTotals.get(tr)) * 100;
						createExcelCell( r, 5, numberFormatter.format(percentage).replace(',','.'), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
						cumPercentage += percentage;
						createExcelCell( r, 6, numberFormatter.format(cumPercentage).replace(',','.'), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
						createExcelCell( r, 7, ""+tn.getOccurenceCount(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
						createExcelCell( r, 8, te.getLineageString( tn ), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
						
					}
					/*skip a row*/
					currentExcelRowNumber++;
				}
			}
		}
	}

	private Cell createExcelCell( Row row, int column, String contents, String styleName, int cellType ){
		Cell c = row.createCell( column );
		c.setCellStyle(excelStyles.get(styleName));
		c.setCellType( cellType );
		if( cellType == Cell.CELL_TYPE_NUMERIC ){
			try{
				c.setCellValue(Double.parseDouble(contents));
			}catch (Exception e) {
				c.setCellValue("NA");
//				e.printStackTrace();
//				System.exit(0);
			}
		}
		else c.setCellValue( contents );
		return c;
	}
	
	
	/**
	 * compares the ranks of two taxonomy experiment trees
	 * @param rank the taxonomic rank
	 * @param teOne the tree experiment one
	 * @param teTwo the tree experiment two
	 * @param pw the printwriter to write to
	 */
	private void compareRanks(TaxonomyRank rank, TaxonomyExperiment teOne, TaxonomyExperiment teTwo) {
		/*get required parameters*/
		int occurrencecutoff = Integer.parseInt( analysisProperties.getProperty("node_count_cutoff", "10") );
		boolean cumulative = Boolean.parseBoolean( analysisProperties.getProperty("cumulative", "true") );
		
		/*fetch nodes of tree one*/
		List<TaxNode> nodesOne = teOne.getTopOccurringNodes( rank, occurrencecutoff, cumulative );
		
		/*put them in a set for quick check of existence*/
		Set<TaxNode> nodesOneSet = new HashSet<TaxNode>();
		nodesOneSet.addAll(nodesOne);
		
		/*fetch nodes of tree two*/
		List<TaxNode> nodesTwo = teTwo.getTopOccurringNodes( rank, occurrencecutoff, cumulative );
		
		/*return if nothing to compare*/
		if(nodesOne.size() == 0 || nodesTwo.size() == 0 ) return;

		/*check which ones are present in both the sets*/
		Set<TaxNode> unionNodesSet = new HashSet<TaxNode>();
		for( TaxNode tn : nodesTwo ){
			if( nodesOneSet.contains(tn) ) unionNodesSet.add(tn);
		}
		
		currentExcelRowNumber++;
		Row r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		r.setHeightInPoints(ExcelUtils.TITLE_CELL_HEIGHT);
		createExcelCell( r, 0, "TAXONOMY LEVEL " + rank, ExcelUtils.TITLE_STYLE, Cell.CELL_TYPE_STRING );

		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 1, "experiment", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 2, "node count", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
		
		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 1, teOne.getDisplayName(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 2, ""+nodesOne.size(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );

		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 1, teTwo.getDisplayName(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 2, ""+nodesTwo.size(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );

		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 1, "union", ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 2, ""+unionNodesSet.size(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );

		/*skip a row*/
		currentExcelRowNumber++;
		
		OverlapSummary os = new OverlapSummary();
		os.setRank(rank);
		
		try{
			/*this method also fetches the countOne, countTwo and unionCount data*/
			double pc = getPearsonCorrelation(rank, teOne, teTwo, os);
			os.setPearsonCorrelation( pc );
		}catch (IllegalArgumentException e) {
			os.setPearsonCorrelation(Double.MIN_VALUE);
		}
		overlapSummaries.add( os );
		
		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 1, "NODES OF " + teOne.getDisplayName(), ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );

		printLevelNodesInfo(teOne, nodesOne, unionNodesSet);
		
		r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 1, "NODES OF " + teTwo.getDisplayName(), ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );

		printLevelNodesInfo(teTwo, nodesTwo, unionNodesSet);
		
	}

	/**
	 * calculates the overlap score statistic for two lists of nodes. 
	 * @param rank
	 * @param teOne
	 * @param teTwo
	 * @param IllegalArgumentException is thrown when the score cannot be calculated (eg too few data) 
	 */
	private double getPearsonCorrelation(TaxonomyRank rank, TaxonomyExperiment teOne, 
			TaxonomyExperiment teTwo, OverlapSummary os) throws IllegalArgumentException {
		/*get both lists*/
		List<TaxNode> nodesOne = teOne.getTopOccurringNodes( rank, Integer.MAX_VALUE, true );
		List<TaxNode> nodesTwo = teTwo.getTopOccurringNodes( rank, Integer.MAX_VALUE, true );
		
		os.setCountOne( nodesOne.size() );
		os.setCountTwo( nodesTwo.size() );
		
		HashMap<Integer, NodeCompareStatistics> nodeStatistics = new HashMap<Integer, NodeCompareStatistics>();
		int rankCountOne = 0;
		int rankCountTwo = 0;
		/*create a overlapping set that encompasses both lists: list one*/
		for( TaxNode tn : nodesOne){
			assert ! nodeStatistics.containsKey( tn.getTaxID() );
			
			rankCountOne += tn.getCumulativeChildNumber();
			
			NodeCompareStatistics ncs = new NodeCompareStatistics( tn.getTaxID() );
			ncs.countOne = Math.log10( tn.getCumulativeChildNumber() );
			ncs.name = tn.getScientificName();
			nodeStatistics.put(ncs.id, ncs);
		}
		/*create a overlapping set that encompasses both lists: list two*/
		for( TaxNode tn : nodesTwo){
			rankCountTwo += tn.getCumulativeChildNumber();
			
			if( nodeStatistics.containsKey(tn.getTaxID()) ){
				NodeCompareStatistics ncs = nodeStatistics.get( tn.getTaxID() );
				ncs.countTwo = Math.log10( tn.getCumulativeChildNumber() );
			}
			else{
				NodeCompareStatistics ncs = new NodeCompareStatistics( tn.getTaxID() );
				ncs.countTwo = Math.log10( tn.getCumulativeChildNumber() );
				ncs.name = tn.getScientificName();
				nodeStatistics.put(ncs.id, ncs);
			}
		}
		
		//int degreesOfFreedom = nodeStatistics.size() - 1;
		double correctionFactor = (double)rankCountOne / rankCountTwo;
		//System.out.println( "DF=" + degreesOfFreedom 
		//		+ " rank count one=" + rankCountOne 
		//		+ " rank count two=" + rankCountTwo
		//		+ " correction factor=" + correctionFactor );
		
		/* CHI-SQUARE = sum( (O-E)^2/E )
		 * df = degrees of freedom: nodes in rank -1*/

		int size = nodeStatistics.size();
		double[] one = new double[ size ];
		double[] two = new double[ size ];
		double[] twoCorrected = new double[ size ];
		int index = 0;
		int unionCount = 0;
		
		for( NodeCompareStatistics ncs : nodeStatistics.values() ){
			//System.out.println( "name=" + ncs.name + " taxID=" + ncs.id + " countOne=" + ncs.countOne + " countTwo=" + ncs.countTwo);
			if( Math.pow( ncs.countOne, 10 ) > 0 && Math.pow( ncs.countTwo, 10 ) > 0 ) unionCount++;
			one[index] = ncs.countOne;
			two[index] = ncs.countTwo;
			twoCorrected[index] = ncs.countTwo * correctionFactor;
			index++;
		}
		os.setCountUnion(unionCount);
		
		if( size > 1 ){
			double correlation = new PearsonsCorrelation().correlation(one, two);
			//System.out.println( "rank=" + rank + " correlation=" + correlation);
			return correlation;
			
		}else throw new IllegalArgumentException("too few data points to calculate correlation");
		
		
//		System.out.println( "rank=" + rank + " observed=" + Arrays.toString( one ) + " expected=" + Arrays.toString( twoCorrected ) );
		
//		System.out.println(TestUtils.chiSquare(twoCorrected, one));
//		try {
//			System.out.println(TestUtils.chiSquareTest(twoCorrected, one));
//			System.out.println(TestUtils.chiSquareTest(twoCorrected, one, 0.0001));
//		} catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (MathException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		//System.out.println(TestUtils.chiSquareTest(expected, observed));
		//TestUtils.chiSquareTest(expected, observed);
	}

	/**
	 * prints the occurrences for a tax level for an experiment
	 * @param pw
	 * @param te
	 * @param nodesList
	 * @param unionNodesSet
	 */
	private void printLevelNodesInfo( TaxonomyExperiment te, List<TaxNode> nodesList, Set<TaxNode> unionNodesSet) {
		Row r = currentExcelSheet.createRow( currentExcelRowNumber++ );
		createExcelCell( r, 1, "Name", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 2, "Cumulative occurrence", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 3, "Leaf occurrence", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 4, "In union", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );
		createExcelCell( r, 5, "Lineage", ExcelUtils.SUBTITLE_STYLE, Cell.CELL_TYPE_STRING );

		for( TaxNode tn : nodesList ){
			r = currentExcelSheet.createRow( currentExcelRowNumber++ );
			createExcelCell( r, 1, tn.getScientificName(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
			createExcelCell( r, 2, ""+tn.getCumulativeChildNumber(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
			createExcelCell( r, 3, ""+tn.getOccurenceCount(), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_NUMERIC );
			createExcelCell( r, 4, unionNodesSet.contains(tn)?"yes":"no", ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
			createExcelCell( r, 5, te.getLineageString( tn ), ExcelUtils.DATA_STYLE, Cell.CELL_TYPE_STRING );
		}
		currentExcelRowNumber++;
	}
	
	
	
	private class OverlapSummary{
		private TaxonomyRank rank;
		private int countOne;
		private int countTwo;
		private int countUnion;
		private double overlapScore;
		/**
		 * @return the rank
		 */
		public TaxonomyRank getRank() {
			return rank;
		}
		/**
		 * @param rank the rank to set
		 */
		public void setRank(TaxonomyRank rank) {
			this.rank = rank;
		}
		/**
		 * @return the countOne
		 */
		public int getCountOne() {
			return countOne;
		}
		/**
		 * @param countOne the countOne to set
		 */
		public void setCountOne(int countOne) {
			this.countOne = countOne;
		}
		/**
		 * @return the countTwo
		 */
		public int getCountTwo() {
			return countTwo;
		}
		/**
		 * @param countTwo the countTwo to set
		 */
		public void setCountTwo(int countTwo) {
			this.countTwo = countTwo;
		}
		/**
		 * @return the countUnion
		 */
		public int getCountUnion() {
			return countUnion;
		}
		/**
		 * @param countUnion the countUnion to set
		 */
		public void setCountUnion(int countUnion) {
			this.countUnion = countUnion;
		}
		/**
		 * @return the overlapScore
		 */
		public double getPearsonCorrelation() {
			return overlapScore;
		}
		/**
		 * @param overlapScore the overlapScore to set
		 */
		public void setPearsonCorrelation(double overlapScore) {
			this.overlapScore = overlapScore;
		}
	}
	
	private class NodeCompareStatistics{
		public int id;
		@SuppressWarnings("unused")
		public String name;
		public double countOne;
		public double countTwo;
		//public double binomialPvalue;
		
		public NodeCompareStatistics( int id ){
			this.id = id;
		}
	}
	
}
