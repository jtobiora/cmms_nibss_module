package ng.upperlink.nibss.cmms.mandates.utils;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;

/*
*  operations that can be performed on excel sheet
* */
public interface IExcelOperation {
    void writeDropDown(Workbook workbook, Sheet sheet) throws Exception;

    <T> List<T> fetchFromExcelSheet(Workbook workbook, int totalRowCount, Object object) throws Exception;

    void createAllCustomCell(Workbook workbook);

    void setTitle(Row row);

    <T> List<T> saveBulk(List<T> bulkObject) throws Exception;

    <T> List<T> generateAndSaveObjects(Workbook workbook, int totalRowCount, Object object) throws Exception;
}
