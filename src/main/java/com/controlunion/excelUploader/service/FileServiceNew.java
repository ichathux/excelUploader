package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.enums.Errors;
import com.controlunion.excelUploader.model.FarmerList;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
public class FileServiceNew {

    private boolean isAuditDetailsDone = false;

    public ResponseEntity uploadExcelFile(MultipartFile file) {
        log.info("File recieved " + file.getOriginalFilename());
        List<String> errorList = new ArrayList<>();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is not present");
        }
        if (!isFileExcel(file)) {
            return ResponseEntity.badRequest().body("File format not support. It must be .xls or .xlsx");
        }
        return readFile(file, errorList);

    }

    private boolean isFileExcel(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        System.out.println(fileName);
        assert fileName != null;
        if ((fileName.endsWith(".xls") || fileName.endsWith(".xlsx"))) {
            return true;
        }
        return false;
    }

    private ResponseEntity readFile(MultipartFile file,
                                    List<String> errorList) {

        List<FarmerList> farmerLists = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0); // Assuming the data is on the first sheet

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                XSSFRow row = sheet.getRow(i);
                if (row != null) {
                    if (!checkRowContainNullValues(row)){
                        System.out.println(Errors.EMPTY_ROW.getName() + " : " + (i+1));
                    }else {
                        if (i == 0){

                        }
                        if (i == 1){

                        }
                        if (i == 2){

                        }
                    }
                } else {
                    System.out.println(Errors.EMPTY_ROW.getName() + " : " + (i+1));

                }


            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!errorList.isEmpty()) {
            log.error("errors while reading file ");
            return ResponseEntity.badRequest().body(errorList);
        }
        return ResponseEntity.ok().body(farmerLists);
    }

    private boolean checkRowContainNullValues(Row row) {
        Iterator<Cell> cells = row.cellIterator();
        boolean notAEmptyRow = false;

        while (cells.hasNext()){
            Cell cell = cells.next();
            if (cell.getCellType() != CellType.BLANK){
                return true;
            }
        }
        return notAEmptyRow;
    }
}
