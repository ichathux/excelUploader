package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.config.ExcelFilePropertiesConfig;
import com.controlunion.excelUploader.dto.ChangesDto;
import com.controlunion.excelUploader.dto.ExcelErrorResponse;
import com.controlunion.excelUploader.dto.FarmerListComparisonDto;
import com.controlunion.excelUploader.dto.FarmerListMandatoryFieldsDto;
import com.controlunion.excelUploader.enums.Errors;
//import com.controlunion.excelUploader.fileUpload.dto.ProgressMessage;
import com.controlunion.excelUploader.fileUpload.dto.ProgressMessage;
import com.controlunion.excelUploader.fileUpload.dto.UpoadProgressDto;
import com.controlunion.excelUploader.fileUpload.dto.WebSocketResponse;
import com.controlunion.excelUploader.fileUpload.testNew.Greeting;
import com.controlunion.excelUploader.fileUpload.testNew.GreetingController;
import com.controlunion.excelUploader.fileUpload.testNew.HelloMessage;
import com.controlunion.excelUploader.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
public class FileServiceNewTest {

    private static int newUser = 0;
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

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


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
//        System.out.println(fileName);
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
            sendProgressUpdate(5, "Validating sheet");

            validateAuditHeaders(sheet, errorList, projectId, projectName);
//            log.info(className + ".readFile : returned row number " + rowNumber);
//            log.error(className + ".readFile : Audit headers errors " + errorList);

            final Iterator<Row> iterator = sheet.rowIterator();
            Map<Integer, Crop> cropMapping = new LinkedHashMap<>();
            sendProgressUpdate(10, "Validating headers");
            Row row = findTableStart(iterator, errorList, rowNumber + 1);

//            log.info(className + ".readFile : returned row number " + rowNumber);
//            log.error(className + ".readFile : until table start errors " + errorList);
//            log.info(className + ".readFile : table start point : " + rowNumber);

            assert row != null;
            System.out.println("start validating headers - 1");
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            validate1stLevelHeaders(row, errorList, evaluator);
            System.out.println("end validating headers - 1");
            row = iterator.next();
//            System.out.println("start validating headers - 2");
            validate2ndLevelHeaders(row, errorList, cropMapping, sheet, evaluator);
            System.out.println("end validating headers - 2");

            iterator.next();
            long lastCertifiedAuditId = planService.getLastCertifiedPlanForProId(auditId, proId).getPlanID();

            List<FarmerListFinal> fFinals = farmerListFinalService
                    .getBeforeCertifiedFarmLIstFinal(proId, auditId);
//            if (fFinals == null){
//
//            }


//            System.out.println("Read audit data start "+fFinals.get(0).getAuditID());
//            System.out.println("Read audit data start "+fFinals.get(0).getProID());
            sendProgressUpdate(15, "Start Reading data");
            FarmerListComparisonDto farmerListComparisonDto = readAuditData(iterator,
                    cropMapping,
                    errorList,
                    evaluator,
                    auditId,
                    proId,
                    lastCertifiedAuditId,
                    fFinals);
            System.out.println("Read audit data done");
            workbook.close();
//            System.out.println("Is errorlist empty : " + errorList.size());
            if (errorList.isEmpty()) {
                try {
                    sendProgressUpdate(45, "Comparing Reading data");
                    farmerLists = makeComparison(farmerListComparisonDto, fFinals, auditId);
                    endTime = System.currentTimeMillis();
                    log.info("task end : " + (endTime - startTime) + "ms");
//                    System.out.println("new user count " + newUser);
                    sendProgressUpdate(75, "Saving data");
                    farmerListService.saveFarmerList(proId, auditId, farmerLists);

                    return ResponseEntity.ok().build();
                } catch (DataIntegrityViolationException e) {
//                    System.out.println("error");
                    errorList.add(ExcelErrorResponse.builder()
                            .error("Data already contained. Project :" + projectName + " Audit : " + auditId).build());
                    return ResponseEntity.badRequest().body(errorList);
                } catch (Exception e) {
//                    System.out.println("Err");
                    errorList.add(ExcelErrorResponse.builder()
                            .error(e.getMessage()).build());
                    return ResponseEntity.badRequest().body(errorList);
                }
            } else {
//                log.error(className + ".errors while reading file " + errorList);
                return ResponseEntity.badRequest().body(errorList);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            errorList.add(ExcelErrorResponse.builder()
                    .error(e.getMessage()).build());
            return ResponseEntity.badRequest().body(errorList);
        } catch (Exception e) {
//            log.error(e.getMessage());
            e.printStackTrace();
            errorList.add(ExcelErrorResponse.builder()
                    .error(e.getMessage()).build());
            return ResponseEntity.badRequest().body(errorList);
        }
//        return ResponseEntity.badRequest().body(errorList);
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
//        boolean isNewFarmer = false;

