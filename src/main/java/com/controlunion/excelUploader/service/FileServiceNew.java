package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.config.ExcelFilePropertiesConfig;
import com.controlunion.excelUploader.dto.ExcelErrorResponse;
import com.controlunion.excelUploader.dto.FarmerListComparisonDto;
import com.controlunion.excelUploader.dto.FarmerListMandatoryFieldsDto;
import com.controlunion.excelUploader.enums.Errors;
import com.controlunion.excelUploader.model.Crop;
import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.FarmerListCrop;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.repository.FarmerlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.sql.Date;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceNew {

    private final String className = this.getClass().getName();
    private boolean isAuditDetailsDone = false;
    private final ExcelFilePropertiesConfig excelFileProperties;
    private final CropService cropService;
    private final FarmerListService farmerListService;
    private final FarmerListFinalService farmerListFinalService;
    private final FarmerlistRepository farmerlistRepository;

    public ResponseEntity uploadExcelFile(MultipartFile file,
                                          int projectId,
                                          int auditId,
                                          String projectName,
                                          int proId) {

        log.info("File recieved " + file.getOriginalFilename());
        List<ExcelErrorResponse> errorList = new ArrayList<>();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is not present");
        }
        if (!isFileExcel(file)) {
            return ResponseEntity.badRequest().body("File format not support. It must be .xls or .xlsx");
        }
        return readFile(file, errorList, projectId, auditId, projectName, proId);

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
                                    List<ExcelErrorResponse> errorList,
                                    int projectId,
                                    int auditId, String projectName, int proId) {

        List<FarmerList> farmerLists = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0); // Assuming the data is on the first sheet
            int rowNumber = 0;
            validateAuditHeaders(sheet, errorList, projectId, projectName);
            log.info(className + ".readFile : returned row number " + rowNumber);
            log.error(className + ".readFile : Audit headers errors " + errorList);
            final Iterator<Row> iterator = sheet.iterator();
            Map<Integer, Crop> cropMapping = new LinkedHashMap<>();

            Row row = findTableStart(iterator, errorList, rowNumber + 1);
            log.info(className + ".readFile : returned row number " + rowNumber);
            log.error(className + ".readFile : until table start errors " + errorList);
            log.info(className + ".readFile : table start point : " + rowNumber);
            validate1stLevelHeaders(row, errorList);
            row = iterator.next();
            validate2ndLevelHeaders(row, errorList, cropMapping, sheet);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            iterator.next();
            farmerLists = readAuditData(iterator, cropMapping, errorList, evaluator, projectId, auditId, proId);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }

        if (!errorList.isEmpty()) {
            log.error(className + ".errors while reading file ");
            return ResponseEntity.badRequest().body(errorList);
        }
        farmerListService.saveFarmerList(farmerLists);
        return ResponseEntity.ok().body("done");
    }

    private List<FarmerList> readAuditData(Iterator<Row> iterator,
                                           Map<Integer, Crop> cropMapping,
                                           List<ExcelErrorResponse> errorList,
                                           FormulaEvaluator evaluator,
                                           int projectId,
                                           int auditId,
                                           int proId) {

        HashMap<String, ArrayList<String>> userMap = new HashMap<>(); // store farmer's plots
        java.sql.Date dateCert = null;
        String certification = "";
        int cuid = 0;
        String farmerCode = "";
        String plotCode = "";
        boolean isNewFarmer = false;


        if (cropMapping.keySet().size() == 0) {
            errorList.add(ExcelErrorResponse.builder()
                    .error(Errors.REQUIRED_CROP.getName())
                    .build());
        }
        List<FarmerList> farmerListList = new ArrayList<>();
        List<FarmerListComparisonDto> farmerListComparisonDto = new ArrayList<>();
        FarmerListFinal farmerListFinal = null;
        while (iterator.hasNext()) {
            System.out.println("*********************************new Row***********************");
            Row row = iterator.next();
            if (!isRowNotEmpty(row)) {
                continue;
            }
            final Iterator<Cell> cellIterator = row.cellIterator();
            FarmerListMandatoryFieldsDto mandatory_fields = new FarmerListMandatoryFieldsDto();

            FarmerList farmerList = new FarmerList();
            List<FarmerListCrop> farmerListCropList = new ArrayList<>();
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
                    try {
                        cell = isFormulaProceed(cell, evaluator, errorList);
                    } catch (IllegalStateException | FormulaParseException e) {
                        errorList.add(ExcelErrorResponse.builder()
                                .location("Cell:" + cell.getAddress())
                                .error(Errors.INVALID_FORMULA.getName())
                                .build());
                        log.error(className + ".readAuditData : " + cell.getAddress() + " " + Errors.INVALID_FORMULA.getName());
                        continue;
                    } catch (RuntimeException e) {
                        errorList.add(ExcelErrorResponse.builder()
                                .location("Cell:" + cell.getAddress())
                                .error(Errors.INVALID_FORMULA.getName())
                                .build());
                        log.error(className + ".readAuditData : " + "cell : " + cell.getAddress() + " " + Errors.INVALID_FORMULA.getName());
                        continue;
                    }
                }

                switch (cell.getColumnIndex()) {
                    case 0:
                        try {
                            cuid = BigDecimal.valueOf(cell.getNumericCellValue()).intValue();
                            log.info(className + "cuid : " + cuid);
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format.")
                                    .build());
                        }
                        break;
                    case 1:
                        farmerList.setUnitNoEUJAS(convertCellValueToStringValue(cell));
//                        unitNoEUJAS = convertCellValueToStringValue(cell);
                        mandatory_fields.setUnitNoEUJAS(true);
                        log.info(className + ".readAuditData : Unit Number for EU / JAS : " + convertCellValueToStringValue(cell));

                        break;
                    case 2:
                        farmerCode = convertCellValueToStringValue(cell);
                        farmerList.setFarCodeEUJAS(farmerCode);
                        mandatory_fields.setFarCodeEUJAS(true);

                        if (row.getCell(0) == null || row.getCell(0).getStringCellValue().trim().equals("")) {
                            System.out.println("cuid not present");
                            cuid = farmerListFinalService.getFarmerCUIDByFarmerCode(farmerCode, proId);
                            System.out.println(cuid);
                            if (cuid == 0) {
                                isNewFarmer = true;
                                log.info(className + ".readAuditData: isNewFarmer - " + isNewFarmer + " : creating cuid");
                                cuid = farmerListFinalService.createCuid();
                                log.info(className + ".readAuditData: isNewFarmer - " + isNewFarmer + " : new cuid - "+cuid);
                            }
                            log.info(className + ".readAuditData: isNewFarmer - " + isNewFarmer + " : cuid-" + cuid);
                        }
                        farmerList.setCufarmerID(cuid);
                        farmerList.setIsNew(isNewFarmer ? 1 : 0);
                        log.info(className + ".readAuditData : Farmer Code for EU / JAS : " + farmerCode);
                        userMap.putIfAbsent(cell.getStringCellValue(), new ArrayList<>());
                        break;
                    case 3:
                        farmerList.setUnitNoNOP(convertCellValueToStringValue(cell));
                        mandatory_fields.setUnitNoNOP(true);
                        log.info(className + ".readAuditData : Unit Number for NOP : " + convertCellValueToStringValue(cell));
                        break;
                    case 4:
                        farmerList.setFarCodeNOP(convertCellValueToStringValue(cell));
                        mandatory_fields.setFarCodeNOP(true);
                        log.info(className + ".readAuditData : farmer code for NOP : " + convertCellValueToStringValue(cell));
                        break;
                    case 5:
                        farmerList.setFarmerName(convertCellValueToStringValue(cell));
                        mandatory_fields.setFarmerName(true);
                        log.info(className + ".readAuditData : Farmer Name : " + convertCellValueToStringValue(cell));
                        break;
                    case 6:
                        farmerList.setFarmName(convertCellValueToStringValue(cell));
                        mandatory_fields.setFarmName(true);
                        log.info(className + ".readAuditData : Name of the Farm : " + convertCellValueToStringValue(cell));
                        break;
                    case 7:
                        plotCode = convertCellValueToStringValue(cell);
                        farmerList.setPlotCode(plotCode);
                        mandatory_fields.setPlotCode(true);
                        log.info(className + ".readAuditData : Checking duplicate plots for farmer : " + farmerCode + " with " + plotCode);
                        if (userMap.containsKey(farmerCode)) {
                            if (userMap.get(farmerCode).contains(plotCode)) {
                                errorList.add(ExcelErrorResponse.builder()
                                        .location("Cell:" + cell.getAddress())
                                        .error(Errors.DUPLICATE_PLOT_VALUES.getName())
                                        .errorValue(plotCode)
                                        .build());
                            } else {
                                userMap.get(farmerCode).add(plotCode);
                            }
                        }

                        break;
                    case 8:
                        try {
//                            totalArea = cell.getNumericCellValue();
                            farmerList.setTotalArea(cell.getNumericCellValue());
                            mandatory_fields.setTotalArea(true);
                            log.info(className + ".readAuditData : Total Area (Ha) : " + cell.getNumericCellValue());
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error(Errors.REQUIRED_NUMBER_VALUE.getName())
                                    .build());
                        }
                        break;
                    case 9:
