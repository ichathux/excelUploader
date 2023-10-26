package com.controlunion.excelUploader.controller;

import com.controlunion.excelUploader.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("api/file/v1/")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "upload", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity excelUpload(@RequestPart("file") MultipartFile file){
        return fileService.uploadExcelFile(file);
    }

    @GetMapping("findAll")
    public ResponseEntity getAllCrops(){
        fileService.getAllCrops();
        return ResponseEntity.ok().build();
    }
}
