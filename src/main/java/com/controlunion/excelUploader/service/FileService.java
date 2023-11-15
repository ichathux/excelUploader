package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.config.ExcelFilePropertiesConfig;
import com.controlunion.excelUploader.dto.*;
import com.controlunion.excelUploader.enums.Errors;
import com.controlunion.excelUploader.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileService {

    private final String className = this.getClass().getName();
    private final ExcelFilePropertiesConfig excelFileProperties;
    private final CropService cropService;
    private final FarmerListService farmerListService;
    private final FarmerListFinalService farmerListFinalService;
    private final PlanService planService;
    private final FarmerListCropFinalService farmerListCropFinalService;
    private final FarmerListDeletedService farmerListDeletedService;
    private long startTime;
    private long endTime;

    int lines = 0;

    public ResponseEntity uploadExcelFile(MultipartFile file,
                                          int projectId,
                                          int auditId,
                                          String projectName,
                                          int proId) {
        startTime = System.currentTimeMillis();
        log.info("File recieved " + file.getOriginalFilename());
        List<ExcelErrorResponse> errorList = new ArrayList<>();

        if (file.isEmpty()) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Uploaded file")
                    .error("File is not present")
                    .build());
            return ResponseEntity.badRequest().body(errorList);
        }

        if (!isFileExcel(file)) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Uploaded file")
                    .error("File format not support. It must be .xls or .xlsx")
                    .build());
            return ResponseEntity.badRequest().body(errorList);
        }
        return readFile(file, errorList, projectId, auditId, projectName, proId);

    }

    private boolean isFileExcel(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        assert fileName != null;
        return fileName.endsWith(".xls") || fileName.endsWith(".xlsx");
    }

    private ResponseEntity readFile(MultipartFile file,
                                    List<ExcelErrorResponse> errorList,
                                    int projectId,
                                    int auditId, String projectName, int proId) {

        List<FarmerList> farmerLists;

        try (InputStream inputStream = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0); // Assuming the data is on the first sheet
            int rowNumber = 0;

            validateAuditHeaders(sheet, errorList, projectId, projectName);

            final Iterator<Row> iterator = sheet.rowIterator();
            Map<Integer, Crop> cropMapping = new LinkedHashMap<>();
            Row row = findTableStart(iterator, errorList, rowNumber + 1);

            assert row != null;
            System.out.println("start validating headers - 1");
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            validate1stLevelHeaders(row, errorList, evaluator);

            System.out.println("end validating headers - 1");
            row = iterator.next();
            validate2ndLevelHeaders(row, errorList, cropMapping, sheet, evaluator);

            System.out.println("end validating headers - 2");
            iterator.next();
            long lastCertifiedAuditId;

            try {
                lastCertifiedAuditId = planService.getLastCertifiedPlanForProId(auditId, proId).getPlanID();
                log.info("Last certified Plan: " + auditId + " for proId: " + proId + " last audit id : " + lastCertifiedAuditId);
            } catch (NullPointerException e) {
                lastCertifiedAuditId = 0;
            }

            List<FarmerListFinal> fFinals;
            if (lastCertifiedAuditId != 0) {
                fFinals = farmerListFinalService
                        .getBeforeCertifiedFarmLIstFinal(proId, auditId);
            } else {
                fFinals = null;
            }

            FarmerListComparisonDto farmerListComparisonDto = readAuditData(iterator,
                    cropMapping,
                    errorList,
                    evaluator,
                    auditId,
                    proId,
                    lastCertifiedAuditId,
                    fFinals);

            System.out.println("Read audit data done. total line " + lines + " read");
            workbook.close();
            if (fFinals != null) {
                log.info("size of old list : " + fFinals.size());
            }
            log.info("size of new list : " + farmerListComparisonDto.getMap1().size());

            if (errorList.isEmpty()) {
                try {
                    if (fFinals != null) {
                        farmerLists = makeComparison(farmerListComparisonDto, fFinals, auditId);
                    } else {
                        farmerLists = new ArrayList<>(farmerListComparisonDto.getMap1().values());
                    }

                    endTime = System.currentTimeMillis();
                    log.info("task end : " + (endTime - startTime) + "ms");
                    farmerListService.saveFarmerList(proId, auditId, farmerLists);
                    endTime = System.currentTimeMillis();
                    log.info("task end : " + (endTime - startTime) + "ms");
                    return ResponseEntity.ok().build();
                } catch (DataIntegrityViolationException e) {
                    e.printStackTrace();
                    errorList.add(ExcelErrorResponse.builder()
                            .error("Data already contained. Project :" + projectName + " Audit : " + auditId).build());
                    return ResponseEntity.badRequest().body(errorList);
                } catch (Exception e) {
                    e.printStackTrace();
                    errorList.add(ExcelErrorResponse.builder()
                            .error(e.getMessage()).build());
                    return ResponseEntity.badRequest().body(errorList);
                }
            } else {
                return ResponseEntity.badRequest().body(errorList);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            errorList.add(ExcelErrorResponse.builder()
                    .error(e.getMessage()).build());
            return ResponseEntity.badRequest().body(errorList);
        } catch (Exception e) {
            e.printStackTrace();
            errorList.add(ExcelErrorResponse.builder()
                    .error(e.getMessage()).build());
            return ResponseEntity.badRequest().body(errorList);
        }
    }

    private FarmerListComparisonDto readAuditData(Iterator<Row> iterator,
                                                  Map<Integer, Crop> cropMapping,
                                                  List<ExcelErrorResponse> errorList,
                                                  FormulaEvaluator evaluator,
                                                  int auditId,
                                                  int proId,
                                                  long lastCertifiedAuditId,
                                                  List<FarmerListFinal> fFinals) {

        HashMap<String, ArrayList<String>> userMap = new HashMap<>(); // store farmer's plots
        int cuid = 0;
        String farmerCode = "";
        String plotCode = "";

        if (cropMapping.keySet().size() == 0) {
            errorList.add(ExcelErrorResponse.builder()
                    .error(Errors.REQUIRED_CROP.getName())
                    .build());
        }

        HashMap<String, FarmerListFinal> farmarCodeMappingWithFarmListFinal = new LinkedHashMap<>();
        HashMap<String, FarmerList> farmarCodeMappingWithFarmList = new LinkedHashMap<>();
        HashMap<String, Integer> farmerCodeVsCuid = new LinkedHashMap<>();

        while (iterator.hasNext()) {
            Row row = iterator.next();
            if (isRowEmpty(row)) {
                continue;
            }
            lines++;
            final Iterator<Cell> cellIterator = row.cellIterator();
            FarmerListMandatoryFieldsDto mandatory_fields = new FarmerListMandatoryFieldsDto();

            FarmerList farmerList = new FarmerList();
            farmerList.setAuditID(auditId);
            farmerList.setProID(proId);

            List<FarmerListCrop> farmerListCropList = new ArrayList<>();
            while (cellIterator.hasNext()) {

                Cell cell = cellIterator.next();

                if (cell.getCellType() == CellType.BLANK) {
                    continue;
                }

                if (cell.getCellType() == CellType.FORMULA) {
                    try {
                        cell = isFormulaProceed(cell, evaluator);
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
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format.")
                                    .build());
                        }
                        break;
                    case 1:
                        try {
                            farmerList.setUnitNoEUJAS(convertCellValueToStringValue(cell, errorList));
                            mandatory_fields.setUnitNoEUJAS(true);

                        } catch (Exception e) {
                            log.error(className + ".readAuditData : Unit Number for EU / JAS" + e.getMessage());

                        }

                        break;
                    case 2:
                        farmerCode = convertCellValueToStringValue(cell, errorList); //getting cell value form farmerCode
                        String finalFarmerCode = farmerCode.trim();
                        farmerList.setFarCodeEUJAS(farmerCode);     //sett farmerCode for farmerList object
                        mandatory_fields.setFarCodeEUJAS(true);     //set mandatory field contain
                        FarmerListFinal farmerListFinal;

                        try {
                            if (row.getCell(0) == null || convertCellValueToStringValue(cell, errorList).trim().equals("")) {
                                if (fFinals != null) {
                                    farmerListFinal = fFinals.stream()
                                            .filter(f -> f.getFarCodeEUJAS().trim().equals(finalFarmerCode))
                                            .findAny()
                                            .orElse(null);
                                    if (farmerListFinal == null) {
                                        farmerList.setIsNew(1);
//                                    new user
                                        if (farmerCodeVsCuid.containsKey(farmerCode)) {
                                            cuid = farmerCodeVsCuid.get(farmerCode);
                                        } else {
                                            cuid = farmerListFinalService.createCuid();
                                            farmerCodeVsCuid.put(farmerCode, cuid);
                                        }
                                    } else {
//                                    existing farmer send error message to user enter cuid
                                        cuid = farmerListFinal.getCufarmerID();
                                        farmerList.setListid(0);
//                                    errorList.add(ExcelErrorResponse.builder()
//                                            .error("You must enter cuid ")
//                                            .correctValue(String.valueOf(cuid))
//                                            .location("Row : " + (row.getRowNum() + 1))
//                                            .build());
                                    }
                                } else {
                                    farmerList.setIsNew(1);
                                    if (farmerCodeVsCuid.containsKey(farmerCode)) {
                                        cuid = farmerCodeVsCuid.get(farmerCode);
                                    } else {
                                        cuid = farmerListFinalService.createCuid();
                                        farmerCodeVsCuid.put(farmerCode, cuid);
                                    }
                                }
//                                System.out.println("empty cuid");

                            } else {
//                                System.out.println("contain cuid");
                                if (fFinals != null) {
                                    farmerListFinal = fFinals.stream()
                                            .filter(f -> f.getFarCodeEUJAS().trim().equals(finalFarmerCode))
                                            .findAny()
                                            .orElse(null);

                                    if (farmerListFinal == null) {
                                        farmerList.setIsNew(1);
                                        int finalCuid = cuid;
                                        farmerListFinal = fFinals.stream()
                                                .filter(f -> f.getCufarmerID() == finalCuid)
                                                .findAny().orElse(null);
                                        if (farmerListFinal != null) {
                                            System.out.println("found invalid farmer code for cuid " + cuid);
                                            farmerCode = farmerList.getFarCodeEUJAS();
                                        } else {
                                            farmerList.setIsNew(1);
                                            if (farmerCodeVsCuid.containsKey(farmerCode)) {
                                                cuid = farmerCodeVsCuid.get(farmerCode);
                                            } else {
                                                cuid = farmerListFinalService.createCuid();
                                                farmerCodeVsCuid.put(farmerCode, cuid);
                                            }
                                        }

                                    } else {
                                        farmerList.setIsNew(0);
                                        if (cuid != farmerListFinal.getCufarmerID()) {
//                                    cuid not matched send error message to user

                                            errorList.add(ExcelErrorResponse.builder()
                                                    .error("cuid not matched")
                                                    .correctValue(String.valueOf(farmerListFinal.getCufarmerID()))
                                                    .location("Row : " + (row.getRowNum() + 1))
                                                    .build());
                                            cuid = farmerListFinal.getCufarmerID();

                                        } else {
                                            cuid = farmerListFinal.getCufarmerID();

                                        }
                                    }
                                } else {
                                    if (farmerCodeVsCuid.containsKey(farmerCode)) {
                                        cuid = farmerCodeVsCuid.get(farmerCode);
                                    } else {
                                        cuid = farmerListFinalService.createCuid();
                                        farmerCodeVsCuid.put(farmerCode, cuid);
                                    }
                                    farmerList.setIsNew(1);
                                }
                            }

                        } catch (NullPointerException e) {
                            log.error("null pointer occurred " + e.getMessage());
                            e.printStackTrace();
                            log.error(className + ".readAuditData " + "location : " + cell.getAddress() + ": farCode mapping : " + e.getMessage());

                        }

                        farmerList.setCufarmerID(cuid);
                        userMap.putIfAbsent(cell.getStringCellValue().trim(), new ArrayList<>());
                        break;
                    case 3:
                        try {
                            farmerList.setUnitNoNOP(convertCellValueToStringValue(cell, errorList));
                            mandatory_fields.setUnitNoNOP(true);

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + ".readAuditData : Unit Number for NOP : " + convertCellValueToStringValue(cell));
                        break;
                    case 4:
                        try {
                            farmerList.setFarCodeNOP(convertCellValueToStringValue(cell, errorList));
                            mandatory_fields.setFarCodeNOP(true);

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + ".readAuditData : farmer code for NOP : " + convertCellValueToStringValue(cell));
                        break;
                    case 5:
                        try {
                            farmerList.setFarmerName(convertCellValueToStringValue(cell, errorList));
                            mandatory_fields.setFarmerName(true);

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + ".readAuditData : Farmer Name : " + convertCellValueToStringValue(cell));
                        break;
                    case 6:
                        try {
                            farmerList.setFarmName(convertCellValueToStringValue(cell, errorList));
                            mandatory_fields.setFarmName(true);

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + ".readAuditData : Name of the Farm : " + convertCellValueToStringValue(cell));
                        break;
                    case 7:
                        plotCode = convertCellValueToStringValue(cell, errorList);
                        farmerList.setPlotCode(plotCode);
                        mandatory_fields.setPlotCode(true);
//                        log.info(className + ".readAuditData : Checking duplicate plots for farmer : " + farmerCode + " with " + plotCode);
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
                            farmerList.setTotalArea(cell.getNumericCellValue());
                            mandatory_fields.setTotalArea(true);
//                            log.info(className + ".readAuditData : Total Area (Ha) : " + cell.getNumericCellValue());
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error(Errors.REQUIRED_NUMBER_VALUE.getName())
                                    .build());
                            log.error(className + ".readAuditData " + "location : " + cell.getAddress() + ": Total Area (Ha) : " + e.getMessage());
                        }
                        break;
                    case 9:
                        try {
                            farmerList.setGps(convertCellValueToStringValue(cell, errorList));

                        } catch (IllegalStateException e) {
                            log.error(className + ".readAuditData : Cell:" + cell.getAddress() + "" + e.getMessage());
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + ".readAuditData : GPS : " + convertCellValueToStringValue(cell));

                        break;
                    case 10:
                        try {
                            farmerList.setAddress(convertCellValueToStringValue(cell, errorList));

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + ".readAuditData : Address/Village : " + convertCellValueToStringValue(cell));

                        break;
                    case 11:
                        try {
                            farmerList.setCity(convertCellValueToStringValue(cell, errorList));
                            mandatory_fields.setCity(true);
//                            log.info(className + ".readAuditData : City : " + convertCellValueToStringValue(cell));
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format.")
                                    .build());
                        }

                        break;
                    case 12:
                        try {
                            farmerList.setDateCert(new Date(cell.getDateCellValue().getTime()));
//                            log.info(className + ".readAuditData : Application date for certification (yyyy-mm-dd) : " + dateCert);
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format. Change cell format to Date.")
                                    .build());
                        }
                        break;
                    case 13:
                        try {
                            farmerList.setAplyRetrospe(convertCellValueToStringValue(cell, errorList).trim().equalsIgnoreCase("yes") ? 1 : 0);

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + ".readAuditData : Applying for Retrospective consideration (Yes/No) : " + convertCellValueToStringValue(cell));

                        break;
                    case 14:
                        try {
                            String certifications = convertCellValueToStringValue(cell, errorList);
//                            System.out.println(certifications);
                            certifications = certifications.replaceAll("/", ",");
//                            System.out.println(certifications);
                            farmerList.setCertification(certifications);

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + ".readAuditData : Certifications : " + certification);

                        break;
                    case 15:
                        try {
                            farmerList.setFertilizer(convertCellValueToStringValue(cell, errorList));

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + ".readAuditData : Types of fertilizer, pesticide used : " + convertCellValueToStringValue(cell));

                        break;
                    case 16:
                        try {
                            farmerList.setFerUseDate(convertCellValueToStringValue(cell, errorList));

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + ".readAuditData : Last date of use : " + convertCellValueToStringValue(cell));
                        break;
                    case 17:
                        try {
                            farmerList.setDateConfersion(new Date(cell.getDateCellValue().getTime()));
                            mandatory_fields.setStartingDateCon(true);
//                            log.info(className + ".readAuditData : Starting date of Conversion period (yyyy-mm-dd) : " + new java.sql.Date(cell.getDateCellValue().getTime()));
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format. Change cell format to Date.")
                                    .build());
                            log.error(className + "Location" + cell.getAddress() + ".readAuditData : Starting date of Conversion period (yyyy-mm-dd) : " + e.getMessage());
                        }

                        break;
                    case 18:
                        try {
                            farmerList.setDateorganic(new Date(cell.getDateCellValue().getTime()));
                            mandatory_fields.setStartingDateOrg(true);
//                            log.info(className + "readAuditData : Starting date of Organic Period (yyyy-mm-dd) : " + new Date(cell.getDateCellValue().getTime()));

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format. Change cell format to Date.")
                                    .build());
                            log.error(className + "Location" + cell.getAddress() + ".readAuditData : Starting date of Organic Period (yyyy-mm-dd) : " + e.getMessage());

                        }
                        break;
                    case 19:
                        try {
                            farmerList.setEujas_field(convertCellValueToStringValue(cell, errorList));
                            mandatory_fields.setEujasField(true);
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }

//                        log.info(className + "readAuditData : Field Status EU/JAS ic1/ic2/ic3/org : " + convertCellValueToStringValue(cell));
                        break;
                    case 20:
                        try {
                            farmerList.setEujas_harvest(convertCellValueToStringValue(cell, errorList));
                            mandatory_fields.setEujasHarvest(true);
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }

//                        log.info(className + "readAuditData : Harvest status EU/JAS conv/ic/org : " + convertCellValueToStringValue(cell));
                        break;
                    case 21:
                        try {
                            farmerList.setUsda_field(convertCellValueToStringValue(cell, errorList));

                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + "readAuditData : Field status NOP ic1/ic2/ic3/org : " + convertCellValueToStringValue(cell));
                        break;
                    case 22:
                        try {
                            farmerList.setUsda_harvest(convertCellValueToStringValue(cell, errorList));
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }
//                        log.info(className + "readAuditData : Harvest status NOP conv/org : " + convertCellValueToStringValue(cell));
                        break;
//                    reading crop section
                    default:

                        try {
                            readingCropsData(cropMapping, errorList, cuid, plotCode, row, farmerListCropList, cell);
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("Invalid input type")
                                    .build());
                        }

                        break;
                }
            }
//            log.info(className + "readAuditData : found " + farmerListCropList.size() + " for farmer crop list");
            checkMandatoryFieldsExists(mandatory_fields, errorList, (row.getRowNum() + 1));
            farmerList.setUser("isuru");
            farmerList.setGps("");
            farmerList.setChngCropdata("");
            farmerList.setChngFarmdata("");
            farmerList.setSysTimeStamp(new Date(System.currentTimeMillis()));
            farmerList.setInspected(0);
            farmerList.setFarmerListCropList(farmerListCropList);
