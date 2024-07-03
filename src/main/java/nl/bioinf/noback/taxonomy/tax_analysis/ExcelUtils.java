package nl.bioinf.noback.taxonomy.tax_analysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelUtils {
	private static final String FONT_NAME = "Arial";
	public static final String TITLE_STYLE = "title";
	public static final String SUBTITLE_STYLE = "subtitle";
	public static final String DATA_STYLE = "data";
	public static final int TITLE_CELL_HEIGHT = 20;
	public static final int SUBTITLE_CELL_HEIGHT = 14;
	//public static final int DATA_CELL_HEIGHT = 10;
	
//	public static final int WIDE_COLUMN = 20;
	
    /**
     * cell styles used for formatting calendar sheets
     */
    public static Map<String, CellStyle> createStyles(Workbook wb){
        Map<String, CellStyle> styles = new HashMap<String, CellStyle>();

        CellStyle style;
        
        Font titleFont = wb.createFont();
        titleFont.setFontHeightInPoints((short)14);
        titleFont.setFontName( FONT_NAME );
        style = wb.createCellStyle();
        style.setFont(titleFont);
        style.setBorderBottom(CellStyle.BORDER_DOTTED);
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        //style.setFillForegroundColor( IndexedColors.DARK_YELLOW.getIndex() );
       // style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        styles.put( TITLE_STYLE, style );

        Font subtitleFont = wb.createFont();
        subtitleFont.setFontHeightInPoints((short)10);
        subtitleFont.setFontName( FONT_NAME );
        subtitleFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        style = wb.createCellStyle();
        style.setFont(subtitleFont);
        style.setFillForegroundColor( IndexedColors.LIGHT_YELLOW.getIndex() );
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setAlignment(CellStyle.ALIGN_LEFT);
        //style.setBorderBottom(CellStyle.BORDER_DOTTED);
        //style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        styles.put( SUBTITLE_STYLE, style );

        Font dataFont = wb.createFont();
        dataFont.setFontHeightInPoints((short)10);
        dataFont.setFontName( FONT_NAME );
        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_LEFT);
        style.setFont(dataFont);
        styles.put( DATA_STYLE, style );
        
        
        //styles below are not used (yet)
        Font itemFont = wb.createFont();
        itemFont.setFontHeightInPoints((short)9);
        itemFont.setFontName("Trebuchet MS");
        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_LEFT);
        style.setFont(itemFont);
        styles.put("item_left", style);

        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setFont(itemFont);
        styles.put("item_right", style);


        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFont(itemFont);
        style.setDataFormat(wb.createDataFormat().getFormat("m/d/yy"));
        styles.put("input_d", style);


        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setFont(itemFont);
        style.setBorderRight(CellStyle.BORDER_DOTTED);
        style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBorderBottom(CellStyle.BORDER_DOTTED);
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBorderLeft(CellStyle.BORDER_DOTTED);
        style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBorderTop(CellStyle.BORDER_DOTTED);
        style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setDataFormat(wb.createDataFormat().getFormat("0"));
        style.setBorderBottom(CellStyle.BORDER_DOTTED);
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        styles.put("formula_i", style);

        return styles;
    }
}
