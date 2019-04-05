package ng.upperlink.nibss.cmms.mandates.utils;

import ng.upperlink.nibss.cmms.mandates.exceptions.CustomGenericException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/*
* Excel reader to read excel files
* */
public class ExcelReader {
    private static Logger logger = LoggerFactory.getLogger(ExcelReader.class);

    private int startRow;

    private int columnCount;

    private List<String> keyColumns = new ArrayList<String>();

    private Workbook workbook;

    private ExcelType excelType;

    public enum ExcelType{
        XLS,XLSX;
    }

    public ExcelReader(ExcelType type){
        this.excelType=type;
    }


    //read a file path
    public List<String[]> readFilePath(String filePath) throws IOException{
        InputStream is=new FileInputStream(new File(filePath));
        return readInputStream(is);
    }


    /** Read a file
     * @param file
     * @return
     * @throws IOException
     */
    public List<String[]> readFile(File file) throws IOException{
        InputStream is=new FileInputStream(file);
        return readInputStream(is);

    }

    public List<String[]> readInputStream(InputStream stream) throws IOException {
        switch(excelType){
            case XLS:
                workbook = new HSSFWorkbook(stream);
                break;
            case XLSX:
                workbook = new XSSFWorkbook(stream);
                break;
            default:throw new CustomGenericException("Invalid file type specified");
        }
        stream.close();
       List<String[]> wk = processWorkBook(workbook);
       return wk;
    }

    /** Iterate through workbook and process same
     * @param workbook
     * @return
     */
    private List<String[]> processWorkBook(Workbook workbook) {
       List<String[]> recordList=new ArrayList<>();
       workbook.setMissingCellPolicy(Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);

        Sheet sheet=workbook.getSheetAt(0); //get the first sheet
        int rowc=0;
        columnCount=this.getColumnCount();
        String[] currentRecord=null;
        read:
        for(rowc =startRow;;rowc++){
            Row row = sheet.getRow(rowc);

            currentRecord= new String[columnCount+1];

            String currval=null;
            for (int j = 0; j < columnCount; j++) {

                Cell cell=null;
                try {
                    cell = row.getCell(j);
                } catch (NullPointerException e) {
                    cell=null;

                }
                //this is to handle the spaces in the format
                if (cell==null && currentRecord[(j==0?0:j-1)]==null){
                    break read;
                }

                if (null!=cell){

                    switch(cell.getCellTypeEnum()){
                        case STRING:
                            currval = cell.getStringCellValue().trim();
                            break;
                        case NUMERIC:
                            currval =  String.format("%.2f",cell.getNumericCellValue()); // new
                            //currval =  String.valueOf(cell.getNumericCellValue());

                            break;
                        case FORMULA:
                            System.out.println("Formula is " + cell.getCellFormula());
                            switch(cell.getCachedFormulaResultTypeEnum()) {
                                case NUMERIC:
                                    System.out.println("Last evaluated as: " + cell.getNumericCellValue());
                                    currval = String.format("%.2f",cell.getNumericCellValue());
                                    break;
                                case STRING:
                                    System.out.println("Last evaluated as \"" + cell.getRichStringCellValue() + "\"");
                                    currval =cell.getStringCellValue().trim();
                                    break;
                            }
                            break;

                    }
                    logger.debug("[" + rowc + "," + j + "]=" + currval);
                }else{
                    currval="";
                }
                currentRecord[j]=currval==null?"":currval.trim();

            }

            recordList.add(currentRecord);
        }
        return recordList;
    }

    //get start of row
    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    //get columns
    public List<String> getKeyColumns() {
        return keyColumns;
    }

    public void setKeyColumns(List<String> keyColumns) {
        this.keyColumns = keyColumns;
    }

    //get column count
    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }
}