//                        gps = convertCellValueToStringValue(cell);
                        farmerList.setGps(convertCellValueToStringValue(cell));
                        log.info(className + ".readAuditData : GPS : " + convertCellValueToStringValue(cell));

                        break;
                    case 10:
//                        address = convertCellValueToStringValue(cell);
                        farmerList.setAddress(convertCellValueToStringValue(cell));
                        log.info(className + ".readAuditData : Address/Village : " + convertCellValueToStringValue(cell));

                        break;
                    case 11:
                        try {
//                            city = convertCellValueToStringValue(cell);
                            farmerList.setCity(convertCellValueToStringValue(cell));
                            mandatory_fields.setCity(true);
                            log.info(className + ".readAuditData : City : " + convertCellValueToStringValue(cell));
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format.")
                                    .build());
                        }

                        break;
                    case 12:
                        try {
//                            dateCert = new java.sql.Date(cell.getDateCellValue().getTime());
                            farmerList.setDateCert(new java.sql.Date(cell.getDateCellValue().getTime()));
                            log.info(className + ".readAuditData : Application date for certification (yyyy-mm-dd) : " + dateCert);
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format. Change cell format to Date.")
                                    .build());
                        }
                        break;
                    case 13:
                        farmerList.setAplyRetrospe(convertCellValueToStringValue(cell) == "yes" ? 1 : 0);
