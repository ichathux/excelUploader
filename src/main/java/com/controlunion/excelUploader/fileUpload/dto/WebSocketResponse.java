package com.controlunion.excelUploader.fileUpload.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@NoArgsConstructor
@Setter
public class WebSocketResponse {
    private String message;
    private int progress;

}
