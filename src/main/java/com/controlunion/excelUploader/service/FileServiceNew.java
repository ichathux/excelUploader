package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.config.ExcelFilePropertiesConfig;
import com.controlunion.excelUploader.dto.ChangesDto;
import com.controlunion.excelUploader.dto.ExcelErrorResponse;
import com.controlunion.excelUploader.dto.FarmerListComparisonDto;
import com.controlunion.excelUploader.dto.FarmerListMandatoryFieldsDto;
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
public class FileServiceNew {

    private final String className = this.getClass().getName();
    private final ExcelFilePropertiesConfig excelFileProperties;
    private final CropService cropService;
    private final FarmerListService farmerListService;
    private final FarmerListFinalService farmerListFinalService;
    private final PlanService planService;
    private final FarmerListCropFinalService farmerListCropFinalService;
//    private final FarmListDeletedService farmListDeletedService;
//    private final FarmListCropDeletedService farmListCropDeletedService;

    public ResponseEntity uploadExcelFile(MultipartFile file,
                                          int projectId,
                                          int auditId,
                                          String projectName,
                                          int proId) {

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
            FarmerListComparisonDto farmerListComparisonDto = readAuditData(iterator, cropMapping, errorList, evaluator, projectId, auditId, proId);
            farmerLists = makeComparison(farmerListComparisonDto);
            if (errorList.isEmpty()) {
                farmerListService.saveFarmerList(farmerLists);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }

        if (!errorList.isEmpty()) {
            log.error(className + ".errors while reading file ");
            return ResponseEntity.badRequest().body(errorList);
        }

        return ResponseEntity.ok().body("done");
    }

