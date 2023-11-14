package com.controlunion.excelUploader.controller;

import com.controlunion.excelUploader.service.CropService;
//import com.controlunion.excelUploader.service.FileServiceNew;
import com.controlunion.excelUploader.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("api/file/v1/")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final CropService cropService;

    /**
     *
     * @param file (MultipartFile file )
     * @return
     */

    @PostMapping(value = "upload", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity excelUpload(@RequestPart("file") MultipartFile file,
                                      @RequestParam("project_id") String projectId,
                                      @RequestParam("project_name") String projectName,
                                      @RequestParam("audit") String auditId,
                                      @RequestParam("proId") String proId){
        System.out.println(file.getOriginalFilename());

        System.out.println(auditId);

        System.out.println(projectId);
        System.out.println(projectName);
        System.out.println(proId);

        return fileService.uploadExcelFile(file,
                Integer.parseInt(projectId),
                Integer.parseInt(auditId),
                projectName,
                Integer.parseInt(proId));
    }

    @GetMapping("findAll")
    public ResponseEntity getAllCrops(){
        cropService.getAllCrops();
        return ResponseEntity.ok().build();
    }
}