        if (cropMapping.keySet().size() == 0) {
            errorList.add(ExcelErrorResponse.builder()
                    .error(Errors.REQUIRED_CROP.getName())
                    .build());
        }

        HashMap<String, FarmerListFinal> farmarCodeMappingWithFarmListFinal = new LinkedHashMap<>();
        HashMap<String, FarmerList> farmarCodeMappingWithFarmList = new LinkedHashMap<>();
        HashMap<String, Integer> farmerCodeVsCuid = new LinkedHashMap<>();


        while (iterator.hasNext()) {
//            System.out.println("*********************************new Row***********************");
            Row row = iterator.next();
            if (isRowEmpty(row)) {
//                System.out.println("empty for found "+row.getRowNum());
                continue;
            }
            final Iterator<Cell> cellIterator = row.cellIterator();
            FarmerListMandatoryFieldsDto mandatory_fields = new FarmerListMandatoryFieldsDto();

            FarmerList farmerList = new FarmerList();
            farmerList.setAuditID(auditId);
            farmerList.setProID(proId);

            List<FarmerListCrop> farmerListCropList = new ArrayList<>();
//            System.out.println(row);
//            System.out.println("++++++++++++++++++++++++++++++++");
//
//            System.out.println("reading row "+row.getRowNum());

            while (cellIterator.hasNext()) {

                Cell cell = cellIterator.next();

//                if (cell.getColumnIndex() == 0) {
//                    if (cell.getCellType() == CellType.BLANK) {
//                        break;
//                    }
//                }

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
//                            log.info(className + " cuid : " + cuid);
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format.")
                                    .build());
                        }
                        break;
                    case 1:
//                        System.out.println("reading Unit Number for EU / JAS "+ row.getRowNum());
                        try {
//                            if (row.getRowNum() == 65) {
//                                System.out.println("**************************");
////                                System.out.println(cell);
//                            }
                            farmerList.setUnitNoEUJAS(convertCellValueToStringValue(cell, errorList));
                            mandatory_fields.setUnitNoEUJAS(true);
//                        log.info(className + ".readAuditData : Unit Number for EU / JAS : " + convertCellValueToStringValue(cell));

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
                        String finalPlotCode = convertCellValueToStringValue(row.getCell(7), errorList);
                        try {
//                            if (row.getCell(0) == null || convertCellValueToStringValue(cell, errorList).trim().equals("")) {
////                                System.out.println("empty cuid");
//                                farmerListFinal = fFinals.stream()
//                                        .filter(f -> f.getFarCodeEUJAS().trim().equals(finalFarmerCode))
//                                        .findAny()
//                                        .orElse(null);
////                                System.out.println(farmerListFinal.getFarmerName());
//                                if (farmerListFinal == null) {
////                                    System.out.println("empty cuid-new farmer");
//                                    farmerList.setIsNew(1);
////                                    new user
//                                    if (farmerCodeVsCuid.containsKey(farmerCode)) {
//                                        cuid = farmerCodeVsCuid.get(farmerCode);
//                                    } else {
//                                        cuid = farmerListFinalService.createCuid();
//                                        farmerCodeVsCuid.put(farmerCode, cuid);
//                                    }
//                                    newUser++;
//                                } else {
////                                    System.out.println("empty cuid-existing farmer");
////                                    existing farmer send error message to user enter cuid
//                                    cuid = farmerListFinal.getCufarmerID();
//                                    farmerList.setListid(0);
////                                    errorList.add(ExcelErrorResponse.builder()
////                                            .error("You must enter cuid ")
////                                            .correctValue(String.valueOf(cuid))
////                                            .location("Row : " + (row.getRowNum() + 1))
////                                            .build());
//                                }
//                            } else {
//
////                                System.out.println("contain cuid");
//                                farmerListFinal = fFinals.stream()
//                                        .filter(f -> f.getFarCodeEUJAS().trim().equals(finalFarmerCode))
//                                        .findAny()
//                                        .orElse(null);
//
//                                if (farmerListFinal == null) {
//                                    int finalCuid = cuid;
//                                    farmerListFinal = fFinals.stream()
//                                            .filter( f -> f.getCufarmerID() == finalCuid)
//                                            .findAny().orElse(null);
//                                    if (farmerListFinal != null){
//                                        System.out.println("found invalid farmer code for cuid "+cuid);
//                                        farmerCode = farmerList.getFarCodeEUJAS();
//                                    }else{
//                                        farmerList.setIsNew(1);
//                                        if (farmerCodeVsCuid.containsKey(farmerCode)) {
//                                            cuid = farmerCodeVsCuid.get(farmerCode);
//                                        } else {
//                                            cuid = farmerListFinalService.createCuid();
//                                            farmerCodeVsCuid.put(farmerCode, cuid);
//                                        }
//                                    }
//
////                                    farmerList.setIsNew(1);
//                                } else {
////                                    System.out.println("contain cuid-existing farmer");
//                                    if (cuid != farmerListFinal.getCufarmerID()) {
////                                        System.out.println("contain cuid-existing farmer-wrong cuid");
////                                    cuid not matched send error message to user
////                                        errorList.add(ExcelErrorResponse.builder()
////                                                .error("cuid not matched")
////                                                .correctValue(String.valueOf(farmerListFinal.getCufarmerID()))
////                                                .location("Row : " + (row.getRowNum() + 1))
////                                                .build());
////                                        cuid = farmerListFinal.getCufarmerID();
//
//                                    }
//
//                                    cuid = farmerListFinal.getCufarmerID();
//                                    farmerList.setIsNew(0);
//                                }
//
//                            }

                            farmerListFinal = fFinals.stream()
                                    .filter(f -> f.getFarCodeEUJAS().trim().equals(finalFarmerCode))
                                    .findAny()
                                    .orElse(null);
                            if (farmerListFinal != null){
                                cuid = farmerListFinal.getCufarmerID();
                                farmerList.setIsNew(0);
                            }else{
                                farmerList.setIsNew(1);
                                if (farmerCodeVsCuid.containsKey(farmerCode)) {
                                    cuid = farmerCodeVsCuid.get(farmerCode);
                                } else {
                                    cuid = farmerListFinalService.createCuid();
                                    farmerCodeVsCuid.put(farmerCode, cuid);
                                }
                            }
                        } catch (NullPointerException e) {
                            log.error("null pointer occurred " + e.getMessage());
                            e.printStackTrace();
                            log.error(className + ".readAuditData " + "location : " + cell.getAddress() + ": farCode mapping : " + e.getMessage());

                        }

                        farmerList.setCufarmerID(cuid);
//                        farmerList.setIsNew(isNewFarmer ? 1 : 0);
//                        log.info(className + ".readAuditData : Farmer Code for EU / JAS : " + farmerCode
//                                + " checkUserIsExist : " + isNewFarmer + " cuid : " + cuid);
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
                            readingCropsData(cropMapping, errorList, cuid, plotCode, row, farmerList, farmerListCropList, cell);
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
                                  FarmerList farmerList,
                                  List<FarmerListCrop> farmerListCropList,
                                  Cell cell) {
        int i = 0;
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
//                        System.out.println(crop.getCropName() + " : " + row.getCell(currentCellStart + i).getAddress());
                        switch (i) {
                            case 0:
                                try {
                                    farmerListCrop.setNoOfPlant(convertCellValueToNumberValue(row.getCell(currentCellStart + i), errorList));

                                } catch (NullPointerException e) {
                                    farmerListCrop.setNoOfPlant(0);
                                }
                                break;
                            case 1:
                                try {
                                    farmerListCrop.setEstiYield(convertCellValueToNumberValue(row.getCell(currentCellStart + i), errorList));

                                } catch (NullPointerException e) {
                                    farmerListCrop.setEstiYield(0);
                                }
                                break;
                            case 2:
                                try {
                                    farmerListCrop.setRealYield(convertCellValueToNumberValue(row.getCell(currentCellStart + i), errorList));
                                } catch (NullPointerException e) {
                                    farmerListCrop.setRealYield(0);
                                }
                                break;
                            case 3:
                                try {
                                    farmerListCrop.setNoOfSesons(convertCellValueToNumberValue(row.getCell(currentCellStart + i), errorList));
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
        log.info("start comparison with farmlist with farmlist_final");

        ArrayList<FarmerList> farmerLists = new ArrayList<>();
        System.out.println(farmerListFinals.size() + " init found");

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

        if (!farmerListFinals.isEmpty()) {
            System.out.println(farmerListFinals.size() + " deleted");
            farmerListDeletedService.addDataToFarmListDeleted(farmerListFinals, auditID);
        }

        return farmerLists;
    }

    public String convertFarmDataChangesTo(ArrayList<ChangesDto> list) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Error converting HashMap to JSON";
        }
    }