    private FarmerListComparisonDto readAuditData(Iterator<Row> iterator,
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

        HashMap<String, FarmerListFinal> farmarCodeMappingWithFarmListFinal = new LinkedHashMap<>();
        HashMap<String, FarmerList> farmarCodeMappingWithFarmList = new LinkedHashMap<>();
        HashMap<String, Integer> farmerCodeVsCuid = new LinkedHashMap<>();

        long lastCertifiedAuditId = planService.getLastCertifiedPlanForProId(proId).getPlanID();

        while (iterator.hasNext()) {
            System.out.println("*********************************new Row***********************");
            Row row = iterator.next();
            if (!isRowNotEmpty(row)) {
                continue;
            }
            final Iterator<Cell> cellIterator = row.cellIterator();
            FarmerListMandatoryFieldsDto mandatory_fields = new FarmerListMandatoryFieldsDto();

            FarmerList farmerList = new FarmerList();
            farmerList.setProID(proId);
            farmerList.setAuditID(auditId);

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
                        cell = isFormulaProceed(cell, evaluator, errorList, row);
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
                            log.info(className + " cuid : " + cuid);
                        } catch (IllegalStateException e) {
                            errorList.add(ExcelErrorResponse.builder()
                                    .location("Cell:" + cell.getAddress())
                                    .error("contains illegal format.")
                                    .build());
                        }
                        break;
                    case 1:
                        farmerList.setUnitNoEUJAS(convertCellValueToStringValue(cell));
                        mandatory_fields.setUnitNoEUJAS(true);
                        log.info(className + ".readAuditData : Unit Number for EU / JAS : " + convertCellValueToStringValue(cell));

                        break;
                    case 2:
                        farmerCode = convertCellValueToStringValue(cell); //getting cell value form farmerCode
                        farmerList.setFarCodeEUJAS(farmerCode);     //sett farmerCode for farmerList object
                        mandatory_fields.setFarCodeEUJAS(true);     //set mandatory field contain
                        List<FarmerListFinal> farmerListFinal;
                        try {
                            String finalPlotCode = convertCellValueToStringValue(row.getCell(7));
                            log.info("getting plot code early " + finalPlotCode);
                            farmerListFinal = farmerListFinalService.getFarmerByCodeAndProIdAndAuditId(farmerCode, proId, (int) lastCertifiedAuditId);
                            FarmerListFinal ff = farmerListFinal
                                    .stream().filter(f -> f.getPlotCode().equals(finalPlotCode))
                                    .findFirst()
                                    .orElse(null);

                            if (row.getCell(0) == null || convertCellValueToStringValue(cell).equals("")) {
                                log.info("cuid not contain in excel sheet");
                                if (farmerCodeVsCuid.containsKey(farmerCode)) {
                                    cuid = farmerCodeVsCuid.get(farmerCode);
                                    log.info("cuid already created for : " + farmerCode + " : cuid " + cuid);
                                } else {
                                    if (farmerListFinal == null) {
                                        isNewFarmer = true;
                                        cuid = farmerListFinalService.createCuid();
                                        farmerCodeVsCuid.put(farmerCode, cuid);
                                        log.info("cuid created for : " + farmerCode + " : cuid " + cuid);
                                    } else {
                                        cuid = farmerListFinal.get(0).getCufarmerID();
                                        log.info("founb cuid for : " + farmerCode + " : cuid " + cuid);
                                    }

                                    farmerCodeVsCuid.put(farmerCode, cuid);
                                }
                                if (!isNewFarmer) {
                                    errorList.add(ExcelErrorResponse.builder()
                                            .location("Row " + (row.getRowNum() + 1))
                                            .error("cuid must be insert")
                                            .correctValue(cuid + "").build());
                                    log.error(className + ".readAuditData: isNewFarmer - " + isNewFarmer + " : need to insert - " + cuid);
                                }
                            } else {
                                if (farmerCodeVsCuid.containsKey(farmerCode)) {
                                    if (cuid != farmerCodeVsCuid.get(farmerCode)) {
                                        errorList.add(ExcelErrorResponse.builder()
                                                .error("cuid not matched")
                                                .location("Cell " + row.getCell(0).getAddress())
                                                .errorValue(cuid + "")
                                                .build());
                                        log.error(className + ".readAuditData: isNewFarmer - " + isNewFarmer + " : cuid used for another farmer - " + cuid);
                                    } else {
                                        System.out.println(ff.getFarmerName());
                                        if (ff != null) {
                                            log.info(className + ".readAuditData: adding farmerlistfinal to map: " + farmerCode + " plot " + plotCode);
                                            farmarCodeMappingWithFarmListFinal.put(farmerCode + " plot " + finalPlotCode, ff);
                                        }
                                    }
                                } else {
                                    if (farmerListFinal == null) {
                                        errorList.add(ExcelErrorResponse.builder()
                                                .location("Row " + (row.getRowNum() + 1))
                                                .error("invalid cuid. selected farmer not contain in the database")
                                                .build());
                                        log.error(className + ".readAuditData: isNewFarmer - " + isNewFarmer + " : need to insert - " + cuid);
                                    } else {
                                        if (cuid != farmerListFinal.get(0).getCufarmerID()) {
                                            errorList.add(ExcelErrorResponse.builder()
                                                    .error("cuid not matched")
                                                    .location("Cell " + row.getCell(0).getAddress())
                                                    .errorValue(cuid + "")
                                                    .build());
                                            log.error(className + ".readAuditData: isNewFarmer - " + isNewFarmer + " : cuid used for another farmer - " + cuid);
                                        } else {
                                            cuid = farmerListFinal.get(0).getCufarmerID();
                                            farmerCodeVsCuid.put(farmerCode, cuid);
                                            System.out.println(ff.getFarmerName());
                                            if (ff != null) {
                                                log.info(className + "..readAuditData: adding farmerlistfinal to map: " + farmerCode + " plot " + plotCode);
                                                farmarCodeMappingWithFarmListFinal.put(farmerCode + " plot " + finalPlotCode, ff);
                                            }
                                        }

                                    }
                                }

                            }
                            log.info("created farmerlist final list");
                        } catch (NullPointerException e) {
                            log.error("null pointer occurred " + e.getMessage());
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
                        farmerList.setGps(convertCellValueToStringValue(cell));
                        log.info(className + ".readAuditData : GPS : " + convertCellValueToStringValue(cell));

                        break;
                    case 10:
                        farmerList.setAddress(convertCellValueToStringValue(cell));
                        log.info(className + ".readAuditData : Address/Village : " + convertCellValueToStringValue(cell));

                        break;
                    case 11:
                        try {
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
                        farmerList.setAplyRetrospe(convertCellValueToStringValue(cell).trim().equalsIgnoreCase("yes") ? 1 : 0);
                        log.info(className + ".readAuditData : Applying for Retrospective consideration (Yes/No) : " + convertCellValueToStringValue(cell));

                        break;
                    case 14:
                        farmerList.setCertification(convertCellValueToStringValue(cell).replaceAll(",","/"));
                        log.info(className + ".readAuditData : Certifications : " + certification);

                        break;
                    case 15:
                        farmerList.setFertilizer(convertCellValueToStringValue(cell));
                        log.info(className + ".readAuditData : Types of fertilizer, pesticide used : " + convertCellValueToStringValue(cell));

                        break;
                    case 16:
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
                        farmerList.setEujas_field(convertCellValueToStringValue(cell));
                        mandatory_fields.setEujasField(true);
                        log.info(className + "readAuditData : Field Status EU/JAS ic1/ic2/ic3/org : " + convertCellValueToStringValue(cell));

                        break;
                    case 20:
                        farmerList.setEujas_harvest(convertCellValueToStringValue(cell));
                        mandatory_fields.setEujasHarvest(true);
                        log.info(className + "readAuditData : Harvest status EU/JAS conv/ic/org : " + convertCellValueToStringValue(cell));

                        break;
                    case 21:
                        farmerList.setUsda_field(convertCellValueToStringValue(cell));
                        log.info(className + "readAuditData : Field status NOP ic1/ic2/ic3/org : " + convertCellValueToStringValue(cell));
                        break;
                    case 22:
                        farmerList.setUsda_harvest(convertCellValueToStringValue(cell));
                        log.info(className + "readAuditData : Harvest status NOP conv/org : " + convertCellValueToStringValue(cell));
                        break;
//                    reading crop section
                    default:
                        readingCropDatas(cropMapping, errorList, cuid, plotCode, row, farmerList, farmerListCropList, cell);

                        break;
                }
            }
            log.info(className + "readAuditData : found " + farmerListCropList.size() + " for farmer crop list");

            checkMandatoryFieldsExists(mandatory_fields, errorList, (row.getRowNum() + 1));
            farmerList.setUser("isuru");
            farmerList.setGps("");
            farmerList.setChngCropdata("");
            farmerList.setChngFarmdata("");
            farmerList.setSysTimeStamp(new Date(System.currentTimeMillis()));
            farmerList.setProID(proId);
            farmerList.setAuditID(auditId);
            farmerList.setInspected(0);
            farmerList.setFarmerListCropList(farmerListCropList);
            log.info("adding farmlist to hashmap " + farmerCode);
            farmarCodeMappingWithFarmList.put(farmerCode + " plot " + plotCode, farmerList);

        }

