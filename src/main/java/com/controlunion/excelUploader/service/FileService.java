package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.config.ExcelFilePropertiesConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private ExcelFilePropertiesConfig excelFilePropertiesConfig;

    public FileService(ExcelFilePropertiesConfig excelFilePropertiesConfig) {
        this.excelFilePropertiesConfig = excelFilePropertiesConfig;
    }

    public ResponseEntity uploadExcelFile(MultipartFile file) {
        log.info("File recieved " + file.getOriginalFilename());
        System.out.println("config dfaat ");
        startTime = System.currentTimeMillis();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is not present");
        }
        return readFile(file);
    }

    private ResponseEntity readFile(MultipartFile file) {
        List<String> errorList = new ArrayList<>();
        Map<String, String> tableTopLevelHeaders = excelFilePropertiesConfig.getHeader1();
        Map<String, String> tableSecondLevelHeaders = excelFilePropertiesConfig.getHeader2();
        Map<Integer, String> tableThirdLevelHeaders = excelFilePropertiesConfig.getHeader3();

        HashMap<String, ArrayList<String>> userMap = new HashMap<>();
        try {
            File temp = File.createTempFile("temp" , null);
            file.transferTo(temp);
            try (Workbook workbook = WorkbookFactory.create(temp)) {
                Sheet sheet = workbook.getSheetAt(0); // Assuming the data is on the first sheet
                for (Row row : sheet) {
                    DataFormatter dataFormatter = new DataFormatter();
                    if (row.getCell(0) == null) {
                        break;
                    }
                    int rowNumber = row.getRowNum();
                    if (rowNumber == 0) {
                        cellNameValidation(row.getCell(0) ,
                                "Project Name" ,
                                errorList
                        );
                    }
                    if (rowNumber == 1) {
                        cellNameValidation(row.getCell(0) ,
                                "Project Id" ,
                                errorList
                        );
                    }
                    if (rowNumber == 2) {
                        cellNameValidation(row.getCell(0) ,
                                "Year" ,
                                errorList
                        );
                    }
                    if (rowNumber == 3) {
                        log.debug("reading headers");
                        Iterator<Cell> cellIterator = row.cellIterator();
                        while (cellIterator.hasNext()) {
                            Cell cell = cellIterator.next();
                            if (cell.getCellType() == CellType.BLANK) {
                                continue;
                            }
                            if (tableTopLevelHeaders.containsKey(cell.getAddress().toString().trim())) {
                                if (!tableTopLevelHeaders
                                        .get(cell.getAddress().toString().trim())
                                        .equals(cell.getStringCellValue().trim())) {
                                    log.error("error in " + cell.getAddress() + ", " + cell.getStringCellValue());
                                    cellNameValidation(cell ,
                                            tableTopLevelHeaders.get(cell.getAddress().toString().trim()) ,
                                            errorList
                                    );
                                }
                            }

                            log.info("Cell " + cell.getAddress() + " : " + cell.getStringCellValue());
                        }
                    }
                    if (rowNumber == 4) {
                        log.debug("reading sub headers");
                        Iterator<Cell> cellIterator = row.cellIterator();
                        while (cellIterator.hasNext()) {
                            Cell cell = cellIterator.next();
                            if (cell.getCellType() == CellType.BLANK) {
                                continue;
                            }
                            if (cell.getColumnIndex() < excelFilePropertiesConfig.getStartPoint() - 1) {
                                if (tableSecondLevelHeaders.containsKey(cell.getAddress().toString().trim())) {
                                    if (!tableSecondLevelHeaders
                                            .get(cell.getAddress().toString().trim())
                                            .equals(cell.getStringCellValue().trim())) {
                                        log.error("error in " + cell.getAddress() + ", " + cell.getStringCellValue());
                                        cellNameValidation(cell ,
                                                tableSecondLevelHeaders.get(cell.getAddress().toString().trim()) ,
                                                errorList
                                        );
                                    }
                                }
                            } else {
                                log.info("Cell - getting product - " + cell.getAddress() + " : " + cell.getStringCellValue() + " : " + cell.getColumnIndex());
                                try {
                                    if (!isCropValid(cell.getStringCellValue())) {
                                        errorList.add(cell.getAddress() + " must be valid existing product");
                                    }
                                } catch (IllegalStateException e) {
                                    errorList.add(cell.getAddress() + " must be Text");
                                }
                            }
                        }
                    }
                    if (rowNumber == 5) {
                        log.debug("reading sub sub headers");
                        Iterator<Cell> cellIterator = row.cellIterator();
                        int index = 1;
                        while (cellIterator.hasNext()) {
                            Cell cell = cellIterator.next();
                            if (cell.getCellType() == CellType.BLANK) {
                                continue;
                            }
                            index = ((cell.getColumnIndex() - excelFilePropertiesConfig.getStartPoint()) % 4)+1;
                            String title = tableThirdLevelHeaders.get(index);
                            try{
                                if (!cell.getStringCellValue().trim().equals(title.trim())){
                                    errorList.add(cell.getAddress() + " must be "+title);
                                }
                            }catch (IllegalStateException e){
                                errorList.add(cell.getAddress() + " must be Text");
                            }
                            log.info("Cell " + cell.getAddress() + " : " + cell.getStringCellValue());
                        }
                        if (index < 4){
                            errorList.add(tableThirdLevelHeaders.get(index)+" must added to end of table headers");
                        }
                    }
                    if (rowNumber > 5) {
                        log.debug("reading sub sub headers");
//                        Iterator<Cell> cellIterator = row.cellIterator();
//                        while (cellIterator.hasNext()){
//                            Cell cell = cellIterator.next();
//                            if (cell.getCellType() == CellType.BLANK){
//                                continue;
//                            }
//                        }
                        log.info("values " + row.getCell(0).getNumericCellValue() + " : " + row.getCell(7).getStringCellValue());
                        long cuid = BigDecimal.valueOf(row.getCell(0).getNumericCellValue()).longValue();
                        String farmerCode = row.getCell(2).getStringCellValue();
                        log.info("values " + row.getCell(0).getNumericCellValue() + " : " + cuid);
                        String plotCode = row.getCell(7).getStringCellValue();
                        if (userMap.containsKey(farmerCode)) {
                            if (userMap.get(farmerCode).contains(plotCode)) {
                                errorList.add(row.getCell(7).getAddress() + " contains duplicate plot value");
                            }
                        } else {
                            ArrayList<String> strings = new ArrayList<>();
                            strings.add(plotCode);
                            userMap.put(farmerCode , strings);
                        }
                        try {
                            double area = row.getCell(8).getNumericCellValue();
                        }catch (IllegalStateException e){
                            errorList.add(row.getCell(8).getAddress()+" must be number");
                        }
                        String cellValue = dataFormatter.formatCellValue(row.getCell(12));
                        try {
                            log.info(row.getCell(12).getAddress() + " date " + row.getCell(12).getNumericCellValue() + ", " + cellValue);
                        } catch (IllegalStateException e) {
                            errorList.add(row.getCell(12).getAddress() + " contains illegal format of date");
                        }
                    }
                }
            } catch (IOException e) {
                log.error("error occurred while loading excel file " + e.getMessage());
                errorList.add("error occurred while loading excel file "+ e.getMessage());
            } catch (EncryptedDocumentException e) {
                log.error("encrypted document " + e.getMessage());
                errorList.add("encrypted document");
            }
        } catch (IOException e) {
            log.error("error occurred " + e.getMessage());
            e.printStackTrace();
            errorList.add("error occurred "+e.getMessage());
        }
        log.error("errors while reading file " + errorList);
        endTime = System.currentTimeMillis();
        log.info("task end : " + (endTime - startTime) + "ms");
        if (!errorList.isEmpty()) {
            return ResponseEntity.badRequest().body(errorList);
        }
        return null;
    }

    private boolean isCropValid(String stringCellValue) {
        return true;
    }


    private void cellNameValidation(Cell cell ,
                                    String name ,
                                    List<String> errorList) {
        if (!cell.toString().equalsIgnoreCase(name)) {
            errorList.add(cell.getAddress() + " must be " + name);
        }
        log.info("cell 1 " + cell);
    }



}
