package com.controlunion.excelUploader.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Component
public class ExcelSheetUtil {

    public static Iterator<Row> createCustomRowIterator(XSSFSheet sheet, int startRow) {
        final Iterator<Row> iterator = sheet.iterator();

        // Advance the iterator to the specified starting row
        int i = 0;
        int rowNumber = 0;
        while (iterator.hasNext() && i < startRow){

            System.out.println("Skipping RowNo "+rowNumber);
            if (rowNumber < i){
                break;
            }
            Row row = iterator.next();
            rowNumber = row.getRowNum();
            i++;
        }

        return new Iterator<Row>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Row next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }
}