    public String convertArrayListToJsonCrops(ArrayList<HashMap<String, ChangesDto>> arrayList) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayList<String> list = new ArrayList<>();
            for (HashMap<String, ChangesDto> chnges : arrayList) {
                list.add(convertHashMapToJsonCrops(chnges));
            }
            String jsonArray = objectMapper.writeValueAsString(arrayList);
            return jsonArray;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

    }

    public String convertHashMapToJsonCrops(HashMap<String, ChangesDto> map) {
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

//        System.out.println("got "+aFinal);
        if (entry.getValue().getIsNew() == 1) {
//            log.info("new farmer " + entry.getValue().getFarCodeEUJAS() + " no need to compare");
            return entry.getValue();
        }
        String plot = entry.getKey().split("plot")[1];
        int cuid = entry.getValue().getCufarmerID();
//        FarmerListFinal aFinal = farmerListFinals
//                .stream().filter(s -> s.getCufarmerID() == cuid && s.getPlotCode() == plot)
//                .findFirst()
//                .orElse(null);
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode cChanges = mapper.createArrayNode();
        ArrayNode fChanges = mapper.createArrayNode();

//        HashMap<String, ChangesDto> farmChanges = new LinkedHashMap<>();
        ArrayList<ChangesDto> farmChanges = new ArrayList<>();
        ArrayList<HashMap<String, ChangesDto>> cropChanges = new ArrayList<>();
//        FarmerListFinal farmerListFinal = farmerListFinalHashMap.get(entry.getKey());
        FarmerList farmerList = entry.getValue();
//        log.info("comparing farmer: " + farmerList.getCufarmerID() + " project : " + farmerList.getProID() + " audit : " + farmerList.getAuditID() + " with previous data");

        if (aFinal == null) {
            farmChanges.add(new ChangesDto("new plot", plot, null));
        } else {
            if (!aFinal.getFarmerName().trim().equals(farmerList.getFarmerName())) {
                farmChanges.add(new ChangesDto("Farmer Name", aFinal.getFarmerName(), farmerList.getFarmerName()));
            }
            if (!aFinal.getFarmName().trim().equals(farmerList.getFarmName())) {
                farmChanges.add(new ChangesDto("Name of the Farm", aFinal.getFarmName(), farmerList.getFarmName()));
            }
            if (aFinal.getTotalArea() != farmerList.getTotalArea()) {
                farmChanges.add(new ChangesDto("Total Area", String.valueOf(aFinal.getTotalArea()), String.valueOf(farmerList.getTotalArea())));
            }
            if (!aFinal.getGps().trim().equals(farmerList.getGps())) {
                farmChanges.add(new ChangesDto("GPS", aFinal.getGps(), farmerList.getGps()));
            }
            if (!aFinal.getAddress().trim().equals(farmerList.getAddress())) {
                farmChanges.add(new ChangesDto("Address", aFinal.getAddress(), farmerList.getAddress()));
            }
            if (!aFinal.getCity().trim().equals(farmerList.getCity())) {
                farmChanges.add(new ChangesDto("City", aFinal.getCity(), farmerList.getCity()));
            }
            if (!aFinal.getDateCert().equals(farmerList.getDateCert())) {
                farmChanges.add(new ChangesDto("Application date for certification (yyyy-mm-dd)", String.valueOf(aFinal.getDateCert()), String.valueOf(farmerList.getDateCert())));
            }
            if (aFinal.getAplyRetrospe() != farmerList.getAplyRetrospe()) {
                farmChanges.add(new ChangesDto("Applying for Retrospective consideration (Yes/No)", String.valueOf(aFinal.getAplyRetrospe()), String.valueOf(farmerList.getAplyRetrospe())));
            }
            if (!aFinal.getCertification().trim().equals(farmerList.getCertification())) {
                farmChanges.add(new ChangesDto("Certifications", aFinal.getCertification(), farmerList.getCertification()));
            }
            if (!aFinal.getFertilizer().trim().equals(farmerList.getFertilizer())) {
                farmChanges.add(new ChangesDto("Types of fertilizer, pesticide used", aFinal.getFertilizer(), farmerList.getFertilizer()));
            }
            if (!aFinal.getFerUseDate().equals(farmerList.getFerUseDate())) {
                farmChanges.add(new ChangesDto("Last date of use ", aFinal.getFerUseDate(), farmerList.getFerUseDate()));
            }
            if (!aFinal.getDateConfersion().equals(farmerList.getDateConfersion())) {
                farmChanges.add(new ChangesDto("Starting date of Conversion period (yyyy-mm-dd)", String.valueOf(aFinal.getDateConfersion()), String.valueOf(farmerList.getDateConfersion())));
            }
            if (!aFinal.getDateorganic().equals(farmerList.getDateorganic())) {
                farmChanges.add(new ChangesDto("Starting date of Organic Period (yyyy-mm-dd)", String.valueOf(aFinal.getDateorganic()), String.valueOf(farmerList.getDateorganic())));
            }
            if (!aFinal.getEujas_field().trim().equals(farmerList.getEujas_field())) {
                farmChanges.add(new ChangesDto("Field Status EU/JAS ic1/ic2/ic3/org", aFinal.getEujas_field(), farmerList.getEujas_field()));
            }
            if (!aFinal.getEujas_harvest().trim().equals(farmerList.getEujas_harvest())) {
                farmChanges.add(new ChangesDto("Harvest status EU/JAS conv/ic/org", aFinal.getEujas_harvest(), farmerList.getEujas_harvest()));
            }
            if (!aFinal.getUsda_field().trim().equals(farmerList.getUsda_field())) {
                farmChanges.add(new ChangesDto("Field status NOP ic1/ic2/ic3/org", aFinal.getUsda_field(), farmerList.getUsda_field()));
            }
            if (!aFinal.getUsda_harvest().trim().equals(farmerList.getUsda_harvest())) {
                farmChanges.add(new ChangesDto("Harvest status NOP conv/org", aFinal.getUsda_harvest(), farmerList.getUsda_harvest()));
            }

            List<FarmerListCrop> farmerListCrops = farmerList.getFarmerListCropList();
            List<FarmerListCropFinal> farmerListCropFinals = farmerListCropFinalService
                    .findFarmerListCropFinalsForFarmerFarmerListFinal(aFinal);

            cropChanges = compareCropsData(farmerListCropFinals,
                    farmerListCrops);

            String cropChangesStr = convertArrayListToJsonCrops(cropChanges);
//            System.out.println(cropChangesStr);
            farmerList.setChngCropdata(cropChangesStr);
//            farmerListFinals.remove(aFinal);
        }
        if (cropChanges.size() > 0 || farmChanges.size() > 0) {
            farmerList.setIsChange(1);
        }

        farmerList.setChngFarmdata(convertFarmDataChangesTo(farmChanges));
//        System.out.println(convertHashMapToJson(farmChanges));
        return farmerList;

    }

    private ArrayList<HashMap<String, ChangesDto>> compareCropsData(List<FarmerListCropFinal> farmerListCrops_final,
                                                                    List<FarmerListCrop> farmerListCrops) {

        ArrayList<Integer> checkedCrops = new ArrayList<>();
        ArrayList<HashMap<String, ChangesDto>> arrayList = new ArrayList<>();
//        System.out.println(farmerListCrops_final);
//        System.out.println(farmerListCrops);
        try {
            for (FarmerListCropFinal crop_final : farmerListCrops_final) {
                checkedCrops.add(crop_final.getCropID());
                HashMap<String, ChangesDto> changesMap = new LinkedHashMap<>();

                FarmerListCrop prevFarmerlistCrop = farmerListCrops.stream()
                        .filter(s -> s.getCropID() == crop_final.getCropID()
                                && s.getPlotCode().trim().equals(crop_final.getPlotCode()))
                        .findFirst()
                        .orElse(null);

                if (prevFarmerlistCrop == null) {
                    changesMap.put("cropChange", new ChangesDto(crop_final.getCropID(), "Crop deleted", crop_final.getCropID(), 0));
//                changesMap.put("cropChange", new ChangesDto(crop_final.getCropID(),"Crop deleted",crop_final.getCropID(), farmerListCrop));
                    arrayList.add(changesMap);
                } else {

                    if (crop_final.getNoOfPlant() != prevFarmerlistCrop.getNoOfPlant()) {
                        changesMap.put("cropChange", new ChangesDto(crop_final.getCropID(), "Number of Plants", crop_final.getNoOfPlant(), prevFarmerlistCrop.getNoOfPlant()));
                        arrayList.add(changesMap);
                    }
                    if (crop_final.getEstiYield() != prevFarmerlistCrop.getEstiYield()) {
                        changesMap.put("cropChange", new ChangesDto(crop_final.getCropID(), "Estimated Yield (Kg) / year", crop_final.getNoOfPlant(), prevFarmerlistCrop.getNoOfPlant()));
                        arrayList.add(changesMap);
                    }
                    if (crop_final.getRealYield() != prevFarmerlistCrop.getRealYield()) {
                        changesMap.put("cropChange", new ChangesDto(crop_final.getCropID(), "Realistic Yield in Last year (kg)", crop_final.getNoOfPlant(), prevFarmerlistCrop.getNoOfPlant()));
                        arrayList.add(changesMap);
                    }
                    if (crop_final.getNoOfSesons() != prevFarmerlistCrop.getNoOfSesons()) {
                        changesMap.put("cropChange", new ChangesDto(crop_final.getCropID(), "Realistic Yield in Last year (kg)", crop_final.getNoOfPlant(), prevFarmerlistCrop.getNoOfPlant()));
                        arrayList.add(changesMap);
                    }
                }
            }

            farmerListCrops.stream()
                    .filter(crop -> !checkedCrops.contains(crop.getCropID()))
                    .map(crop -> {
                        HashMap<String, ChangesDto> changesMap = new LinkedHashMap<>();
                        changesMap.put("cropChange", new ChangesDto(crop.getCropID(), "new crop added", 0, crop.getId()));
                        return arrayList.add(changesMap);

//                    return changesCrop.put(crop.getCropID(), changesMap);

                    }).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error occured while comparing crops " + e.getMessage());
        }

        return arrayList;
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
//        log.info("start validating audit headers");
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
//                continue;
            }
        }
