package com.controlunion.excelUploader.fileUpload.controller;

// FileUploadController.java

//import com.controlunion.excelUploader.fileUpload.ProgressMultipartFileWrapper;
//import com.controlunion.excelUploader.fileUpload.service.FileUploadService;
import com.controlunion.excelUploader.fileUpload.dto.UpoadProgressDto;
import com.controlunion.excelUploader.fileUpload.dto.WebSocketResponse;
import com.controlunion.excelUploader.service.FileServiceNewTest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload")
public class FileUploadController {

    private final SimpMessagingTemplate messagingTemplate;
    private final FileServiceNewTest fileServiceNewTest;

    @PostMapping
    public ResponseEntity<String> handleFileUpload(@RequestPart("file") MultipartFile file,
                                                   @RequestParam("project_id") String projectId,
                                                   @RequestParam("project_name") String projectName,
                                                   @RequestParam("audit") String auditId,
                                                   @RequestParam("proId") String proId) {

//        ProgressMultipartFileWrapper fileWrapper = new ProgressMultipartFileWrapper(file, (bytesRead, contentLength, done) ->
//                System.out.println()
//        );

        fileServiceNewTest.uploadExcelFile(file,
                Integer.parseInt(projectId),
                Integer.parseInt(auditId),
                projectName, Integer.parseInt(proId));
        UpoadProgressDto response = new UpoadProgressDto();
        response.setContentLength(100);
        response.setBytesRead(100);
        response.setDone(true);
//        response.setMessage("File uploaded: " + file.getOriginalFilename());
        messagingTemplate.convertAndSend("/topic/upload-progress", response);

//        return fileServiceNewTest.uploadExcelFile(file,
//                Integer.parseInt(projectId),
//                Integer.parseInt(auditId),
//                projectName, Integer.parseInt(proId));

        return ResponseEntity.ok("File uploaded successfully!");
    }
}
