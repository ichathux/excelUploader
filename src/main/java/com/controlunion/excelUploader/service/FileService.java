package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.config.ExcelFilePropertiesConfig;
import com.controlunion.excelUploader.model.Crop;
import com.controlunion.excelUploader.repository.CropRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;

@Service
@Slf4j
public class FileService {
    private long startTime;
    private long endTime;
    @Autowired
    private ExcelFilePropertiesConfig excelProperties;
    @Autowired
    private CropRepository cropRepository;

    public FileService(ExcelFilePropertiesConfig excelFilePropertiesConfig) {
        this.excelProperties = excelFilePropertiesConfig;
    }

    //    making file object
    public ResponseEntity uploadExcelFile(MultipartFile file) {
        log.info("File recieved " + file.getOriginalFilename());
        startTime = System.currentTimeMillis();
        List<String> errorList = new ArrayList<>();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is not present");
        }
        try {
            File temp = File.createTempFile("temp" , null);
            file.transferTo(temp);
            return readFile(temp , errorList);
        } catch (IOException e) {
            errorList.add("error occurred " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error occurred from copying file");
        }

    }

    //    start reading file
    private ResponseEntity readFile(File temp ,
                                    List<String> errorList) {

        Map<String, String> tableTopLevelHeaders = excelProperties.getHeader1(); // store main headers
        Map<String, String> tableSecondLevelHeaders = excelProperties.getHeader2(); // store 2nd level headers
        Map<Integer, String> tableThirdLevelHeaders = excelProperties.getHeader3(); // store crops headers
        Map<Integer, Crop> cropMapping = new HashMap<>(); // store crops in excel sheet with cell address
        HashMap<String, ArrayList<String>> userMap = new HashMap<>(); // store farmer's plots

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(temp.toPath()))) {
            Sheet sheet = workbook.getSheetAt(0); // Assuming the data is on the first sheet
            for (Row row : sheet) {
                if (row.getCell(0) == null) {
                    break;
                }
                int rowNumber = row.getRowNum();
                switch (rowNumber) {
                    case 0:
                        cellNameValidation(row.getCell(0) ,
                                "Project Name" ,
                                errorList
                        );
                        break;
                    case 1:
                        cellNameValidation(row.getCell(0) ,
                                "Project Id" ,
                                errorList
                        );
                        break;
                    case 2:
                        cellNameValidation(row.getCell(0) ,
                                "Year" ,
                                errorList
                        );
                        break;
                    case 3:
                        log.debug("reading headers");
//                        read top level headers
                        validate1stLevelHeaders(errorList ,
                                tableTopLevelHeaders ,
                                row.cellIterator());
                        break;
                    case 4:
                        log.debug("reading sub headers");
//                        read second level headers
                        validate2ndLevelHeaders(errorList ,
                                tableSecondLevelHeaders ,
                                cropMapping ,
                                row.cellIterator());
                        break;
                    case 5:
//                        reading third level headers
                        log.debug("reading sub sub headers");
                        validateCropsSubHeaders(errorList ,
                                tableThirdLevelHeaders ,
                                row.cellIterator());
                        break;
                    default:
                        log.debug("reading sub sub headers");
//                        raed user input data
                        readUserData(errorList ,
                                userMap ,
                                row.cellIterator() ,
                                cropMapping ,
                                workbook);
                        break;

                }
            }
        } catch (IOException e) {
            log.error("error occurred while loading excel file " + e.getMessage());
            e.printStackTrace();
            errorList.add("error occurred while loading excel file " + e.getMessage());
        } catch (EncryptedDocumentException e) {
            log.error("encrypted document " + e.getMessage());
            e.printStackTrace();
            errorList.add("encrypted document");
        }

        endTime = System.currentTimeMillis();
        log.info("task end : " + (endTime - startTime) + "ms");

        if (!errorList.isEmpty()) {
            log.error("errors while reading file ");
            errorList.stream().forEach(System.out::println);
            return ResponseEntity.badRequest().body(errorList);
        }

        return ResponseEntity.ok().build();
    }

    //    validating crops sub headers
    private void validateCropsSubHeaders(List<String> errorList ,
                                         Map<Integer, String> tableThirdLevelHeaders ,
                                         Iterator<Cell> cellIterator) {
        int index = 1;
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
//            check cell is blank
            if (cell.getCellType() == CellType.BLANK) {
                continue;
            }
            index = ((cell.getColumnIndex() - excelProperties.getStartPoint()) % 4) + 1;
            String title = tableThirdLevelHeaders.get(index);
            try {
//                check table header is contained provided list
                if (!cell.getStringCellValue().trim().equals(title.trim())) {
                    errorList.add(cell.getAddress() + " must be " + title);
                }
            } catch (IllegalStateException e) {
                errorList.add(cell.getAddress() + " must be Text");
            }
            log.debug("Cell " + cell.getAddress() + " : " + cell.getStringCellValue());
        }
