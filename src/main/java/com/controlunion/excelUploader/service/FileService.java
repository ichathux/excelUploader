package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.config.ExcelFilePropertiesConfig;
import com.controlunion.excelUploader.model.Crop;
import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.repository.CropRepository;
import com.controlunion.excelUploader.repository.FarmerlistRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class FileService {

    private long startTime;
    private long endTime;
    private int cropsSubHeadersRowNumber = Integer.MAX_VALUE;

    private final ExcelFilePropertiesConfig excelProperties;

    private final CropRepository cropRepository;

    private final FarmerlistRepository farmerlistRepository;

    public FileService(ExcelFilePropertiesConfig excelFilePropertiesConfig,
                       CropRepository cropRepository,
                       FarmerlistRepository farmerlistRepository) {

        this.excelProperties = excelFilePropertiesConfig;
        this.cropRepository = cropRepository;
        this.farmerlistRepository = farmerlistRepository;
    }

    @SneakyThrows
    //    making file object
    public ResponseEntity uploadExcelFile(MultipartFile file) {
        log.info("File recieved " + file.getOriginalFilename());
        startTime = System.currentTimeMillis();
        List<String> errorList = new ArrayList<>();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is not present");
        }
        return readFile(file, errorList);

    }

    //    start reading file
    private ResponseEntity readFile(MultipartFile temp,
                                    List<String> errorList) {

        Map<Integer, String> tableTopLevelHeaders = excelProperties.getHeader1(); // store main headers
        Map<Integer, String> tableSecondLevelHeaders = excelProperties.getHeader2(); // store 2nd level
        Map<Integer, Crop> cropMapping = new HashMap<>(); // store crops in excel sheet with cell address

        HashMap<String, ArrayList<String>> userMap = new HashMap<>(); // store farmer's plots
        List<FarmerList> farmerLists = new ArrayList<>();

        try (InputStream inputStream = temp.getInputStream();) {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0); // Assuming the data is on the first sheet
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            int rowNumber = 0;
            int lastrowNo = 0;
//            int skippedRows = 0;
            looping:
            for (Row row : sheet) {
                log.info("**********reading row : " + rowNumber + " excel row : " + row.getRowNum());

                if ((row.getRowNum() - lastrowNo) > 1) {
                    log.info("Skipped rows - lastrow " + lastrowNo + " : current row " + rowNumber);
                    emptyRowCheckerLoop:
                    for (int i = row.getRowNum() - 1; i > lastrowNo; i--) {
                        try{
                            Iterator<Cell> cellIterator = sheet.getRow(i).cellIterator();
                            while (cellIterator.hasNext()) {
                                Cell cell = cellIterator.next();
                                if (cell.getCellType() != CellType.BLANK) {
                                    break emptyRowCheckerLoop;
                                }
                            }
                            log.info("Empty row found in row : " + (i + 1));
                            errorList.add("Row : " + (i + 1) + " containing empty row");
                        }catch (NullPointerException e){
                            log.info("Empty row found in row : " + (i + 1));
                            errorList.add("Row : " + (i + 1) + " containing empty row");
                        }

                    }
                }
                try {
                    if (row.getCell(0).getCellType().equals(CellType.BLANK)) {
                        continue looping;
                    }
                } catch (NullPointerException e) {
//                    continue looping;
                    System.out.println(cropsSubHeadersRowNumber);
                    if (row.getRowNum() != cropsSubHeadersRowNumber){
                        log.error("null pointer found "+row.getFirstCellNum());
                        errorList.add("row : "+(row.getRowNum()+1) + " First value can't be null");
                    }

                }

                lastrowNo = row.getRowNum();

                switch (rowNumber) {
                    case 0:
                        log.info("validating Project Name : ");
                        Cell cell = row.getCell(0);
                        if (!tableHeaderNameValidation(cell,
                                excelProperties.getAuditDetails().get(rowNumber),
                                errorList
                        )) {
                            errorList.add(cell.getAddress().toString() + " must be contain " + excelProperties.getAuditDetails().get(rowNumber));
                            continue looping;
                        }
                        break;
                    case 1:
                        log.info("validating Project Id : ");
                        cell = row.getCell(0);
                        if (!tableHeaderNameValidation(cell,
                                excelProperties.getAuditDetails().get(rowNumber),
                                errorList
                        )) {
                            errorList.add(cell.getAddress().toString() + " must be contain " + excelProperties.getAuditDetails().get(rowNumber));
                            continue looping;
                        }
                        break;
                    case 2:
                        log.info("validating Year : ");
                        cell = row.getCell(0);
                        try{
                            if (!tableHeaderNameValidation(cell,
                                    excelProperties.getAuditDetails().get(rowNumber),
                                    errorList
                            )) {
                                errorList.add(cell.getAddress().toString() + " must be contain " + excelProperties.getAuditDetails().get(rowNumber));
                                continue looping;
                            }
                        }catch (NullPointerException e){
                            continue looping;
                        }


                        break;
                    case 3:
//                        isInSheetHeaderValidating = false;
//                        isInTableHeaderValidating = true;
                        cell = row.getCell(0);
                        if (!cell.getStringCellValue().equals(excelProperties.getTableStartHeader())){
                            errorList.add(cell.getAddress().toString() + " must be contain " + tableTopLevelHeaders.get(0));
                            continue looping;
                        }
                        log.info("validating 1st level headers start");
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

//                        reading third level headers
//                        log.info("reading sub sub headers");
//                        validateCropsSubHeaders(errorList,
//                                tableThirdLevelHeaders,
//                                row.cellIterator());
//                        isInTableHeaderValidating = false;
//                        isInUserDataHeaderValidating = true;
                        log.info("*****************************************************************************************************************");
//                        rowNumber++;
                        break;
                    default:
//                        raed user input data
                        log.info("validating user datas");
                        readUserInputData(errorList,
                                userMap,
                                row.cellIterator(),
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
//        System.out.println(farmerLists);
//        saveFarmerListOnDB(farmerLists);
        return ResponseEntity.ok().body(farmerLists);
    }

    private void saveFarmerListOnDB(List<FarmerList> farmerLists) {
        farmerlistRepository.saveAll(farmerLists);
    }

//    //    @SneakyThrows
//    //    validating crops sub headers
//    private void validateCropsSubHeaders(List<String> errorList,
//                                         Map<Integer, String> tableThirdLevelHeaders,
//                                         Iterator<Cell> cellIterator) {
//        int index = 1;
//        while (cellIterator.hasNext()) {
//            Cell cell = cellIterator.next();
////            check cell is blank
//            if (cell.getCellType() == CellType.BLANK) {
//                continue;
//            }
////            indexing crops sub header
//            index = ((cell.getColumnIndex() - excelProperties.getStartPoint()) % 4) + 1;
//            String title = tableThirdLevelHeaders.get(index);
//            try {
////                check table header is contained provided list
//                if (!cell.getStringCellValue().trim().equals(title.trim())) {
//                    errorList.add(cell.getAddress() + " must be contain " + title);
//                }
//            } catch (IllegalStateException e) {
//                errorList.add(cell.getAddress() + " must be Text");
//            }
//            log.info("Cell " + cell.getAddress() + " : " + cell.getStringCellValue());
//        }
////        each crops have 4 sub headers.
//        if (index < 4) {
//            errorList.add(tableThirdLevelHeaders.get(index) + " must added to end of table headers");
//        }
//    }

    private void validateCropsSub(XSSFSheet sheet, int rowNumber, Crop crop, int colNumber, List<String> errorList) {
        XSSFRow row = sheet.getRow(rowNumber);
        if (row != null) {
            for (int i = colNumber; i < colNumber + 4; i++) {
                XSSFCell cell = row.getCell(i);
                if (!excelProperties.getHeader3().get(i - colNumber).equals(cell.getStringCellValue().trim())) {
                    errorList.add(cell.getAddress() + " : must be " + " : " + excelProperties.getHeader3().get(i - colNumber));
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
                    if (!tableHeaderNameValidation(cell,
                            tableTopLevelHeaders.get(cell.getAddress().toString().trim()),
                            errorList
                    )) {
                        errorList.add(cell.getAddress().toString() + " : must be contain " + tableTopLevelHeaders.get(cell.getColumnIndex()));
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
                        if (!tableHeaderNameValidation(cell,
                                tableSecondLevelHeaders.get(cell.getColumnIndex()),
                                errorList
                        )) {

                            errorList.add(cell.getAddress().toString() + ": must be contain " + tableSecondLevelHeaders.get(cell.getColumnIndex()));

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
                validateCrops(errorList, cropMapping, cell, sheet);
            }
        }
    }

    //    validating crops implement
    private void validateCrops(List<String> errorList,
                               Map<Integer, Crop> cropMapping,
                               Cell cell, XSSFSheet sheet) {
        try {
            Crop crop = getCrop(cell.getStringCellValue().trim());
            if (crop == null) {
                errorList.add(cell.getAddress() + " must be valid existing crop - " + cell.getStringCellValue());
                log.error("crop not valid");
            } else {
                cropMapping.put(cell.getColumnIndex(), crop);
                log.info("crop " + crop.getCropName() + " : passed");
                cropsSubHeadersRowNumber = cell.getRowIndex() + 1;
                validateCropsSub(sheet, cropsSubHeadersRowNumber, crop, cell.getColumnIndex(), errorList);

            }
        } catch (IllegalStateException e) {
            errorList.add(cell.getAddress() + " must be Text");
            e.printStackTrace();
        }

    }


    private void readUserInputData(List<String> errorList,
                                   HashMap<String, ArrayList<String>> userMap,
                                   Iterator<Cell> cellIterator,
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
                log.info("**********Formular*********" + cell.getAddress());
                try {
                    CellValue cellValue = evaluator.evaluate(cell);
                    String result = cellValue.formatAsString();
                    log.info("result of formula : " + result);
                    cell.setCellValue(result);
                } catch (FormulaParseException e) {
                    errorList.add(cell.getAddress() + " invalid formula found");
                }

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
                        log.info("Unit Number for EU / JAS : " + unitNoEUJAS);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 2:
                    try {
                        farmerCode = cell.getStringCellValue().trim();
                        log.info("Farmer Code for EU / JAS : " + farmerCode);
                        userMap.putIfAbsent(cell.getStringCellValue(), new ArrayList<>());
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 3:
                    try {
                        unitNoNOP = cell.getStringCellValue().trim();
                        log.info("Unit Number for NOP : " + unitNoNOP);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 4:
                    try {
                        farCodeNOP = cell.getStringCellValue().trim();
                        log.info("farmer code for NOP : " + farCodeNOP);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 5:
                    try {
                        farmerName = cell.getStringCellValue().trim();
                        log.info("Farmer Name : " + farmerName);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 6:
                    try {
                        farmName = cell.getStringCellValue().trim();
                        log.info("Name of the Farm : " + farmName);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 7:
                    try {
                        plotCode = cell.getStringCellValue().trim();
                        log.info("Checking duplicate plots for farmer : " + farmerCode + " with " + plotCode);
                        if (userMap.containsKey(farmerCode)) {
                            if (userMap.get(farmerCode).contains(plotCode)) {
                                errorList.add(cell.getAddress() + " contains duplicate plot value : " + cell.getStringCellValue());
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
                        log.info("Starting date of Conversion period (yyyy-mm-dd) : " + dateConversion);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format of date. It must be " + excelProperties.getDateFormat());
                    }
                    break;
                case 18:
                    try {
                        dateOrganic = cell.getDateCellValue();
                        log.info("Starting date of Organic Period (yyyy-mm-dd) : " + dateOrganic);

                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format of date. It must be " + excelProperties.getDateFormat());
                    }
                    break;
                case 19:
                    try {
                        eujasField = cell.getStringCellValue().trim();
                        log.info("Field Status EU/JAS ic1/ic2/ic3/org : " + eujasField);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 20:
                    try {
                        eujasHarvest = cell.getStringCellValue().trim();
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
    }

    //    get crop from db
    private Crop getCrop(String name) {
        log.info("checking crop " + name + " on db");
        return cropRepository.findByCropName(name).orElse(null);
    }


    private boolean tableHeaderNameValidation(Cell cell,
                                              String name,
                                              List<String> errorList) {
        if (!cell.toString().equalsIgnoreCase(name)) {
//            errorList.add(cell.getAddress() + ":invalid table header " + name);
//            errorList.add(cell.getAddress().toString() + " must be contain " + name);
            return false;
        }
        log.info(name + " validating pass");
        log.info(cell.getAddress() + " " + cell);
        return true;
    }

    public void getAllCrops() {
        System.out.println(cropRepository.findAll());
    }
}
