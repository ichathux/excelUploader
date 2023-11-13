package com.controlunion.excelUploader.fileUpload.controller;

import com.controlunion.excelUploader.fileUpload.dto.UpoadProgressDto;
import com.controlunion.excelUploader.fileUpload.dto.WebSocketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

@Controller
//@RequiredArgsConstructor
//@CrossOrigin("http://localhost:4200")
public class WebSocketController {

    @MessageMapping("/upload-progress")
    @SendTo("/topic/upload-progress")
    public UpoadProgressDto uploadProgress(UpoadProgressDto progress) throws InterruptedException {
        System.out.println("recive message "+progress);
//        for (int i = 0 ; i < 10 ; i++){
//            Thread.sleep(1000);
//             progress.setBytesRead(i);
//             return progress;
//        }
        return progress;
    }

}