//        each crops have 4 sub headers.
        if (index < 4) {
            errorList.add(tableThirdLevelHeaders.get(index) + " must added to end of table headers");
        }
    }

    //   validating headers like Farmer Details, Farm Details, Location information	..etc
    private void validate1stLevelHeaders(List<String> errorList ,
                                         Map<String, String> tableTopLevelHeaders ,
                                         Iterator<Cell> cellIterator) {
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

            log.debug("Cell " + cell.getAddress() + " : " + cell.getStringCellValue());
        }
    }

    //    validating second level headers in table like CUID, Unit Number for EU / JAS, Farmer Code for EU / JAS
    private void validate2ndLevelHeaders(List<String> errorList ,
                                         Map<String, String> tableSecondLevelHeaders ,
                                         Map<Integer, Crop> cropMapping ,
                                         Iterator<Cell> cellIterator) {
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            if (cell.getCellType() == CellType.BLANK) {
                continue;
            }
//            validating fixed headers in table
            if (cell.getColumnIndex() < excelProperties.getStartPoint()) {
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
//                validating crops name
                log.debug("Cell - getting crop - " + cell.getAddress() + " : " + cell.getStringCellValue() + " : " + cell.getColumnIndex());
                validateCrops(errorList , cropMapping , cell);
            }
        }
    }

    //    validating crops implement
    private void validateCrops(List<String> errorList , Map<Integer, Crop> cropMapping , Cell cell) {
        try {
            Crop crop = getCrop(cell , errorList);
            if (crop == null) {
                errorList.add(cell.getAddress() + " must be valid existing crop");
            } else {
                cropMapping.put(cell.getColumnIndex() , crop);
            }
        } catch (IllegalStateException e) {
            errorList.add(cell.getAddress() + " must be Text");
            e.printStackTrace();
        }

    }


    private void readUserData(List<String> errorList ,
                              HashMap<String, ArrayList<String>> userMap ,
                              Iterator<Cell> cellIterator ,
                              Map<Integer, Crop> cropMapping ,
                              Workbook workbook) {

        String unitNoEUJAS;
        String unitNoNOP;
        String farCodeNOP;
        String farmerName;
        String farmName;
        double totalArea;
        String city;
        String gps;
        Date dateCert;
        String aplyRetrospe;
        Date dateConversion;
        String fertilizer;
        String eujasField;
        String eujasHarvest;
        String usdaHarvest;
        String usdaField;
        String address;
        String certification;
        Date dateOrganic;
        long cuid;
        String farmerCode = null;
        String plotCode;
        Crop crop = new Crop();
        if (cropMapping.keySet().size() == 0) {
            errorList.add("You need add least one crop");
        }

        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();

            if (cell.getColumnIndex() == 0){
                if (cell.getCellType() == CellType.BLANK){
                    break;
                }
            }
            if (cell.getCellType() == CellType.BLANK) {
                continue;
            }
            if (cell.getCellType() == CellType.FORMULA) {
                log.info("**********Formular*********" + cell.getAddress());
                FormulaEvaluator evaluator = workbook
                        .getCreationHelper()
                        .createFormulaEvaluator();
                CellValue cellValue = evaluator.evaluate(cell);
                String result = cellValue.formatAsString();
                cell.setCellValue(result);
            }

            switch (cell.getColumnIndex()) {
                case 0:
                    try {
                        cuid = BigDecimal.valueOf(cell.getNumericCellValue()).longValue();
                        log.debug("cuid : " + cuid);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 1:
                    try {
                        unitNoEUJAS = cell.getStringCellValue().trim();
                        log.debug("Unit Number for EU / JAS : " + unitNoEUJAS);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 2:
                    try {
                        farmerCode = cell.getStringCellValue().trim();
                        log.debug("Farmer Code for EU / JAS : " + farmerCode);
                        userMap.putIfAbsent(cell.getStringCellValue() , new ArrayList<>());
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 3:
                    try {
                        unitNoNOP = cell.getStringCellValue().trim();
                        log.debug("Unit Number for NOP : " + unitNoNOP);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 4:
                    try {
                        farCodeNOP = cell.getStringCellValue().trim();
                        log.debug("farmer code for NOP : " + farCodeNOP);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 5:
                    try {
                        farmerName = cell.getStringCellValue().trim();
                        log.debug("Farmer Name : " + farmerName);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 6:
                    try {
                        farmName = cell.getStringCellValue().trim();
                        log.debug("Name of the Farm : " + farmName);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 7:
                    try {
                        plotCode = cell.getStringCellValue().trim();
                        log.debug("Checking duplicate plots for farmer : " + farmerCode + " with " + plotCode);
                        if (userMap.containsKey(farmerCode)) {
                            if (userMap.get(farmerCode).contains(plotCode)) {
                                errorList.add(cell.getAddress() + " contains duplicate plot value");
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
                        log.debug("Total Area (Ha) : " + totalArea);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " must be number");
                    }
                    break;
                case 9:
                    try {
                        gps = cell.getStringCellValue().trim();
                        log.debug("GPS : " + gps);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 10:
                    try {
                        address = cell.getStringCellValue().trim();
                        log.debug("Address/Village : " + address);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 11:
                    try {
                        city = cell.getStringCellValue().trim();
                        log.debug("City : " + city);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 12:
                    try {
                        dateCert = cell.getDateCellValue();
                        log.debug("Application date for certification (yyyy-mm-dd) : " + dateCert);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format of date. It must be " + excelProperties.getDateFormat());
                    }
                    break;
                case 13:
                    try {
                        aplyRetrospe = cell.getStringCellValue().trim();
                        log.debug("Applying for Retrospective consideration (Yes/No) : " + aplyRetrospe);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 14:
                    try {
                        certification = cell.getStringCellValue().trim();
                        log.debug("Certifications : " + certification);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 15:
                    try {
                        fertilizer = cell.getStringCellValue().trim();
                        log.debug("Types of fertilizer, pesticide used : " + fertilizer);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 16:
                    try {
                        log.debug("Last date of use : " + cell.getStringCellValue());
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 17:
                    try {
                        dateConversion = cell.getDateCellValue();
                        log.debug("Starting date of Conversion period (yyyy-mm-dd) : " + dateConversion);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format of date. It must be " + excelProperties.getDateFormat());
                    }
                    break;
                case 18:
                    try {
                        dateOrganic = cell.getDateCellValue();
                        log.debug("Starting date of Organic Period (yyyy-mm-dd) : " + dateOrganic);

                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format of date. It must be " + excelProperties.getDateFormat());
                    }
                    break;
                case 19:
                    try {
                        eujasField = cell.getStringCellValue().trim();
                        log.debug("Field Status EU/JAS ic1/ic2/ic3/org : " + eujasField);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
                case 20:
                    try {
                        eujasHarvest = cell.getStringCellValue().trim();
                        log.debug("Harvest status EU/JAS conv/ic/org : " + eujasHarvest);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 21:
                    try {
                        usdaField = cell.getStringCellValue().trim();
                        log.debug("Field status NOP ic1/ic2/ic3/org : " + usdaField);
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }
                    break;
                case 22:
                    try {
                        usdaHarvest = cell.getStringCellValue().trim();
                        log.debug("Harvest status NOP conv/org : " + usdaHarvest);
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
                        log.info(crop.getCropName() + " cell-type : "+cell.getCellType().toString()+" value : "+ cell.getNumericCellValue());
                    } catch (IllegalStateException e) {
                        errorList.add(cell.getAddress() + " contains illegal format.");
                    }

                    break;
            }

        }
    }

    //    get crop from db
    private Crop getCrop(Cell cell , List<String> errorList) {
        String cropName = cell.getStringCellValue().trim();
        log.debug("checking crop " + cropName + " on db");
        return cropRepository
                .findByCropName(cropName)
                .orElseGet(
                        () -> {
                            errorList.add(cell.getAddress() + "(" + cropName + ") must be valid existing crop");
                            return null;
                        });
    }

    private void cellNameValidation(Cell cell ,
                                    String name ,
                                    List<String> errorList) {
        if (!cell.toString().equalsIgnoreCase(name)) {
            errorList.add(cell.getAddress() + " must be " + name);
        }
        log.debug("cell 1 " + cell);
    }

    public void getAllCrops() {
        System.out.println(cropRepository.findAll());
    }
}