//                        aplyRetrospe = convertCellValueToStringValue(cell);
                        log.info(className + ".readAuditData : Applying for Retrospective consideration (Yes/No) : " + convertCellValueToStringValue(cell));

                        break;
                    case 14:
//                        certification = convertCellValueToStringValue(cell);
                        farmerList.setCertification(convertCellValueToStringValue(cell));
                        log.info(className + ".readAuditData : Certifications : " + certification);

                        break;
                    case 15:
//                        fertilizer = cell.getStringCellValue().trim();
                        farmerList.setFertilizer(convertCellValueToStringValue(cell));
                        log.info(className + ".readAuditData : Types of fertilizer, pesticide used : " + convertCellValueToStringValue(cell));

                        break;
                    case 16:
//                        ferUseDate = convertCellValueToStringValue(cell);
                        farmerList.setFerUseDate(convertCellValueToStringValue(cell));
                        log.info(className + ".readAuditData : Last date of use : " + convertCellValueToStringValue(cell));
                        break;
                    case 17:
                        try {
                            farmerList.setDateConfersion(new java.sql.Date(cell.getDateCellValue().getTime()));
                            mandatory_fields.setStartingDateCon(true);
                            log.info(className + ".readAuditData : Starting date of Conversion period (yyyy-mm-dd) : " + new java.sql.Date(cell.getDateCellValue().getTime()));
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format. Change cell format to Date.")
                                    .build());
                        }

                        break;
                    case 18:
                        try {
//                            dateOrganic = new Date(cell.getDateCellValue().getTime());
                            farmerList.setDateorganic(new Date(cell.getDateCellValue().getTime()));
                            mandatory_fields.setStartingDateOrg(true);
                            log.info(className + "readAuditData : Starting date of Organic Period (yyyy-mm-dd) : " + new Date(cell.getDateCellValue().getTime()));

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format. Change cell format to Date.")
                                    .build());
                        }
                        break;
                    case 19:
//                    try {
//                        eujasField = convertCellValueToStringValue(cell);
                        farmerList.setEujas_field(convertCellValueToStringValue(cell));
                        mandatory_fields.setEujasField(true);
                        log.info(className + "readAuditData : Field Status EU/JAS ic1/ic2/ic3/org : " + convertCellValueToStringValue(cell));