//        log.info("end validating audit headers");
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

//        log.info("start finding table start point at " + rowNumber);
        System.out.println(rowIterator.next());
//        findingTableStartOnRows:
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
//            log.info("Iterating through rows until find table");
//            log.info("actual row number " + row.getRowNum() + "native row number : " + rowNumber);
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
//                log.info("set new row number : " + rowNumber);
            }
            final Iterator<Cell> cellIterator = row.cellIterator();
            int matchingTableHeaders = 0;
//            log.info("checking is table start");
//            lookingCellLoop:
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                try {
                    if (cell.getStringCellValue().trim().equals(excelFileProperties.getHeader1().get(cell.getColumnIndex()))) {
                        matchingTableHeaders++;
//                        log.info("matching headers " + matchingTableHeaders);
                    }
                    if (matchingTableHeaders > (excelFileProperties.getHeader1().size() / 2)) {
//                        log.info("end finding table start " + row.getRowNum());
//                        break findingTableStartOnRows;
                        return row;
                    }
                } catch (IllegalStateException ignored) {

                }
            }
//            log.info("table not started yet");
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

//            log.info("not table starting " + rowNumber);
            rowNumber++;
        }

        return null;
    }

//    private void validateTableHeaders(Iterator<Row> rowIterator, Row row, List<ExcelErrorResponse> errorList, int rowNumber) {
////        System.out.println("Starting row - ");
////        System.out.println(row);
//
//        Row row1 = rowIterator.next();
//
//    }


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
//        log.info("validating 2nd level headers " + row.getRowNum());
        final Iterator<Cell> cellIterator = row.iterator();
        int index = 0;

        while (cellIterator.hasNext()) {
//            log.info("iterating in header 2");
            Cell cell = cellIterator.next();
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

        validateCropsHeaders(cellIterator, errorList, cropMapping, sheet, index, evaluator);
    }

    private void validateCropsHeaders(Iterator<Cell> cellIterator,
                                      List<ExcelErrorResponse> errorList,
                                      Map<Integer, Crop> cropMapping,
                                      XSSFSheet sheet,
                                      int lastColNumber,
                                      FormulaEvaluator evaluator) {
//        System.out.println("ready validating crops " + lastColNumber);
        ArrayList<Cell> cells = new ArrayList<>();
        while (cellIterator.hasNext()) {
            cells.add(cellIterator.next());
        }

        for (int i = 0; i < cells.size(); i += excelFileProperties.getHeader3().size()) {
            Cell cell = cells.get(i);
            if (cell.getCellType() == CellType.FORMULA) {
                cell = isFormulaProceed(cell, evaluator);
            }
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
                        .errorValue(cell.getStringCellValue().trim())
                        .build());
            } else {

//                System.out.println(cell.getColumnIndex());
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
//        log.info("validateCrops : checking crop on cell " + cell.getAddress() + " col " + cell.getColumnIndex());
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
//                System.out.println(cropMapping);
//                log.info("validateCrops : crop " + crop.getCropName() + " : passed");
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
            if (!stringCellValue.equals("") && stringCellValue.startsWith("\"=")) {
                System.out.println(stringCellValue);
                return false;
            }
        } catch (Exception ignore) {

        }

        return true;
    }

    private double convertCellValueToNumberValue(Cell cell, List<ExcelErrorResponse> errorList) {
        try {
            CellType cellType = cell.getCellType();
//        System.out.println(cellType);

            double val;
            switch (cellType) {
                case STRING:
//                System.out.println("String found");
                    val = Double.parseDouble(cell.getStringCellValue().trim());
                    break;
                case NUMERIC:
//                System.out.println("NUMERIC found");
                    val = cell.getNumericCellValue();
                    break;
                case FORMULA:
//                System.out.println("FORMULA found");
                    val = cell.getNumericCellValue();
                    break;
                default:
                    val = 0d;
                    break;

            }
            return val;
//        log.info(className + ".convertCellValueToNumberValue : returning val " + val);
        } catch (Exception e) {
//            e.printStackTrace();
//            errorList.add(ExcelErrorResponse
//                    .builder()
//                    .error("Invalid type used")
//                    .location(cell.getAddress() + "")
//                    .build());
            return 0;
        }


    }

    private void sendProgressUpdate(int read, String message) {

        UpoadProgressDto upoadProgressDto = new UpoadProgressDto();
        upoadProgressDto.setDone(false);
        upoadProgressDto.setBytesRead(read);
        upoadProgressDto.setContentLength(100);

        messagingTemplate.convertAndSend("/topic/upload-progress", upoadProgressDto);

    }

}
