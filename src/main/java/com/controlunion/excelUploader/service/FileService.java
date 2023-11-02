package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.config.ExcelFilePropertiesConfig;
import com.controlunion.excelUploader.dto.FarmerList_mandatory_fieldsDto;
import com.controlunion.excelUploader.enums.Errors;
import com.controlunion.excelUploader.model.Crop;
import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.repository.FarmerlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileService {

    private long startTime;
    private long endTime;
    private int cropsSubHeadersRowNumber = Integer.MAX_VALUE;
    private int tableWidth;

    private final ExcelFilePropertiesConfig excelProperties;
    private final FarmerlistRepository farmerlistRepository;

    private final CropService cropService;


    //    @SneakyThrows
    //    making file object
    public ResponseEntity uploadExcelFile(MultipartFile file) {
        log.info("File recieved " + file.getOriginalFilename());
        startTime = System.currentTimeMillis();
        List<String> errorList = new ArrayList<>();
        if (file.isEmpty()) {
            errorList.add("File is not present");
            return ResponseEntity.badRequest().body("File is not present");
        }
        if (!isFileExcel(file)) {
            errorList.add("File format not support. It must be .xls or .xlsx");
            return ResponseEntity.badRequest().body(errorList);
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

    //    start reading file
    private ResponseEntity readFile(MultipartFile temp,
                                    List<String> errorList) {

        Map<Integer, String> tableTopLevelHeaders = excelProperties.getHeader1(); // store main headers
        Map<Integer, String> tableSecondLevelHeaders = excelProperties.getHeader2(); // store 2nd level
        Map<Integer, Crop> cropMapping = new HashMap<>(); // store crops in excel sheet with cell address
        HashMap<String, ArrayList<String>> userMap = new HashMap<>(); // store farmer's plots
        List<FarmerList> farmerLists = new ArrayList<>();

        try (InputStream inputStream = temp.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0); // Assuming the data is on the first sheet
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            int rowNumber = 0;
            int lastrowNo = 0;

            looping:
            for (Row row : sheet) {
                log.info("**********reading row : " + rowNumber + " excel row : " + row.getRowNum());
                System.out.println(row);


                if (!checkRowContainNullValues(row)){
                    continue looping;
                }
                System.out.println("processing-1");
                checkIsItEmptyRow(errorList, sheet, lastrowNo, row);
                System.out.println("processing-2");
                if (row.getLastCellNum() - row.getFirstCellNum() == 1) {
                    if (row.getCell(row.getFirstCellNum()).getCellType().equals(CellType.BLANK)) {
                        System.out.println("empty row " + (row.getRowNum() + 1));
                        errorList.add("Row : " + (row.getRowNum() + 1) + " " + Errors.EMPTY_ROW.getName());
                        continue looping;
                    }

                }
                System.out.println("processing-3");
                lastrowNo = row.getRowNum();
                if (rowNumber == 0){
                    Cell cell = row.getCell(0);
                    if (!isTableHeaderNameInvalid(cell,
                            excelProperties.getAuditDetails().get(rowNumber))) {
                        rowNumber++;
                        continue looping;
                    }else{
                        errorList.add(cell.getAddress().toString() + " must be " + excelProperties.getAuditDetails().get(rowNumber));
                        rowNumber++;
                        continue looping;
                    }
                }
                if (rowNumber == 1){
                    Cell cell = row.getCell(0);
                    if (!isTableHeaderNameInvalid(cell,
                            excelProperties.getAuditDetails().get(rowNumber))) {
                        rowNumber++;
                        continue looping;
                    }else{
                        errorList.add(cell.getAddress().toString() + " must be " + excelProperties.getAuditDetails().get(rowNumber));
                        rowNumber++;
                        continue looping;
                    }
                }
                if (rowNumber == 2){
                    Cell cell = row.getCell(0);
                    if (!isTableHeaderNameInvalid(cell,
                            excelProperties.getAuditDetails().get(rowNumber))) {
                        rowNumber++;
                        continue looping;
                    }else{
                        errorList.add(cell.getAddress().toString() + " must be " + excelProperties.getAuditDetails().get(rowNumber));
                        rowNumber++;
                        continue looping;
                    }
                }

                switch (rowNumber) {
//                    case 0:
//                        log.info("validating Project Name : ");
//                        Cell cell = row.getCell(0);
//                        if (isTableHeaderNameInvalid(cell,
//                                excelProperties.getAuditDetails().get(rowNumber))) {
//                            if (cell.getCellType() == CellType.STRING) {
//                                if (!isTableHeaderNameInvalid(cell, excelProperties.getAuditDetails().get(row.getRowNum()))) {
//                                    errorList.add(cell.getAddress().toString() + " must be " + excelProperties.getAuditDetails().get(rowNumber));
//                                }
//                            } else {
//                                errorList.add(cell.getAddress().toString() + " must be " + excelProperties.getAuditDetails().get(rowNumber));
//                                continue looping;
//                            }
//
//                        }
//                        break;
//                    case 1:
//                        log.info("validating Project Id : ");
//                        cell = row.getCell(0);
//                        if (isTableHeaderNameInvalid(cell,
//                                excelProperties.getAuditDetails().get(rowNumber))) {
//                            errorList.add(cell.getAddress().toString() + " must be " + excelProperties.getAuditDetails().get(rowNumber));
//                            continue looping;
//                        }
//                        break;
//                    case 2:
//                        log.info("validating Year : ");
//                        cell = row.getCell(0);
//                        if (isTableHeaderNameInvalid(cell,
//                                excelProperties.getAuditDetails().get(rowNumber))) {
//                            errorList.add(cell.getAddress().toString() + " must be " + excelProperties.getAuditDetails().get(rowNumber));
//                            continue looping;
//                        }
//
//                        break;
                    case 3:
                        log.info("validating 1st level headers start");
                        if (row.getPhysicalNumberOfCells() < tableTopLevelHeaders.size()) {

//                            System.out.println("You need to add tables headers in this row");
                            errorList.add("Row " + (row.getRowNum() + 1) + " invalid value found ");
                            continue looping;
                        }
                        validate1stLevelHeaders(errorList,
                                tableTopLevelHeaders,
                                row.cellIterator());
                        log.info("validating 1st level headers done");
                        log.info("*****************************************************************************************************************");
                        break;
                    case 4:
//                        read second level headers
                        log.info("validating sub headers start");
                        validate2ndLevelHeaders(errorList,
                                tableSecondLevelHeaders,
                                cropMapping,
                                row.cellIterator(),
                                sheet);
                        log.info("validating sub headers done");
                        log.info("*****************************************************************************************************************");

                        break;
                    case 5:
                        log.info("*****************************************************************************************************************");
                        break;
                    default:
//                        raed user input data
                        log.info("validating user datas");

                        readUserInputData(errorList,
                                userMap,
                                row,
                                cropMapping,
                                evaluator,
                                farmerLists);
                        log.info("*****************************************************************************************************************");

                        break;

                }
                rowNumber++;

            }
        } catch (IOException e) {
            log.error("error occurred while loading excel file " + e.getMessage());
            e.printStackTrace();
            errorList.add("error occurred while loading excel file " + e.getMessage());
        } catch (EncryptedDocumentException e) {
            log.error("encrypted document " + e.getMessage());
            e.printStackTrace();
            errorList.add("encrypted document");
        } catch (Exception e) {
            e.printStackTrace();
        }

        endTime = System.currentTimeMillis();
        log.info("task end : " + (endTime - startTime) + "ms");

        if (!errorList.isEmpty()) {
            log.error("errors while reading file ");
//            errorList.stream().forEach(System.out::println);
            return ResponseEntity.badRequest().body(errorList);
        }
//        saveFarmerListOnDB(farmerLists);
        return ResponseEntity.ok().body(farmerLists);
    }

    private boolean checkRowContainNullValues(Row row) {
        System.out.println("Checking null cells available");
        Iterator<Cell> cells = row.cellIterator();
        boolean notAEmptyRow = false;

        while (cells.hasNext()){
            System.out.println("checking");
            Cell cell = cells.next();
            System.out.println(cell.getCellType());
            if (cell.getCellType() != CellType.BLANK){
                return true;
            }
        }
        System.out.println("Empty row found while reading whole ");
        return notAEmptyRow;
    }

    private void checkIsItEmptyRow(List<String> errorList, XSSFSheet sheet, int lastRowNo, Row row) {
        if ((row.getRowNum() - lastRowNo) > 1) {
            log.info("Skipped rows - lastrow " + lastRowNo + " : current row " + row.getRowNum());
            emptyRowCheckerLoop:
            for (int i = lastRowNo + 1; i <= row.getRowNum(); i++) {
                try {
                    Iterator<Cell> cellIterator = sheet.getRow(i).cellIterator();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        if (cell.getCellType() != CellType.BLANK) {
                            break emptyRowCheckerLoop;
                        }
                    }
                    log.info("Empty row found in row : " + (i + 1));
                    errorList.add("Row : " + (i + 1) + " " + Errors.EMPTY_ROW.getName());
                } catch (NullPointerException e) {
                    log.info("Empty row found in row : " + (i + 1));
                    errorList.add("Row : " + (i + 1) + " " + Errors.EMPTY_ROW.getName());
                }
            }
        }


    }


    private void validateCropsSub(XSSFRow row, Crop crop, int colNumber, List<String> errorList) {
        log.info("Validating crops tables headers " + crop.getCropName());

        if (row != null) {
            for (int i = colNumber; i < colNumber + 4; i++) {
                XSSFCell cell = row.getCell(i);
                if (!excelProperties.getHeader3().get(i - colNumber).equals(cell.getStringCellValue().trim())) {
                    errorList.add(cell.getAddress() + " : " + Errors.INVALID_TABLE_HEADER.getName() + " : " + excelProperties.getHeader3().get(i - colNumber));
                    log.error((i - colNumber) + " : " + cell.getStringCellValue() + " : " + crop.getCropName() + " : failed");
                } else {
                    log.info((i - colNumber) + " : " + cell.getStringCellValue() + " : " + crop.getCropName() + " : passed");
                }
            }
        }

    }

    //   validating headers like Farmer Details, Farm Details, Location information	..etc
    private void validate1stLevelHeaders(List<String> errorList,
                                         Map<Integer, String> tableTopLevelHeaders,
                                         Iterator<Cell> cellIterator) {
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            if (cell.getCellType() == CellType.BLANK) {
                continue;
            }
            if (cell.getStringCellValue().trim().equals("")) {
                continue;
            }
            log.info("validating validate1stLevelHeaders: " + cell.getColumnIndex());
            log.info("validating : Not a BLANK cell : " + cell.getColumnIndex() + " : value : " + cell.getStringCellValue());
            if (tableTopLevelHeaders.containsKey(cell.getColumnIndex())) {
                log.info("contained " + cell.getColumnIndex() + " column in properties : " + tableTopLevelHeaders.get(cell.getColumnIndex()));
                if (!tableTopLevelHeaders
                        .get(cell.getColumnIndex())
                        .equals(cell.getStringCellValue().trim())) {
                    log.error("error in " + cell.getAddress() + ", " + cell.getStringCellValue());
                    if (isTableHeaderNameInvalid(cell,
                            tableTopLevelHeaders.get(cell.getColumnIndex())
                    )) {
                        errorList.add(cell.getAddress().toString() + " : " + Errors.INVALID_TABLE_HEADER.getName() + " : " + cell.getStringCellValue());
                    }
                } else {
                    log.info("value : " + cell.getStringCellValue() + ", cell : " + cell.getAddress() + ", passed");
                }

            } else {
                log.error("not contain required column in properties : " + cell.getColumnIndex());
            }

            log.info("Cell " + cell.getAddress() + " : " + cell.getStringCellValue());
        }
    }

    //    validating second level headers in table like CUID, Unit Number for EU / JAS, Farmer Code for EU / JAS
    private void validate2ndLevelHeaders(List<String> errorList,
                                         Map<Integer, String> tableSecondLevelHeaders,
                                         Map<Integer, Crop> cropMapping,
                                         Iterator<Cell> cellIterator, XSSFSheet sheet) {
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            log.info("validating validate2ndLevelHeaders: " + cell.getColumnIndex());

//            validating fixed headers in table
            if (cell.getColumnIndex() < excelProperties.getStartPoint()) {
                if (cell.getCellType() == CellType.BLANK) {
                    log.error("can't be blank");
                    continue;
                }
                if (tableSecondLevelHeaders.containsKey(cell.getColumnIndex())) {
                    log.info("contained " + cell.getColumnIndex() + " column in properties : " + tableSecondLevelHeaders.get(cell.getColumnIndex()));
                    if (!tableSecondLevelHeaders
                            .get(cell.getColumnIndex())
                            .equals(cell.getStringCellValue().trim())) {
                        log.error("error in " + cell.getAddress() + ", " + cell.getStringCellValue());
                        if (isTableHeaderNameInvalid(cell,
                                tableSecondLevelHeaders.get(cell.getColumnIndex())
                        )) {

                            errorList.add(cell.getAddress().toString() + " : " + Errors.INVALID_TABLE_HEADER.getName() + " : " + cell.getStringCellValue());
                        }
                        log.info("value : " + cell.getStringCellValue() + ", cell : " + cell.getAddress() + ", failed");
                    } else {
                        log.info("value : " + cell.getStringCellValue() + ", cell : " + cell.getAddress() + ", passed");
                    }
                } else {
                    log.error("not contain required column in properties : " + cell.getColumnIndex());
                }
            } else {
                if (cell.getCellType() == CellType.BLANK) {
//                    log.error("can't be blank");
                    continue;
                }
//                validating crops name
                log.info("Cell - getting crop - " + cell.getAddress() + " : " + cell.getStringCellValue() + " : " + cell.getColumnIndex());
                Crop crop = new Crop();
                boolean isValidCrop = validateCrops(cropMapping, cell, crop);
                if (!isValidCrop) {
                    errorList.add(cell.getAddress() + " : " + Errors.CROP_NOT_VALID.getName() + " : " + cell.getStringCellValue());
                } else {
                    System.out.println(cropMapping);
                    System.out.println(cell.getColumnIndex());
                    crop = cropMapping.get(cell.getColumnIndex());
                    log.info("crop " + crop.getCropName() + " : passed");
                    cropsSubHeadersRowNumber = cell.getRowIndex() + 1;

                    XSSFRow row = sheet.getRow(cropsSubHeadersRowNumber);
                    tableWidth = row.getLastCellNum();
                    System.out.println("table width : " + tableWidth);
                    validateCropsSub(row, crop, cell.getColumnIndex(), errorList);
                }
            }
        }
    }

    //    validating crops implement
    private boolean validateCrops(Map<Integer, Crop> cropMapping,
                                  Cell cell,
                                  Crop crop) {
        try {
            crop = cropService.getCropByName(cell.getStringCellValue().trim());
            if (crop == null) {
                log.error("crop not valid");
                return false;
            } else {
                if (cropMapping.values().contains(crop)) {
                    return false;
                }
                cropMapping.put(cell.getColumnIndex(), crop);
                log.info("crop " + crop.getCropName() + " : passed");
                cropsSubHeadersRowNumber = cell.getRowIndex() + 1;
                return true;
            }
        } catch (IllegalStateException e) {
            crop = null;
            e.printStackTrace();
            return false;
        }

    }


    private void readUserInputData(List<String> errorList,
                                   HashMap<String, ArrayList<String>> userMap,
                                   Row row,
                                   Map<Integer, Crop> cropMapping,
                                   FormulaEvaluator evaluator,
                                   List<FarmerList> farmerLists) {

        String unitNoEUJAS = null;
        String unitNoNOP = null;
        String farCodeNOP = null;
        String farmerName = null;
        String farmName = null;
        double totalArea = 0.0;
        String city = null;
        String gps = null;
        Date dateCert = null;
        String aplyRetrospe = null;
        Date dateConversion = null;
        String fertilizer = null;
        String ferUseDate = null;
        String eujasField = null;
        String eujasHarvest = null;
        String usdaHarvest = null;
        String usdaField = null;
        String address = null;
        String certification = null;
        Date dateOrganic = null;
        long cuid = 0;
        String farmerCode = null;
        String plotCode = null;
        Crop crop = new Crop();

        if (cropMapping.keySet().size() == 0) {
            errorList.add("You need add least one crop");
        }
        if (row.getFirstCellNum() != 0 || row.getCell(0).getStringCellValue().trim().equals("")) {
            System.out.println("cuid not present");
        }
        Iterator<Cell> cellIterator = row.cellIterator();

        FarmerList_mandatory_fieldsDto mandatory_fields = new FarmerList_mandatory_fieldsDto();

        while (cellIterator.hasNext()) {

            Cell cell = cellIterator.next();
            if (cell.getColumnIndex() == 0) {
                if (cell.getCellType() == CellType.BLANK) {
                    break;
                }
            }
            if (cell.getCellType() == CellType.BLANK) {
                continue;
            }
            if (cell.getCellType() == CellType.FORMULA) {
                if (processFormula(errorList, evaluator, cell))
                    continue;

            }


            switch (cell.getColumnIndex()) {
                case 0:
                    try {
                        cuid = BigDecimal.valueOf(cell.getNumericCellValue()).longValue();
                        log.info("cuid : " + cuid);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 1:
                    try {
                        unitNoEUJAS = cell.getStringCellValue().trim();
                        mandatory_fields.setUnitNoEUJAS(true);
                        log.info("Unit Number for EU / JAS : " + unitNoEUJAS);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 2:
                    try {
                        farmerCode = cell.getStringCellValue().trim();
                        mandatory_fields.setFarCodeEUJAS(true);
                        log.info("Farmer Code for EU / JAS : " + farmerCode);
                        userMap.putIfAbsent(cell.getStringCellValue(), new ArrayList<>());
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 3:
                    try {
                        unitNoNOP = cell.getStringCellValue().trim();
                        mandatory_fields.setUnitNoNOP(true);
                        log.info("Unit Number for NOP : " + unitNoNOP);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 4:
                    try {
                        farCodeNOP = cell.getStringCellValue().trim();
                        mandatory_fields.setFarCodeNOP(true);
                        log.info("farmer code for NOP : " + farCodeNOP);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 5:
                    try {
                        farmerName = cell.getStringCellValue().trim();
                        mandatory_fields.setFarmerName(true);
                        log.info("Farmer Name : " + farmerName);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 6:
                    try {
                        farmName = cell.getStringCellValue().trim();
                        mandatory_fields.setFarmName(true);
                        log.info("Name of the Farm : " + farmName);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 7:
                    try {
                        plotCode = cell.getStringCellValue().trim();
                        mandatory_fields.setPlotCode(true);
                        log.info("Checking duplicate plots for farmer : " + farmerCode + " with " + plotCode);
                        if (userMap.containsKey(farmerCode)) {
                            if (userMap.get(farmerCode).contains(plotCode)) {
                                errorList.add(cell.getAddress() + " : " + Errors.DUPLICATE_PLOT_VALUES.getName() + " : " + cell.getStringCellValue());
                            } else {
                                userMap.get(farmerCode).add(plotCode);
                            }
                        }
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 8:
                    try {
                        totalArea = cell.getNumericCellValue();
                        mandatory_fields.setTotalArea(true);
                        log.info("Total Area (Ha) : " + totalArea);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " must be number");
                    }
                    break;
                case 9:
                    try {
                        gps = cell.getStringCellValue().trim();
                        log.info("GPS : " + gps);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 10:
                    try {
                        address = cell.getStringCellValue().trim();
                        log.info("Address/Village : " + address);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 11:
                    try {
                        city = cell.getStringCellValue().trim();
                        mandatory_fields.setCity(true);
                        log.info("City : " + city);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 12:
                    try {
                        dateCert = cell.getDateCellValue();
                        log.info("Application date for certification (yyyy-mm-dd) : " + dateCert);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format of date. It must be " + excelProperties.getDateFormat());
                    }
                    break;
                case 13:
                    try {
                        aplyRetrospe = cell.getStringCellValue().trim();
                        log.info("Applying for Retrospective consideration (Yes/No) : " + aplyRetrospe);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 14:
                    try {
                        certification = cell.getStringCellValue().trim();
                        log.info("Certifications : " + certification);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 15:
                    try {
                        fertilizer = cell.getStringCellValue().trim();
                        log.info("Types of fertilizer, pesticide used : " + fertilizer);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 16:
                    try {
                        ferUseDate = cell.getStringCellValue();
                        log.info("Last date of use : " + cell.getStringCellValue());
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 17:
                    try {
                        dateConversion = cell.getDateCellValue();
                        mandatory_fields.setStartingDateCon(true);
                        log.info("Starting date of Conversion period (yyyy-mm-dd) : " + dateConversion);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format of date. It must be " + excelProperties.getDateFormat());
                    }
                    break;
                case 18:
                    try {
                        dateOrganic = cell.getDateCellValue();
                        mandatory_fields.setStartingDateOrg(true);
                        log.info("Starting date of Organic Period (yyyy-mm-dd) : " + dateOrganic);

                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format of date. It must be " + excelProperties.getDateFormat());
                    }
                    break;
                case 19:
                    try {
                        eujasField = cell.getStringCellValue().trim();
                        mandatory_fields.setEujasField(true);
                        log.info("Field Status EU/JAS ic1/ic2/ic3/org : " + eujasField);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 20:
                    try {
                        eujasHarvest = cell.getStringCellValue().trim();
                        mandatory_fields.setEujasHarvest(true);
                        log.info("Harvest status EU/JAS conv/ic/org : " + eujasHarvest);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 21:
                    try {
                        usdaField = cell.getStringCellValue().trim();
                        log.info("Field status NOP ic1/ic2/ic3/org : " + usdaField);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 22:
                    try {
                        usdaHarvest = cell.getStringCellValue().trim();
                        log.info("Harvest status NOP conv/org : " + usdaHarvest);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
//                    reading crop section
                default:
                    try {
//                        log.info("starting read crop data "+cell.getNumericCellValue());
                        if (cropMapping.containsKey(cell.getColumnIndex())) {
                            crop = cropMapping.get(cell.getColumnIndex());
                            log.info("prod " + crop.getCropName() + cell.getNumericCellValue());
                        }
//                        log.info(crop.getCropName() + " cell-type : "+cell.getCellType().toString()+" value : "+ cell.getNumericCellValue());
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
            }
        }
//        System.out.println(mandatory_fields);
        checkMandatoryFieldsExists(mandatory_fields, errorList, (row.getRowNum() + 1));
        FarmerList farmerList = FarmerList.builder()
                .cuFarmerID(cuid)
                .unitNoEUJAS(unitNoEUJAS)
                .farCodeEUJAS(farmerCode)
                .unitNoNOP(unitNoNOP)
                .farCodeNOP(farCodeNOP)
                .farmerName(farmerName)
                .farmName(farmName)
                .plotCode(plotCode)
                .totalArea(totalArea)
                .gps(gps)
                .address(address)
                .city(city)
                .dateCert(dateCert)
                .aplyRetrospe(aplyRetrospe == "yes" ? 1 : 0)
                .certification(certification)
                .fertilizer(fertilizer)
                .ferUseDate(ferUseDate)
                .dateConversion(dateConversion)
                .dateOrganic(dateOrganic)
                .eujasField(eujasField)
                .eujasHarvest(eujasHarvest)
                .usdaField(usdaField)
                .usdaHarvest(usdaHarvest).build();
        farmerLists.add(farmerList);
    }

    private boolean processFormula(List<String> errorList, FormulaEvaluator evaluator, Cell cell) {
        log.info("**********Formular*********" + cell.getAddress());
        try {
            CellValue cellValue = evaluator.evaluate(cell);
            String result = cellValue.formatAsString();
            log.info("result of formula : " + result);
            cell.setCellValue(result);
        } catch (IllegalStateException | FormulaParseException e) {
            errorList.add(cell.getAddress() + " : " + Errors.INVALID_FORMULA.getName());
            log.error(cell.getAddress() + " " + Errors.INVALID_FORMULA.getName());
            return true;
        } catch (RuntimeException e) {
            errorList.add(cell.getAddress() + " : " + Errors.INVALID_FORMULA.getName());
            log.error("cell : " + cell.getAddress() + " " + Errors.INVALID_FORMULA.getName());
            return true;
        }
        return false;
    }

    private void checkMandatoryFieldsExists(FarmerList_mandatory_fieldsDto mandatory_fields,
                                            List<String> errorList,
                                            int row) {
        if (!mandatory_fields.isUnitNoEUJAS()) {
            errorList.add("Row : " + row + " : Unit Number for EU / JAS is mandatory.");
        }
        if (!mandatory_fields.isFarCodeEUJAS()) {
            errorList.add("Row : " + row + " : Farmer Code for EU / JAS is mandatory.");

        }
        if (!mandatory_fields.isUnitNoNOP()) {
            errorList.add("Row : " + row + " : Unit Number for NOP is mandatory.");

        }
        if (!mandatory_fields.isFarCodeNOP()) {
            errorList.add("Row : " + row + " : farmer code for NOP is mandatory.");

        }
        if (!mandatory_fields.isFarmerName()) {
            errorList.add("Row : " + row + " : Farmer Name is mandatory.");

        }
        if (!mandatory_fields.isFarmName()) {
            errorList.add("Row : " + row + " : Name of the Farm is mandatory.");

        }
        if (!mandatory_fields.isPlotCode()) {
            errorList.add("Row : " + row + " : Plot code is mandatory.");

        }
        if (!mandatory_fields.isTotalArea()) {
            errorList.add("Row : " + row + " : Total Area (Ha) is mandatory.");

        }
        if (!mandatory_fields.isCity()) {
            errorList.add("Row : " + row + " : City is mandatory.");

        }
        if (!mandatory_fields.isStartingDateCon()) {
            errorList.add("Row : " + row + " : Starting date of Conversion period (yyyy-mm-dd) is mandatory.");

        }
        if (!mandatory_fields.isStartingDateOrg()) {
            errorList.add("Row : " + row + " : Starting date of Organic Period (yyyy-mm-dd) is mandatory.");

        }
        if (!mandatory_fields.isEujasField()) {
            errorList.add("Row : " + row + " : Field Status EU/JAS ic1/ic2/ic3/org is mandatory.");

        }
        if (!mandatory_fields.isEujasHarvest()) {
            errorList.add("Row : " + row + " : Harvest status EU/JAS conv/ic/org is mandatory.");

        }

    }


    private boolean isTableHeaderNameInvalid(Cell cell,
                                             String name) {

        try {
            System.out.println("checking "+cell.getStringCellValue()+" with "+name);
            if (!cell.toString().equalsIgnoreCase(name)) {
                return true;
            }
            log.info(name + " validating pass");
            log.info(cell.getAddress() + " " + cell);
            return false;
        } catch (NullPointerException e) {
            System.out.println("Null value found");
            return false;
        }

    }


}