//                    } catch (IllegalStateException e) {
//                        errorList.add(cell.getAddress() + " contains illegal format.");
//                    }

                        break;
                    case 20:
//                    try {
//                        eujasHarvest = convertCellValueToStringValue(cell);
                        farmerList.setEujas_harvest(convertCellValueToStringValue(cell));
                        mandatory_fields.setEujasHarvest(true);
                        log.info(className + "readAuditData : Harvest status EU/JAS conv/ic/org : " + convertCellValueToStringValue(cell));
//                    } catch (IllegalStateException e) {
//                        errorList.add(cell.getAddress() + " contains illegal format.");
//                    }
                        break;
                    case 21:
//                    try {
//                        usdaField = convertCellValueToStringValue(cell);
                        farmerList.setUsda_field(convertCellValueToStringValue(cell));
                        log.info(className + "readAuditData : Field status NOP ic1/ic2/ic3/org : " + convertCellValueToStringValue(cell));
//                    } catch (IllegalStateException e) {
//                        errorList.add(cell.getAddress() + " contains illegal format.");
//                    }
                        break;
                    case 22:
//                        usdaHarvest = convertCellValueToStringValue(cell);
                        farmerList.setUsda_harvest(convertCellValueToStringValue(cell));
                        log.info(className + "readAuditData : Harvest status NOP conv/org : " + convertCellValueToStringValue(cell));
                        break;
