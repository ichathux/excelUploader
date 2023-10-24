package com.controlunion.excelUploader.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class FileService {
    private long startTime;
    private long endTime;
    public ResponseEntity uploadExcelFile(MultipartFile file) {
        log.info("File recieved " + file.getOriginalFilename());
        startTime = System.currentTimeMillis();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is not present");
        }
        return readFile(file);
    }

    private ResponseEntity readFile(MultipartFile file) {
        List<String> errorList = new ArrayList<>();
        HashMap<Long, ArrayList<String>> userMap = new HashMap<>();
        try {
            File temp = File.createTempFile("temp" , null);
            file.transferTo(temp);
            try (Workbook workbook = WorkbookFactory.create(temp)) {
                Sheet sheet = workbook.getSheetAt(0); // Assuming the data is on the first sheet
                for (Row row : sheet) {
                    DataFormatter dataFormatter = new DataFormatter();
                    if (row.getCell(0) == null){
                        break;
                    }
                    int rowNumber = row.getRowNum();
                    if (rowNumber == 0) {
                        cellNameValidation(row.getCell(0),
                                "Project Name",
                                errorList
                                );
                    }
                    if (rowNumber == 1){
                        cellNameValidation(row.getCell(0),
                                "Project Id",
                                errorList
                        );
                    }
                    if (rowNumber == 2){
                        cellNameValidation(row.getCell(0),
                                "Year",
                                errorList
                        );
                    }
                    if (rowNumber == 3){
                        log.info("reading headers");
                        Iterator<Cell> cellIterator = row.cellIterator();
                        while (cellIterator.hasNext()){
                            Cell cell = cellIterator.next();
                            log.info("Cell "+cell.getAddress() +" : "+cell.getStringCellValue());
                        }
                    }
                    if (rowNumber == 4){
                        log.info("reading sub headers");
                        Iterator<Cell> cellIterator = row.cellIterator();
                        while (cellIterator.hasNext()){
                            Cell cell = cellIterator.next();
                            log.info("Cell "+cell.getAddress() +" : "+cell.getStringCellValue());
                        }
                    }
                    if (rowNumber == 5){
                        log.info("reading sub sub headers");
                        Iterator<Cell> cellIterator = row.cellIterator();
                        while (cellIterator.hasNext()){
                            Cell cell = cellIterator.next();
                            log.info("Cell "+cell.getAddress() +" : "+cell.getStringCellValue());
                        }
                    }
                    if (rowNumber > 5){
                        log.info("user data "+rowNumber);
                        log.info("values "+row.getCell(0).getNumericCellValue()+" : "+row.getCell(7).getStringCellValue());
                        long cuid = BigDecimal.valueOf(row.getCell(0).getNumericCellValue()).longValue();
                        log.info("values "+row.getCell(0).getNumericCellValue()+" : "+cuid);

                        String cellValue = dataFormatter.formatCellValue(row.getCell(12));
                        log.info(row.getCell(12).getAddress() + " date "+row.getCell(12).getNumericCellValue()+", "+cellValue);
                        String plotCode = row.getCell(7).getStringCellValue();
                        if (userMap.containsKey(cuid)){
                            if (userMap.get(cuid).contains(plotCode)){
                                errorList.add(row.getCell(7).getAddress()+" contains duplicate value");
                            }
                        }else {
                            ArrayList<String> strings = new ArrayList<>();
                            strings.add(plotCode);
                            userMap.put(cuid , strings);
                        }
                    }

                }
            }catch (IOException e){
                log.error("error occurred while loading excel file "+e.getMessage());
            }catch (EncryptedDocumentException e){
                log.error("encrypted document "+e.getMessage());
            }
        } catch (IOException e) {
            log.error("error occurred "+e.getMessage());
            e.printStackTrace();
        }
        log.error("errors while reading file "+errorList);
        endTime = System.currentTimeMillis();
        log.info("task end : "+(endTime-startTime)+"ms");
        if (!errorList.isEmpty()){
            return ResponseEntity.badRequest().body(errorList);
        }
        return null;
    }

    private void cellNameValidation(Cell cell,
                                    String name,
                                    List<String> errorList){
        if (!cell.toString().equalsIgnoreCase(name)){
            errorList.add(cell.getAddress()+" must be "+name);
        }
        log.info("cell 1 " + cell);
    }

    private void validateUserData(){

    }
}
