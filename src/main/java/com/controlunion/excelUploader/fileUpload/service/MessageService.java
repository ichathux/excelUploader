package com.controlunion.excelUploader.fileUpload.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessageToUser(String userId, String message) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/specific-user", message);
    }

    public void sendMessageToTopic(String message) {
        messagingTemplate.convertAndSend("/topic/my-topic", message);
    }
}