//                    reading crop section
                    default:
                        try {
                            if (cropMapping.containsKey(cell.getColumnIndex())) {
                                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++reading : " + cropMapping.get(cell.getColumnIndex()).getCropName());

                                Crop crop = cropMapping.get(cell.getColumnIndex());
                                FarmerListCrop farmerListCrop = new FarmerListCrop();
                                farmerListCrop.setCropID(crop.getCropID());
                                farmerListCrop.setPlotCode(plotCode);
                                farmerListCrop.setCufarmerID(cuid);
                                farmerListCrop.setFarmerList(farmerList);

                                int currentCellStart = cell.getColumnIndex();
                                System.out.println("Start from - " + cell.getAddress() + " : " + currentCellStart);
                                for (int i = 0; i < excelFileProperties.getHeader3().size(); i++) {
                                    try {
                                        System.out.println(crop.getCropName() + " : " + row.getCell(currentCellStart + i).getAddress());
                                        switch (i) {
                                            case 0:
                                                farmerListCrop.setNoOfPlant(convertCellValueToNumberValue(row.getCell(currentCellStart + i)));
                                                break;
                                            case 1:
                                                farmerListCrop.setEstiYield(convertCellValueToNumberValue(row.getCell(currentCellStart + i)));
                                                break;
                                            case 2:
                                                farmerListCrop.setRealYield(convertCellValueToNumberValue(row.getCell(currentCellStart + i)));
                                                break;
                                            case 3:
                                                farmerListCrop.setNoOfSesons(convertCellValueToNumberValue(row.getCell(currentCellStart + i)));
                                                break;

                                        }
                                    } catch (NullPointerException e) {

                                    }
                                }
                                farmerListCropList.add(farmerListCrop);
                            }
                        } catch (IllegalStateException e) {
                            log.error("Cell:" + cell.getAddress() + " : " + e.getMessage());
                            e.printStackTrace();
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format.")
                                    .build());
                        }

                        break;
                }
            }


            checkMandatoryFieldsExists(mandatory_fields, errorList, (row.getRowNum() + 1));
            farmerList.setUser("isuru");
            farmerList.setChngCropdata("");
            farmerList.setChngFarmdata("");
            farmerList.setSysTimeStamp(new Date(new java.util.Date().getTime()));
            farmerList.setProID(projectId);
            farmerList.setAuditID(auditId);
            farmerList.setFarmerListCropList(farmerListCropList);
            farmerList.setInspected(0);
            farmerListList.add(farmerList);
            System.out.println(auditId);
            System.out.println(projectId);
            if (!isNewFarmer) {
                if (farmerListFinal != null) {
                    farmerListComparisonDto.add(new FarmerListComparisonDto(farmerList, farmerListFinal));
                }
            }
        }
        if (errorList.isEmpty()) {
            makeComparison(farmerListComparisonDto);
        }
        return farmerListList;

    }

    private void makeComparison(List<FarmerListComparisonDto> farmerListComparisonDto) {
        for (FarmerListComparisonDto far : farmerListComparisonDto) {

        }
    }


    private Cell isFormulaProceed(Cell cell,
                                  FormulaEvaluator evaluator,
                                  List<ExcelErrorResponse> errorList) {
        try {
            log.info("**********Formular*********" + cell.getAddress());
            CellValue cellValue = evaluator.evaluate(cell);
            String result = cellValue.formatAsString();
            cell.setCellType(CellType.STRING);
            cell.setCellValue(result);
            log.info(className + ".isFormulaProceed : result of formula : " + cell.getStringCellValue());
        } catch (IllegalStateException | FormulaParseException e) {
            throw new IllegalStateException();
        }

        return cell;
    }

    public void validateAuditHeaders(XSSFSheet sheet,
                                     List<ExcelErrorResponse> errorList,
                                     int projectId,
                                     String projectName) {
        Map<Integer, String> auditDetails = excelFileProperties.getAuditDetails();
        log.info("start validating audit headers");
        for (int i = 0; i < auditDetails.size(); i++) {
            XSSFRow row = sheet.getRow(i);

            try {
                XSSFCell cell = row.getCell(0);
                String title = cell.getStringCellValue().trim();
                String auditTitle = auditDetails.get(i);
                if (!title.equalsIgnoreCase(auditTitle)) {
                    errorList.add(ExcelErrorResponse.builder()
                            .location("Cell " + cell.getAddress())
                            .error(Errors.INVALID_HEADER.getName())
                            .errorValue(cell.getStringCellValue())
                            .correctValue(auditDetails.get(i))
                            .build());
                } else {
                    switch (title) {
                        case "Project Name":
                            validateProjectName(projectName, row, errorList);
                            break;
                        case "Project ID":
                            validateProjectId(projectId, row, errorList);
                            break;

                    }
                }
            } catch (NullPointerException e) {
                errorList.add(ExcelErrorResponse.builder().location("Row " + (i + 1)).error(Errors.EMPTY_ROW.getName()).build());
                errorList.add(ExcelErrorResponse.builder().location("Row " + (i + 1)).error(Errors.MUST_BE.getName()).correctValue(auditDetails.get(i)).build());
                continue;
            }
        }
        log.info("end validating audit headers");
    }

    private void validateProjectId(int projectId, XSSFRow row, List<ExcelErrorResponse> errorList) {
        XSSFCell cell = row.getCell(1);
        try {
            if (projectId != cell.getNumericCellValue()) {
                errorList.add(ExcelErrorResponse.builder()
                        .location("Cell " + cell.getAddress())
                        .error(Errors.PROJECT_CODE_MISMATCH.getName())
                        .errorValue(convertCellValueToStringValue(cell))
                        .correctValue(String.valueOf(projectId))
                        .build());
            }
        } catch (IllegalStateException e) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Cell " + cell.getAddress())
                    .error(Errors.REQUIRED_NUMBER_VALUE.getName())
                    .errorValue(cell.getStringCellValue())
                    .correctValue(String.valueOf(projectId))
                    .build());
        } catch (NullPointerException e) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Cell " + cell.getAddress())
                    .error(Errors.MANDATORY_CELL.getName())
                    .correctValue(String.valueOf(projectId))
                    .build());
        }
    }

    private void validateProjectName(String projectName, XSSFRow row, List<ExcelErrorResponse> errorList) {
        XSSFCell cell = row.getCell(1);
        try {
            if (!projectName.equals(cell.getStringCellValue())) {
                errorList.add(ExcelErrorResponse.builder()
                        .location("Cell " + cell.getAddress())
                        .error(Errors.PROJECT_NAME_MISMATCH.getName())
                        .errorValue(convertCellValueToStringValue(cell))
                        .correctValue(projectName)
                        .build());
            }
        } catch (IllegalStateException e) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Cell " + cell.getAddress())
                    .error(Errors.REQUIRED_TEXT_VALUE.getName())
                    .errorValue(cell.getStringCellValue())
                    .correctValue(projectName)
                    .build());
        } catch (NullPointerException e) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Cell " + cell.getAddress())
                    .error(Errors.MANDATORY_CELL.getName())
                    .correctValue(String.valueOf(projectName))
                    .build());
        }
    }

    private Row findTableStart(Iterator<Row> rowIterator,
                               List<ExcelErrorResponse> errorList,
                               int rowNumber) {

        log.info("start finding table start point at " + rowNumber);
        System.out.println(rowIterator.next());
        findingTableStartOnRows:
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            log.info("Iterating through rows until find table");
            log.info("actual row number " + row.getRowNum() + "native row number : " + rowNumber);

            if (!isRowNotEmpty(row)) {
                ExcelErrorResponse error = ExcelErrorResponse.builder().location("Row " + (row.getRowNum() + 1)).error(Errors.EMPTY_ROW.getName()).build();
                if (!errorList.contains(error)) {
                    errorList.add(error);
                }
            }
            System.out.println(row);

            if (row.getRowNum() - rowNumber > 0) {
                log.error("Row " + row.getRowNum() + " : found " + (row.getRowNum() - rowNumber) + " empty row(s)");
                for (int i = rowNumber + 1; i <= row.getRowNum(); i++) {
                    log.error("Row *" + i + " : " + Errors.EMPTY_ROW);
                    ExcelErrorResponse error = ExcelErrorResponse.builder().location("Row " + i).error(Errors.EMPTY_ROW.getName()).build();
                    if (!errorList.contains(error)) {
                        errorList.add(error);
                    }
                }
                rowNumber = row.getRowNum();
                log.info("set new row number : " + rowNumber);
            }
            final Iterator<Cell> cellIterator = row.cellIterator();
            int matchingTableHeaders = 0;
            log.info("checking is table start");
            lookingCellLoop:
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                try {
                    if (cell.getStringCellValue().equals(excelFileProperties.getHeader1().get(cell.getColumnIndex()))) {
                        matchingTableHeaders++;
                        log.info("matching headers " + matchingTableHeaders);
                    }
                    if (matchingTableHeaders > (excelFileProperties.getHeader1().size() / 2)) {
                        log.info("end finding table start " + row.getRowNum());
//                        break findingTableStartOnRows;
                        return row;
                    }
                } catch (IllegalStateException e) {
                }
            }
            log.info("table not started yet");
            try {
                if (!excelFileProperties.getAuditDetails().values().contains(row.getCell(0).getStringCellValue())) {
                    if (row.getRowNum() >= excelFileProperties.getAuditDetails().size()) {
                        log.error(row.getCell(0).getAddress() + " : 1" + Errors.INVALID_LOCATION.getName() + " : " + row.getCell(0).getStringCellValue());
                        errorList.add(ExcelErrorResponse.builder()
                                .location("Cell* " + row.getCell(0).getAddress().toString())
                                .error(Errors.INVALID_LOCATION.getName())
                                .errorValue(row.getCell(0).getStringCellValue())
                                .build());
                    }

                } else {
                    if (row.getRowNum() >= excelFileProperties.getAuditDetails().size()) {
                        log.error(row.getCell(0).getAddress() + " : 1" + Errors.INVALID_LOCATION.getName() + " : " + row.getCell(0).getStringCellValue());
                        errorList.add(ExcelErrorResponse.builder()
                                .location("Cell " + row.getCell(0).getAddress().toString())
                                .error(Errors.INVALID_LOCATION.getName())
                                .errorValue(row.getCell(0).getStringCellValue())
                                .build());
                    }
                }

            } catch (IllegalStateException e) {
                log.error(row.getCell(0).getAddress() + " : 2" + Errors.INVALID_LOCATION.getName() + " : " + row.getCell(0).getStringCellValue());
                errorList.add(ExcelErrorResponse.builder()
                        .location("Cell " + row.getCell(0).getAddress().toString())
                        .error(Errors.INVALID_LOCATION.getName())
                        .errorValue(row.getCell(0).getStringCellValue())
                        .build());
            } catch (NullPointerException e) {

            }

            log.info("not table starting " + rowNumber);
            rowNumber++;
        }

        return null;
    }

    private void validateTableHeaders(Iterator<Row> rowIterator, Row row, List<ExcelErrorResponse> errorList, int rowNumber) {
        System.out.println("Starting row - ");
        System.out.println(row);

        Row row1 = rowIterator.next();

    }


    private void validate1stLevelHeaders(Row row, List<ExcelErrorResponse> errorList) {
        Iterator<Cell> cellIterator = row.iterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            try {
                if (excelFileProperties.getHeader1().containsKey(cell.getColumnIndex())) {
                    if (!cell.getStringCellValue().equals(excelFileProperties.getHeader1().get(cell.getColumnIndex()))) {
                        errorList.add(ExcelErrorResponse.builder()
                                .location("Cell " + cell.getAddress())
                                .error(Errors.INVALID_HEADER.getName())
                                .errorValue(cell.getStringCellValue())
                                .correctValue(excelFileProperties.getHeader1()
                                        .get(cell.getColumnIndex())).build());

                    }
                }
            } catch (NullPointerException | IllegalStateException e) {
                errorList.add(ExcelErrorResponse.builder()
                        .location("Cell " + cell.getAddress())
                        .error(Errors.INVALID_HEADER.getName())
                        .errorValue(cell.getStringCellValue())
                        .correctValue(excelFileProperties.getHeader1()
                                .get(cell.getColumnIndex())).build());

            }

        }
    }

    private void validate2ndLevelHeaders(Row row,
                                         List<ExcelErrorResponse> errorList,
                                         Map<Integer, Crop> cropMapping,
                                         XSSFSheet sheet) {
        log.info("validating 2nd level headers " + row.getRowNum());
        final Iterator<Cell> cellIterator = row.iterator();
        int index = 0;

        while (cellIterator.hasNext()) {
            log.info("iterating in header 2");
            Cell cell = cellIterator.next();
            try {
                if (excelFileProperties.getHeader2().containsKey(cell.getColumnIndex())) {
                    if (!cell.getStringCellValue().trim().equals(excelFileProperties.getHeader2().get(cell.getColumnIndex()))) {
                        errorList.add(ExcelErrorResponse.builder()
                                .location("Cell " + cell.getAddress())
                                .error(Errors.INVALID_HEADER.getName())
                                .errorValue(cell.getStringCellValue())
                                .correctValue(excelFileProperties.getHeader2()
                                        .get(cell.getColumnIndex())).build());

                    }
                }
            } catch (NullPointerException | IllegalStateException e) {
                errorList.add(ExcelErrorResponse.builder()
                        .location("Cell " + cell.getAddress())
                        .error(Errors.INVALID_HEADER.getName())
                        .errorValue(cell.getStringCellValue())
                        .correctValue(excelFileProperties.getHeader2()
                                .get(cell.getColumnIndex())).build());

            }
            index++;
            if (index > excelFileProperties.getHeader2().size() - 1) {
                break;
            }
        }

        validateCropsHeaders(cellIterator, errorList, cropMapping, sheet, index);
    }

    private void validateCropsHeaders(Iterator<Cell> cellIterator,
                                      List<ExcelErrorResponse> errorList,
                                      Map<Integer, Crop> cropMapping,
                                      XSSFSheet sheet,
                                      int lastColNumber) {
        System.out.println("ready validating crops " + lastColNumber);
        ArrayList<Cell> cells = new ArrayList<>();
        while (cellIterator.hasNext()) {
            cells.add(cellIterator.next());
        }

        for (int i = 0; i < cells.size(); i += excelFileProperties.getHeader3().size()) {
            Cell cell = cells.get(i);
            if (cell.getCellType() == CellType.BLANK) {
                errorList.add(ExcelErrorResponse.builder()
                        .location("Cell " + cell.getAddress())
                        .error(Errors.MANDATORY_CELL.getName())
                        .build());
                continue;
            }


            boolean isCropValid = validateCrops(cropMapping, cell);
            if (!isCropValid) {
                errorList.add(ExcelErrorResponse.builder()
                        .location("Cell " + cell.getAddress())
                        .error(Errors.CROP_NOT_VALID.getName())
                        .errorValue(cell.getStringCellValue())
                        .build());
            } else {

                System.out.println(cell.getColumnIndex());
                Crop crop = cropMapping.get(cell.getColumnIndex());
                log.info("crop " + crop.getCropName() + " : passed");
                XSSFRow row = sheet.getRow(cell.getRowIndex() + 1);
                validateCropsSub(row, crop, cell.getColumnIndex(), errorList);
                System.out.println();
            }

        }
    }


    private void validateCropsSub(XSSFRow row,
                                  Crop crop,
                                  int colNumber,
                                  List<ExcelErrorResponse> errorList) {
        if (row != null) {
            for (int i = colNumber; i < colNumber + 4; i++) {
                XSSFCell cell = row.getCell(i);
                if (!cell.getStringCellValue().trim().equals(excelFileProperties.getHeader3().get(i - colNumber))) {
                    errorList.add(ExcelErrorResponse.builder().location("Cell " + cell.getAddress())
                            .error(Errors.INVALID_TABLE_HEADER.getName())
                            .errorValue(cell.getStringCellValue())
                            .correctValue(excelFileProperties.getHeader3().get(i - colNumber))
                            .build());
                    log.error("validateCropsSub : " + (i - colNumber) + " : " + cell.getStringCellValue() + " : " + crop.getCropName() + " : failed");
                } else {
                    log.info("validateCropsSub : " + (i - colNumber) + " : " + cell.getStringCellValue() + " : " + crop.getCropName() + " : passed");
                }

            }
        }

    }

    private boolean validateCrops(Map<Integer, Crop> cropMapping,
                                  Cell cell) {
        log.info("validateCrops : checking crop on cell " + cell.getAddress() + " col " + cell.getColumnIndex());
        try {
            Crop crop = cropService.getCropByName(cell.getStringCellValue().trim());
            if (crop == null) {
                log.error("validateCrops : crop not valid");
                return false;
            } else {
                if (cropMapping.values().contains(crop)) {
                    return false;
                }
                cropMapping.put(cell.getColumnIndex(), crop);
                System.out.println(cropMapping);
                log.info("validateCrops : crop " + crop.getCropName() + " : passed");
                return true;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }

    }

    private boolean isRowNotEmpty(Row row) {
        Iterator<Cell> cells = row.cellIterator();
        while (cells.hasNext()) {
            Cell cell = cells.next();
            if (cell.getCellType() != CellType.BLANK) {
                return true;
            }
        }
//        System.out.println("Empty row");
        return false;
    }

    private void checkMandatoryFieldsExists(FarmerListMandatoryFieldsDto mandatory_fields,
                                            List<ExcelErrorResponse> errorList,
                                            int row) {
        if (!mandatory_fields.isUnitNoEUJAS()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Unit Number for EU / JAS is mandatory.")
                    .build());
        }
        if (!mandatory_fields.isFarCodeEUJAS()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Farmer Code for EU / JAS is mandatory.")
                    .build());

        }
        if (!mandatory_fields.isUnitNoNOP()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Unit Number for NOP is mandatory.")
                    .build());

        }
        if (!mandatory_fields.isFarCodeNOP()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("farmer code for NOP is mandatory.")
                    .build());

        }
        if (!mandatory_fields.isFarmerName()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Farmer Name is mandatory.")
                    .build());

        }
        if (!mandatory_fields.isFarmName()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Name of the Farm is mandatory.")
                    .build());

        }
        if (!mandatory_fields.isPlotCode()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Plot code is mandatory.")
                    .build());

        }
        if (!mandatory_fields.isTotalArea()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Total Area (Ha) is mandatory.")
                    .build());

        }
        if (!mandatory_fields.isCity()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Unit City is mandatory.")
                    .build());

        }
        if (!mandatory_fields.isStartingDateCon()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Starting date of Conversion period (yyyy-mm-dd) is mandatory.")
                    .build());

        }
        if (!mandatory_fields.isStartingDateOrg()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Starting date of Organic Period (yyyy-mm-dd) is mandatory.")
                    .build());

        }
        if (!mandatory_fields.isEujasField()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Field Status EU/JAS ic1/ic2/ic3/org is mandatory.")
                    .build());

        }
        if (!mandatory_fields.isEujasHarvest()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Row:" + row)
                    .error("Harvest status EU/JAS conv/ic/org is mandatory.")
                    .build());

        }

    }

    private int getListId() {
        return farmerListService.getLastFarmListId();
    }

    private String convertCellValueToStringValue(Cell cell) {
        CellType cellType = cell.getCellType();

        switch (cellType) {

            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return cell.getNumericCellValue() + "".trim();
            default:
                return "";

        }

    }

    private double convertCellValueToNumberValue(Cell cell) {
        CellType cellType = cell.getCellType();
        System.out.println(cellType);

        double val = 0;
        switch (cellType) {
            case STRING:
                val = Double.parseDouble(cell.getStringCellValue());
                break;
            case NUMERIC:
                val = cell.getNumericCellValue();
                break;
            case FORMULA:
                val = cell.getNumericCellValue();
                break;
            default:
                val = 0d;
                break;

        }
        log.info(className + ".convertCellValueToNumberValue : returning val " + val);
        return val;
    }

}