        return new FarmerListComparisonDto(farmarCodeMappingWithFarmList, farmarCodeMappingWithFarmListFinal, farmerCodeVsCuid);

    }

    private void readingCropDatas(Map<Integer, Crop> cropMapping, List<ExcelErrorResponse> errorList, int cuid, String plotCode, Row row, FarmerList farmerList, List<FarmerListCrop> farmerListCropList, Cell cell) {
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
    }

    private ArrayList<FarmerList> makeComparison(FarmerListComparisonDto dto) {
        log.info("start comparison with farmlist with farmlist_final");
        System.out.println("map 1");
        System.out.println(dto.getMap1().keySet());
        System.out.println("map 2");
        System.out.println(dto.getMap2().keySet());

        ArrayList<FarmerList> farmerLists = new ArrayList<>();
        for (Map.Entry<String, FarmerList> entry : dto.getMap1().entrySet()) {
            farmerLists.add(compareFarmData(entry, dto.getMap2()));
        }
        System.out.println(dto.getMap2().size());
        System.out.println(dto.getCuidVsFarmarCode());
        return farmerLists;
    }

    public String convertHashMapToJson(HashMap<String, ChangesDto> map) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Error converting HashMap to JSON";
        }
    }

    public String convertHashMapToJsonCrops(HashMap<Integer, HashMap<String, ChangesDto>> map) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Error converting HashMap to JSON";
        }
    }

    private FarmerList compareFarmData(Map.Entry<String, FarmerList> entry,
                                       HashMap<String, FarmerListFinal> farmerListFinalHashMap) {

        if (entry.getValue().getIsNew() == 1) {
            log.info("new farmer " + entry.getValue().getFarCodeEUJAS() + " no need to compare");
            return entry.getValue();
        }
        String plot = entry.getKey().split("plot")[1];

        HashMap<String, ChangesDto> farmChanges = new LinkedHashMap<>();
        HashMap<Integer, HashMap<String, ChangesDto>> cropChanges = new LinkedHashMap<>();

        FarmerListFinal farmerListFinal = farmerListFinalHashMap.get(entry.getKey());
        FarmerList farmerList = entry.getValue();
        log.info("comparing farmer: " + farmerList.getCufarmerID() + " project : " + farmerList.getProID() + " audit : " + farmerList.getAuditID() + " with previous data");
        ArrayList<String> checked = new ArrayList<>();

//        checked.add(entry.getKey());

        if (!farmerListFinalHashMap.containsKey(entry.getKey())) {
            farmChanges.put("new plot", new ChangesDto(plot));

        } else {
            if (!farmerListFinal.getFarmerName().equalsIgnoreCase(farmerList.getFarmerName())) {
                farmChanges.put("Farmer Name", new ChangesDto(farmerListFinal.getFarmerName(), farmerList.getFarmerName()));
            }
            if (!farmerListFinal.getFarmName().equalsIgnoreCase(farmerList.getFarmName())) {
                farmChanges.put("Name of the Farm", new ChangesDto(farmerListFinal.getFarmName(), farmerList.getFarmName()));
            }
            if (farmerListFinal.getTotalArea() != farmerList.getTotalArea()) {
                farmChanges.put("Total Area (Ha)", new ChangesDto(String.valueOf(farmerListFinal.getTotalArea()), String.valueOf(farmerList.getTotalArea())));
            }
            if (!farmerListFinal.getGps().equalsIgnoreCase(farmerList.getGps())) {
                farmChanges.put("GPS", new ChangesDto(farmerListFinal.getGps(), farmerList.getGps()));
            }
            if (!farmerListFinal.getAddress().equalsIgnoreCase(farmerList.getAddress())) {
                farmChanges.put("Address/Village", new ChangesDto(farmerListFinal.getAddress(), farmerList.getAddress()));
            }
            if (!farmerListFinal.getCity().equalsIgnoreCase(farmerList.getCity())) {
                farmChanges.put("City", new ChangesDto(farmerListFinal.getCity(), farmerList.getCity()));
            }
            if (!farmerListFinal.getDateCert().equals(farmerList.getDateCert())) {
                farmChanges.put("Application date for certification (yyyy-mm-dd)", new ChangesDto(String.valueOf(farmerListFinal.getDateCert()), String.valueOf(farmerList.getDateCert())));
            }
            if (farmerListFinal.getAplyRetrospe() != farmerList.getAplyRetrospe()) {
                farmChanges.put("Applying for Retrospective consideration (Yes/No)", new ChangesDto(String.valueOf(farmerListFinal.getAplyRetrospe()), String.valueOf(farmerList.getAplyRetrospe())));
            }
            if (!farmerListFinal.getCertification().equalsIgnoreCase(farmerList.getCertification())) {
                farmChanges.put("Certifications", new ChangesDto(farmerListFinal.getCertification(), farmerList.getCertification()));
            }
            if (!farmerListFinal.getFertilizer().equalsIgnoreCase(farmerList.getFertilizer())) {
                farmChanges.put("Types of fertilizer, pesticide used", new ChangesDto(farmerListFinal.getFertilizer(), farmerList.getFertilizer()));
            }
            if (!farmerListFinal.getFerUseDate().equalsIgnoreCase(farmerList.getFerUseDate())) {
                farmChanges.put("Last date of use ", new ChangesDto(farmerListFinal.getFerUseDate(), farmerList.getFerUseDate()));
            }
            if (!farmerListFinal.getDateConfersion().equals(farmerList.getDateConfersion())) {
                farmChanges.put("Starting date of Conversion period (yyyy-mm-dd)", new ChangesDto(String.valueOf(farmerListFinal.getDateConfersion()), String.valueOf(farmerList.getDateConfersion())));
            }
            if (!farmerListFinal.getDateorganic().equals(farmerList.getDateorganic())) {
                farmChanges.put("Starting date of Organic Period (yyyy-mm-dd)", new ChangesDto(String.valueOf(farmerListFinal.getDateorganic()), String.valueOf(farmerList.getDateorganic())));
            }
            if (!farmerListFinal.getEujas_field().equalsIgnoreCase(farmerList.getEujas_field())) {
                farmChanges.put("Field Status EU/JAS ic1/ic2/ic3/org", new ChangesDto(farmerListFinal.getEujas_field(), farmerList.getEujas_field()));
            }
            if (!farmerListFinal.getEujas_harvest().equalsIgnoreCase(farmerList.getEujas_harvest())) {
                farmChanges.put("Harvest status EU/JAS conv/ic/org", new ChangesDto(farmerListFinal.getEujas_harvest(), farmerList.getEujas_harvest()));
            }
            if (!farmerListFinal.getUsda_field().equalsIgnoreCase(farmerList.getUsda_field())) {
                farmChanges.put("Field status NOP ic1/ic2/ic3/org", new ChangesDto(farmerListFinal.getUsda_field(), farmerList.getUsda_field()));
            }
            if (!farmerListFinal.getUsda_harvest().equalsIgnoreCase(farmerList.getUsda_harvest())) {
                farmChanges.put("Harvest status NOP conv/org", new ChangesDto(farmerListFinal.getUsda_harvest(), farmerList.getUsda_harvest()));
            }

            List<FarmerListCrop> farmerListCrops = farmerList.getFarmerListCropList();
            List<FarmerListCropFinal> farmerListCropFinals = farmerListCropFinalService
                    .findFarmerListCropFinalsForFarmerFarmerListFinal(farmerListFinal);

            cropChanges = compareCropsData(farmerListCropFinals,
                    farmerListCrops);

            farmerList.setChngCropdata(convertHashMapToJsonCrops(cropChanges));
//            System.out.println("After converting json - crop");
            System.out.println(convertHashMapToJsonCrops(cropChanges));
            farmerListFinalHashMap.remove(entry.getKey());
        }

        if (cropChanges.size() > 0 || farmChanges.size() > 0){
            farmerList.setIsChange(1);
        }
        if (farmerListFinalHashMap.size() > 0){
//            farmListDeletedService.addDataToFarmListDeleted(farmerListFinalHashMap.values());
        }
        farmerList.setChngFarmdata(convertHashMapToJson(farmChanges));
//        System.out.println("After converting json - farm");
        System.out.println(convertHashMapToJson(farmChanges));


        return farmerList;

    }

    private HashMap<Integer, HashMap<String, ChangesDto>> compareCropsData(List<FarmerListCropFinal> farmerListCrops_final,
                                                         List<FarmerListCrop> farmerListCrops) {



        HashMap<Integer, HashMap<String, ChangesDto>> changesCrop = new HashMap<>();
        ArrayList<Integer> checkedCrops = new ArrayList<>();

        for (FarmerListCropFinal crop_final : farmerListCrops_final) {
            checkedCrops.add(crop_final.getCropID());
            HashMap<String, ChangesDto> changesMap = new LinkedHashMap<>();
            FarmerListCrop crop = farmerListCrops.parallelStream()
                    .filter(s -> s.getCropID() == crop_final.getCropID())
                    .findFirst()
                    .orElse(null);

            if (crop == null) {
                changesMap.put("Crop deleted", new ChangesDto(crop_final.getCropID()));
                changesCrop.put(crop_final.getCropID(), changesMap);
//                farmListCropDeletedService.addDeletedCropsToDB(crop_final);

            } else {

                if (crop_final.getNoOfPlant() != crop.getNoOfPlant()) {
                    changesMap.put("Number of Plants", new ChangesDto(crop_final.getNoOfPlant(), crop.getNoOfPlant()));
                    changesCrop.put(crop_final.getCropID(), changesMap);
                }
                if (crop_final.getEstiYield() != crop.getEstiYield()) {
                    changesMap.put("Estimated Yield (Kg) / year", new ChangesDto(crop_final.getNoOfPlant(), crop.getNoOfPlant()));
                    changesCrop.put(crop_final.getCropID(), changesMap);
                }
                if (crop_final.getRealYield() != crop.getRealYield()) {
                    changesMap.put("Realistic Yield in Last year (kg)", new ChangesDto(crop_final.getNoOfPlant(), crop.getNoOfPlant()));
                    changesCrop.put(crop_final.getCropID(), changesMap);
                }
                if (crop_final.getNoOfSesons() != crop.getNoOfSesons()) {
                    changesMap.put("Number of seasons (only for annual crops)", new ChangesDto(crop_final.getNoOfPlant(), crop.getNoOfPlant()));
                    changesCrop.put(crop_final.getCropID(), changesMap);
                }
            }
        }
        farmerListCrops.parallelStream()
                .filter(crop -> !checkedCrops.contains(crop.getCropID()))
                .map(crop -> {
                    HashMap<String, ChangesDto> changesMap = new LinkedHashMap<>();
                    changesMap.put("new crop added", new ChangesDto(crop.getCropID()));
                    return changesCrop.put(crop.getCropID(),changesMap);

                })
                .collect(Collectors.toList());
        return changesCrop;
    }


    private Cell isFormulaProceed(Cell cell,
                                  FormulaEvaluator evaluator,
                                  List<ExcelErrorResponse> errorList,
                                  Row row) {
        try {
            log.info("**********Formular*********" + cell.getAddress());
            CellValue cellValue = evaluator.evaluate(cell);
            StringBuilder result = new StringBuilder(cellValue.formatAsString());
            log.info("evaluated result " + result);

            if (cellValue.getCellType() == CellType.NUMERIC) {
                log.info("setting formula evaluated value " + result + " parent cell type : ");
                cell.setCellValue(result.toString());

            } else {
                cell.setCellValue(result.substring(1, result.length() - 1));
            }

            log.info(className + ".isFormulaProceed : getting result : result of formula : " + cell.getStringCellValue());
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
//        System.out.println("Starting row - ");
//        System.out.println(row);

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
                System.out.println(cell.getStringCellValue().trim());
                return cell.getStringCellValue().trim();
            case NUMERIC:
                System.out.println(cell.getNumericCellValue() + "".trim());
                return cell.getNumericCellValue() + "".trim();
            case FORMULA:
                return cell.getStringCellValue();
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
                System.out.println("String found");
                val = Double.parseDouble(cell.getStringCellValue());
                break;
            case NUMERIC:
                System.out.println("NUMERIC found");
                val = cell.getNumericCellValue();
                break;
            case FORMULA:
                System.out.println("FORMULA found");
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