//            log.info("adding farmlist to hashmap " + farmerCode);
            farmarCodeMappingWithFarmList.put(farmerCode + " plot " + plotCode, farmerList);

        }
        farmerCodeVsCuid.clear();
        return new FarmerListComparisonDto(proId,
                lastCertifiedAuditId,
                farmarCodeMappingWithFarmList,
                farmarCodeMappingWithFarmListFinal);

    }

    private void readingCropsData(Map<Integer, Crop> cropMapping,
                                  List<ExcelErrorResponse> errorList,
                                  int cuid,
                                  String plotCode,
                                  Row row,
                                  List<FarmerListCrop> farmerListCropList,
                                  Cell cell) {
        int i;
        try {
            if (cropMapping.containsKey(cell.getColumnIndex())) {

//                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++reading : " + cropMapping.get(cell.getColumnIndex()).getCropName());

                Crop crop = cropMapping.get(cell.getColumnIndex());
                FarmerListCrop farmerListCrop = new FarmerListCrop();
                farmerListCrop.setCropID(crop.getCropID());
                farmerListCrop.setPlotCode(plotCode);
                farmerListCrop.setCufarmerID(cuid);
                int currentCellStart = cell.getColumnIndex();

                for (i = 0; i < excelFileProperties.getHeader3().size(); i++) {
                    try {
                        switch (i) {
                            case 0:
                                try {
                                    farmerListCrop.setNoOfPlant(convertCellValueToNumberValue(row.getCell(currentCellStart + i)));

                                } catch (NullPointerException e) {
                                    farmerListCrop.setNoOfPlant(0);
                                }
                                break;
                            case 1:
                                try {
                                    farmerListCrop.setEstiYield(convertCellValueToNumberValue(row.getCell(currentCellStart + i)));

                                } catch (NullPointerException e) {
                                    farmerListCrop.setEstiYield(0);
                                }
                                break;
                            case 2:
                                try {
                                    farmerListCrop.setRealYield(convertCellValueToNumberValue(row.getCell(currentCellStart + i)));
                                } catch (NullPointerException e) {
                                    farmerListCrop.setRealYield(0);
                                }
                                break;
                            case 3:
                                try {

                                    farmerListCrop.setNoOfSesons(convertCellValueToNumberValue(row.getCell(currentCellStart + i)));

                                } catch (NullPointerException e) {
                                    farmerListCrop.setNoOfSesons(0);
                                }
                                break;
                        }
                    } catch (NullPointerException e) {
                        log.error("null pointer occurred when reading crops data");
//                        errorList.add(ExcelErrorResponse.builder()
//                                .error("Format not valid")
//                                .location("Row "+String.valueOf(row.getRowNum()+1)
//                                        +" cell "+(excelFileProperties.getHeader3().get(i) +
//                                        "for : Crop "+crop.getCropName()))
//                                .build());
                        e.printStackTrace();
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
    }

    private ArrayList<FarmerList> makeComparison(FarmerListComparisonDto dto,
                                                 List<FarmerListFinal> farmerListFinals,
                                                 int auditID) {

        log.info("start comparison with farmlist with farmlist_final " + farmerListFinals.size());
        ArrayList<FarmerList> farmerLists = new ArrayList<>();
        for (Map.Entry<String, FarmerList> entry : dto.getMap1().entrySet()) {
            FarmerListFinal aFinal = farmerListFinals
                    .stream()
                    .filter(
                            (s) ->
                                    s.getCufarmerID() == entry.getValue().getCufarmerID() &&
                                            s.getPlotCode().trim().equalsIgnoreCase(entry.getValue().getPlotCode())
                    )
                    .findAny()
                    .orElse(null);

            FarmerList farmerList = compareFarmData(entry, aFinal);
            farmerLists.add(farmerList);
            farmerListFinals.remove(aFinal);
        }
        System.out.println("comparison done " + farmerLists.size());

        if (!farmerListFinals.isEmpty()) {
            System.out.println(farmerListFinals.size() + " deleted");
            farmerListDeletedService.addDataToFarmListDeleted(farmerListFinals, auditID);
        }
        return farmerLists;
    }

    public String convertFarmDataChangesTo(ArrayList<FarmChangesDto> list) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Error converting HashMap to JSON";
        }
    }

    public String convertCropDataChangesTo(ArrayList<ChangesDto> list) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Error converting HashMap to JSON";
        }
    }

    public String convertArrayListToJsonCrops(ArrayList<ChangesDto> arrayList) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayList<String> list = new ArrayList<>();

            for (ChangesDto chnges : arrayList) {
                list.add(convertHashMapToJsonCrops(chnges));
            }
            return objectMapper.writeValueAsString(arrayList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

    }

    public String convertHashMapToJsonCrops(ChangesDto map) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Error converting HashMap to JSON";
        }
    }

    private FarmerList compareFarmData(Map.Entry<String, FarmerList> entry,
                                       FarmerListFinal aFinal) {

        if (entry.getValue().getIsNew() == 1) {
            return entry.getValue();
        }
        String plot = entry.getKey().split("plot")[1];
        ArrayList<FarmChangesDto> farmChanges = new ArrayList<>();
        ArrayList<ChangesDto> cropChanges = new ArrayList<>();
        FarmerList farmerList = entry.getValue();

        if (aFinal == null) {

            System.out.println("new plot");
            farmChanges.add(new FarmChangesDto("new plot", plot, null));
            farmerList.setChngCropdata("[]");

        } else {
            if (!aFinal.getFarmerName().trim().equals(farmerList.getFarmerName())) {
                farmChanges.add(new FarmChangesDto("Farmer Name", aFinal.getFarmerName(), farmerList.getFarmerName()));
            }
            if (!aFinal.getFarmName().trim().equals(farmerList.getFarmName())) {
                farmChanges.add(new FarmChangesDto("Name of the Farm", aFinal.getFarmName(), farmerList.getFarmName()));
            }
            if (aFinal.getTotalArea() != farmerList.getTotalArea()) {
                farmChanges.add(new FarmChangesDto("Total Area", String.valueOf(aFinal.getTotalArea()), String.valueOf(farmerList.getTotalArea())));
            }
            if (!aFinal.getGps().trim().equals(farmerList.getGps())) {
                farmChanges.add(new FarmChangesDto("GPS", aFinal.getGps(), farmerList.getGps()));
            }
            if (!aFinal.getAddress().trim().equals(farmerList.getAddress())) {
                farmChanges.add(new FarmChangesDto("Address", aFinal.getAddress(), farmerList.getAddress()));
            }
            if (!aFinal.getCity().trim().equals(farmerList.getCity())) {
                farmChanges.add(new FarmChangesDto("City", aFinal.getCity(), farmerList.getCity()));
            }
            if (!aFinal.getDateCert().equals(farmerList.getDateCert())) {
                farmChanges.add(new FarmChangesDto("Application date for certification (yyyy-mm-dd)", String.valueOf(aFinal.getDateCert()), String.valueOf(farmerList.getDateCert())));
            }
            if (aFinal.getAplyRetrospe() != farmerList.getAplyRetrospe()) {
                farmChanges.add(new FarmChangesDto("Applying for Retrospective consideration (Yes/No)", String.valueOf(aFinal.getAplyRetrospe()), String.valueOf(farmerList.getAplyRetrospe())));
            }
            if (!aFinal.getCertification().trim().equals(farmerList.getCertification())) {
                farmChanges.add(new FarmChangesDto("Certifications", aFinal.getCertification(), farmerList.getCertification()));
            }
            if (!aFinal.getFertilizer().trim().equals(farmerList.getFertilizer())) {
                farmChanges.add(new FarmChangesDto("Types of fertilizer, pesticide used", aFinal.getFertilizer(), farmerList.getFertilizer()));
            }
            if (!aFinal.getFerUseDate().equals(farmerList.getFerUseDate())) {
                farmChanges.add(new FarmChangesDto("Last date of use ", aFinal.getFerUseDate(), farmerList.getFerUseDate()));
            }
            if (!aFinal.getDateConfersion().equals(farmerList.getDateConfersion())) {
                farmChanges.add(new FarmChangesDto("Starting date of Conversion period (yyyy-mm-dd)", String.valueOf(aFinal.getDateConfersion()), String.valueOf(farmerList.getDateConfersion())));
            }
            if (!aFinal.getDateorganic().equals(farmerList.getDateorganic())) {
                farmChanges.add(new FarmChangesDto("Starting date of Organic Period (yyyy-mm-dd)", String.valueOf(aFinal.getDateorganic()), String.valueOf(farmerList.getDateorganic())));
            }
            if (!aFinal.getEujas_field().trim().equals(farmerList.getEujas_field())) {
                farmChanges.add(new FarmChangesDto("Field Status EU/JAS ic1/ic2/ic3/org", aFinal.getEujas_field(), farmerList.getEujas_field()));
            }
            try {
                if (!aFinal.getEujas_harvest().trim().equals(farmerList.getEujas_harvest())) {
                    farmChanges.add(new FarmChangesDto("Harvest status EU/JAS conv/ic/org", aFinal.getEujas_harvest(), farmerList.getEujas_harvest()));
                }
            } catch (NullPointerException e) {
                if (null != farmerList.getEujas_harvest()) {
                    farmChanges.add(new FarmChangesDto("Harvest status EU/JAS conv/ic/org", null, farmerList.getEujas_harvest()));
                }
                e.printStackTrace();
            }

            if (!aFinal.getUsda_field().trim().equals(farmerList.getUsda_field())) {
                farmChanges.add(new FarmChangesDto("Field status NOP ic1/ic2/ic3/org", aFinal.getUsda_field(), farmerList.getUsda_field()));
            }
            if (!aFinal.getUsda_harvest().trim().equals(farmerList.getUsda_harvest())) {
                farmChanges.add(new FarmChangesDto("Harvest status NOP conv/org", aFinal.getUsda_harvest(), farmerList.getUsda_harvest()));
            }

            List<FarmerListCrop> farmerListCrops = farmerList.getFarmerListCropList();
//            List<FarmerListCropFinal> farmerListCropFinals = farmerListCropFinalService
//                    .findFarmerListCropFinalsForFarmerFarmerListFinal(aFinal);

            List<FarmerListCropFinal> farmerListCropFinals = aFinal.getFarmerListCropFinalList();


            cropChanges = compareCropsData(farmerListCropFinals,
                    farmerListCrops);

//            if (!cropChanges.isEmpty()){
            String cropChangesStr = convertCropDataChangesTo(cropChanges);
            farmerList.setChngCropdata(cropChangesStr);
//            }

        }
        if (cropChanges.size() > 0 || farmChanges.size() > 0) {
            farmerList.setIsChange(1);
        }
        farmerList.setChngFarmdata(convertFarmDataChangesTo(farmChanges));
        return farmerList;
    }

    private ArrayList<ChangesDto> compareCropsData(List<FarmerListCropFinal> farmerListCrops_final,
                                                   List<FarmerListCrop> farmerListCrops) {

        ArrayList<Integer> checkedCrops = new ArrayList<>();
        ArrayList<ChangesDto> changesMap = new ArrayList<>();
//        log.info("comparing with ");
        try {
            for (FarmerListCropFinal crop_final : farmerListCrops_final) {

                checkedCrops.add(crop_final.getCropID());

                FarmerListCrop currentFarmerlistCrop = farmerListCrops.stream()
                        .filter(s -> s.getCropID() == crop_final.getCropID()
                                && s.getPlotCode().trim().equals(crop_final.getPlotCode()))
                        .findFirst()
                        .orElse(null);

                String cropName = cropService.getCropNameById(crop_final.getCropID()).getCropName();
                if (currentFarmerlistCrop == null) {
//                    check crop deleted
                    changesMap.add(new ChangesDto(currentFarmerlistCrop, cropName + " : Number of Plants ", crop_final.getNoOfPlant(), 0));
                    changesMap.add(new ChangesDto(currentFarmerlistCrop, cropName + " : Estimated Yield (Kg) / year ", crop_final.getEstiYield(), 0));
                    changesMap.add(new ChangesDto(currentFarmerlistCrop, cropName + " : No of Seasons ", crop_final.getNoOfSesons(), 0));
                    changesMap.add(new ChangesDto(currentFarmerlistCrop, cropName + " : Realistic Yield in Last year (kg) ", crop_final.getRealYield(), 0));
                } else {
//                    check crop changes
                    if (crop_final.getNoOfPlant() != currentFarmerlistCrop.getNoOfPlant()) {
                        changesMap.add(new ChangesDto(currentFarmerlistCrop, cropName + " : Number of Plants", crop_final.getNoOfPlant(), currentFarmerlistCrop.getNoOfPlant()));
                    }
                    if (crop_final.getEstiYield() != currentFarmerlistCrop.getEstiYield()) {
                        changesMap.add(new ChangesDto(currentFarmerlistCrop, cropName + " : Estimated Yield (Kg) / year", crop_final.getEstiYield(), currentFarmerlistCrop.getEstiYield()));
                    }
                    if (crop_final.getRealYield() != currentFarmerlistCrop.getRealYield()) {
                        changesMap.add(new ChangesDto(currentFarmerlistCrop, cropName + " : Realistic Yield in Last year (kg)", crop_final.getRealYield(), currentFarmerlistCrop.getRealYield()));
                    }
                    if (crop_final.getNoOfSesons() != currentFarmerlistCrop.getNoOfSesons()) {
                        changesMap.add(new ChangesDto(currentFarmerlistCrop, cropName + " : No of Seasons", crop_final.getNoOfSesons(), currentFarmerlistCrop.getNoOfSesons()));
                    }
                }
            }
//            check new crop added
            farmerListCrops.stream()
                    .filter(crop -> !checkedCrops.contains(crop.getCropID()))
                    .forEach(crop -> {
                        String cropName = cropService.getCropNameById(crop.getCropID()).getCropName();
                        changesMap.add(new ChangesDto(crop, cropName + " Estimated Yield (Kg) / year", 0, crop.getEstiYield()));
                        changesMap.add(new ChangesDto(crop, cropName + " Number of Plants", 0, crop.getNoOfPlant()));
                        changesMap.add(new ChangesDto(crop, cropName + " No of Seasons", 0, crop.getNoOfSesons()));
                        changesMap.add(new ChangesDto(crop, cropName + " Realistic Yield in Last year (kg)", 0, crop.getRealYield()));
                    });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error occured while comparing crops " + e.getMessage());
        }

        return changesMap;
    }


    private Cell isFormulaProceed(Cell cell,
                                  FormulaEvaluator evaluator) {
        try {
//            log.info("**********Formular*********" + cell.getAddress());
            CellValue cellValue = evaluator.evaluate(cell);
            StringBuilder result = new StringBuilder(cellValue.formatAsString());
//            log.info("evaluated result " + result);

            if (cellValue.getCellType() == CellType.NUMERIC) {
//                log.info("setting formula evaluated value " + result + " parent cell type : ");
                cell.setCellValue(result.toString());

            } else {
                cell.setCellValue(result.substring(1, result.length() - 1));
            }

//            log.info(className + ".isFormulaProceed : getting result : result of formula : " + cell.getStringCellValue());
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
        for (int i = 0; i < auditDetails.size(); i++) {
            XSSFRow row = sheet.getRow(i);

            try {
                XSSFCell cell = row.getCell(0);
                String title = cell.getStringCellValue().trim().toLowerCase(Locale.ROOT);
                String auditTitle = auditDetails.get(i);
                System.out.println("validating - " + title);
                if (!title.equalsIgnoreCase(auditTitle)) {
                    errorList.add(ExcelErrorResponse.builder()
                            .location("Cell " + cell.getAddress())
                            .error(Errors.INVALID_HEADER.getName())
                            .errorValue(cell.getStringCellValue().trim())
                            .correctValue(auditDetails.get(i))
                            .build());
                } else {
                    System.out.println("validating -else- " + title);
                    switch (title) {
                        case "project name":
                            System.out.println("project mame");
                            validateProjectName(projectName, row, errorList);
                            break;
                        case "project id":
                            System.out.println("project id");
                            validateProjectId(projectId, row, errorList);
                            break;

                    }
                }
            } catch (NullPointerException e) {
                errorList.add(ExcelErrorResponse.builder().location("Row " + (i + 1)).error(Errors.EMPTY_ROW.getName()).build());
                errorList.add(ExcelErrorResponse.builder().location("Row " + (i + 1)).error(Errors.MUST_BE.getName()).correctValue(auditDetails.get(i)).build());
            }
        }
    }

    private void validateProjectId(int projectId, XSSFRow row, List<ExcelErrorResponse> errorList) {
        try {
            XSSFCell cell = row.getCell(1);
            try {
                if (projectId != cell.getNumericCellValue()) {
                    errorList.add(ExcelErrorResponse.builder()
                            .location("Cell " + cell.getAddress())
                            .error(Errors.PROJECT_CODE_MISMATCH.getName())
                            .errorValue(convertCellValueToStringValue(cell, errorList))
                            .correctValue(String.valueOf(projectId))
                            .build());
                } else {

                }
            } catch (IllegalStateException e) {
                errorList.add(ExcelErrorResponse.builder()
                        .location("Cell " + cell.getAddress())
                        .error(Errors.REQUIRED_NUMBER_VALUE.getName())
                        .errorValue(cell.getStringCellValue().trim())
                        .correctValue(String.valueOf(projectId))
                        .build());
            }
        } catch (NullPointerException e) {
            errorList.add(ExcelErrorResponse.builder()
                    .location("Cell ")
                    .error(Errors.MANDATORY_CELL.getName())
                    .correctValue(String.valueOf(projectId))
                    .build());
        }
    }

    private void validateProjectName(String projectName, XSSFRow row, List<ExcelErrorResponse> errorList) {
        System.out.println("Checkign project name");
        try {
            XSSFCell cell = row.getCell(1);
            try {

                if (!projectName.trim().equals(cell.getStringCellValue().trim())) {
                    errorList.add(ExcelErrorResponse.builder()
                            .location("Cell " + cell.getAddress())
                            .error(Errors.PROJECT_NAME_MISMATCH.getName())
                            .errorValue(convertCellValueToStringValue(cell, errorList))
                            .correctValue(projectName)
                            .build());

                    log.error(Errors.PROJECT_NAME_MISMATCH.getName() + "cell " + cell.getAddress());
                } else {
                    System.out.println("project name matched");
                }
            } catch (IllegalStateException e) {
                errorList.add(ExcelErrorResponse.builder()
                        .location("Cell " + cell.getAddress())
                        .error(Errors.REQUIRED_TEXT_VALUE.getName())
                        .errorValue(cell.getStringCellValue().trim())
                        .correctValue(projectName)
                        .build());
            }
        } catch (NullPointerException e) {
            log.error(Errors.MANDATORY_CELL.getName());
            errorList.add(ExcelErrorResponse.builder()
                    .error(Errors.MANDATORY_CELL.getName())
                    .correctValue(String.valueOf(projectName))
                    .build());
        }
    }

    private Row findTableStart(Iterator<Row> rowIterator,
                               List<ExcelErrorResponse> errorList,
                               int rowNumber) {

        System.out.println(rowIterator.next());
//        findingTableStartOnRows:
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (isRowEmpty(row)) {
                ExcelErrorResponse error = ExcelErrorResponse.builder().location("Row " + (row.getRowNum() + 1)).error(Errors.EMPTY_ROW.getName()).build();
                if (!errorList.contains(error)) {
                    errorList.add(error);
                }
            }

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
            }
            final Iterator<Cell> cellIterator = row.cellIterator();
            int matchingTableHeaders = 0;
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                try {
                    if (cell.getStringCellValue().trim().equals(excelFileProperties.getHeader1().get(cell.getColumnIndex()))) {
                        matchingTableHeaders++;
                    }
                    if (matchingTableHeaders > (excelFileProperties.getHeader1().size() / 2)) {
                        return row;
                    }
                } catch (IllegalStateException ignored) {

                }
            }
            try {
                if (!excelFileProperties.getAuditDetails().containsValue(row.getCell(0).getStringCellValue())) {
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
            } catch (NullPointerException ignored) {

            }
            rowNumber++;
        }

        return null;
    }


    private void validate1stLevelHeaders(Row row,
                                         List<ExcelErrorResponse> errorList,
                                         FormulaEvaluator evaluator) {
        for (Cell cell : row) {
            if (cell.getCellType() == CellType.FORMULA) {
                cell = isFormulaProceed(cell, evaluator);
            }
            try {
                if (excelFileProperties.getHeader1().containsKey(cell.getColumnIndex())) {
                    if (!cell.getStringCellValue().trim().equals(excelFileProperties.getHeader1().get(cell.getColumnIndex()))) {
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
                                         XSSFSheet sheet,
                                         FormulaEvaluator evaluator) {
        final Iterator<Cell> cellIterator = row.iterator();
        int index = 0;

        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            System.out.println("valadating headers " + cell.getStringCellValue());
            if (cell.getCellType() == CellType.FORMULA) {
                cell = isFormulaProceed(cell, evaluator);
            }
            try {
                if (excelFileProperties.getHeader2().containsKey(cell.getColumnIndex())) {
                    if (!cell.getStringCellValue().trim().equals(excelFileProperties.getHeader2().get(cell.getColumnIndex()))) {
                        errorList.add(ExcelErrorResponse.builder()
                                .location("Cell " + cell.getAddress())
                                .error(Errors.INVALID_HEADER.getName())
                                .errorValue(cell.getStringCellValue().trim())
                                .correctValue(excelFileProperties.getHeader2()
                                        .get(cell.getColumnIndex())).build());
                    }
                }
            } catch (NullPointerException | IllegalStateException e) {
                errorList.add(ExcelErrorResponse.builder()
                        .location("Cell " + cell.getAddress())
                        .error(Errors.INVALID_HEADER.getName())
                        .errorValue(cell.getStringCellValue().trim())
                        .correctValue(excelFileProperties.getHeader2()
                                .get(cell.getColumnIndex())).build());
            }
            index++;
            if (index > excelFileProperties.getHeader2().size() - 1) {
                break;
            }
        }

        validateCropsHeaders(cellIterator, errorList, cropMapping, sheet, evaluator);
    }

    private void validateCropsHeaders(Iterator<Cell> cellIterator,
                                      List<ExcelErrorResponse> errorList,
                                      Map<Integer, Crop> cropMapping,
                                      XSSFSheet sheet,
                                      FormulaEvaluator evaluator) {
//        System.out.println("ready validating crops " + lastColNumber);
        ArrayList<Cell> cells = new ArrayList<>();

        while (cellIterator.hasNext()) {
            cells.add(cellIterator.next());
        }

        for (Cell cell : cells) {

            if (cell.getCellType() == CellType.FORMULA) {
                cell = isFormulaProceed(cell, evaluator);
            }
            if (cell.getCellType() == CellType.BLANK) {
                continue;
            }
            System.out.println("validating crop " + cell.getStringCellValue());
            boolean isCropValid = validateCrops(cropMapping, cell);

            if (!isCropValid) {
                errorList.add(ExcelErrorResponse.builder()
                        .location("Cell " + cell.getAddress())
                        .error(Errors.CROP_NOT_VALID.getName())
                        .errorValue(cell.getStringCellValue().trim())
                        .build());
            } else {

                Crop crop = cropMapping.get(cell.getColumnIndex());
//                log.info("crop " + crop.getCropName() + " : passed");
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
                            .errorValue(cell.getStringCellValue().trim())
                            .correctValue(excelFileProperties.getHeader3().get(i - colNumber))
                            .build());
                    log.error("validateCropsSub : " + (i - colNumber) + " : " + cell.getStringCellValue().trim() + " : " + crop.getCropName() + " : failed");
                } else {
//                    log.info("validateCropsSub : " + (i - colNumber) + " : " + cell.getStringCellValue() + " : " + crop.getCropName() + " : passed");
                }

            }
        }

    }

    private boolean validateCrops(Map<Integer, Crop> cropMapping,
                                  Cell cell) {
        try {
            Crop crop = cropService.getCropByName(cell.getStringCellValue().trim());
            if (crop == null) {
                log.error("validateCrops : crop not valid");
                return false;
            } else {
                if (cropMapping.containsValue(crop)) {
                    return false;
                }
                cropMapping.put(cell.getColumnIndex(), crop);
                return true;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }
    }


    private boolean isRowEmpty(Row row) {
        Iterator<Cell> cells = row.cellIterator();
        while (cells.hasNext()) {
            Cell cell = cells.next();
            if (cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
//        System.out.println("Empty row");
        return true;
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
//        if (!mandatory_fields.isStartingDateCon()) {
//            errorList.add(ExcelErrorResponse.builder()
//                    .location("Row:" + row)
//                    .error("Starting date of Conversion period (yyyy-mm-dd) is mandatory.")
//                    .build());
//
//        }
//        if (!mandatory_fields.isStartingDateOrg()) {
//            errorList.add(ExcelErrorResponse.builder()
//                    .location("Row:" + row)
//                    .error("Starting date of Organic Period (yyyy-mm-dd) is mandatory.")
//                    .build());
//
//        }
//        if (!mandatory_fields.isEujasField()) {
//            errorList.add(ExcelErrorResponse.builder()
//                    .location("Row:" + row)
//                    .error("Field Status EU/JAS ic1/ic2/ic3/org is mandatory.")
//                    .build());
//
//        }
//        if (!mandatory_fields.isEujasHarvest()) {
//            errorList.add(ExcelErrorResponse.builder()
//                    .location("Row:" + row)
//                    .error("Harvest status EU/JAS conv/ic/org is mandatory.")
//                    .build());
//
//        }

    }


    private String convertCellValueToStringValue(Cell cell, List<ExcelErrorResponse> errorList) {
        CellType cellType = cell.getCellType();
        switch (cellType) {
            case STRING:
                if (!isFormulaLegal(cell.getStringCellValue().trim())) {

                    errorList.add(ExcelErrorResponse.builder()
                            .error(cell.getAddress().toString())
                            .error("Invalid use of formula.")
                            .correctValue("Remove \"\" in " + cell.getStringCellValue())
                            .build());
                    return cell.getStringCellValue().trim();
                }
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return cell.getNumericCellValue() + "".trim();
            case FORMULA:
                return cell.getStringCellValue().trim();
            default:
                return cell.getStringCellValue().trim();

        }
    }

    private boolean isFormulaLegal(String stringCellValue) {
        try {
            if (stringCellValue.startsWith("\"=")) {
                System.out.println(stringCellValue);
                return false;
            }
        } catch (Exception ignore) {

        }

        return true;
    }

    private double convertCellValueToNumberValue(Cell cell) {
        try {
            CellType cellType = cell.getCellType();

            double val;
            switch (cellType) {
                case STRING:
                    val = Double.parseDouble(cell.getStringCellValue().trim());
                    break;
                case NUMERIC:
                case FORMULA:
                    val = cell.getNumericCellValue();
                    break;
                default:
                    val = 0d;
                    break;

            }
            return val;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }


    }


}
