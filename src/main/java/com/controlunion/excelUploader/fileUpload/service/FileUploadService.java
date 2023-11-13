//package com.controlunion.excelUploader.fileUpload.service;
//
//import com.controlunion.excelUploader.fileUpload.dto.ProgressMessage;
//import com.controlunion.excelUploader.service.FileServiceNewTest;
//import lombok.RequiredArgsConstructor;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//@Service
//@RequiredArgsConstructor
//public class FileUploadService {
//    private final SimpMessagingTemplate messagingTemplate;
//    private final FileServiceNewTest fileServiceNewTest;
//
////    public FileUploadService(SimpMessagingTemplate messagingTemplate) {
////        this.messagingTemplate = messagingTemplate;
////    }
//
//    public void processFileUpload(MultipartFile file,
//                                  int projectId,
//                                  int auditId,
//                                  String projectName,
//                                  int proId) {
//
//
//
////        sendProgressUpdate(10, 100, true);
////
////        sendProgressUpdate(30, 100, true);
////        // Your file upload logic here
////        sendProgressUpdate(40, 100, true);
////        // Notify the frontend about the progress
////        sendProgressUpdate(60, 100, true);
////        sendProgressUpdate(80, 100, true);
////        sendProgressUpdate(100, 100, true);
//
//    }
//
//    private void sendProgressUpdate(long bytesRead, long contentLength, boolean done) {
//        // Send a progress update to the "/topic/upload-progress" WebSocket topic
//        messagingTemplate.convertAndSend("/topic/upload-progress", new ProgressMessage(bytesRead, contentLength, done));
//    }
//}
