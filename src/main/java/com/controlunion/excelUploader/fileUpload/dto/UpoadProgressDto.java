package com.controlunion.excelUploader.fileUpload.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UpoadProgressDto {

    private long bytesRead;
    private long contentLength;
    private boolean done;

    public UpoadProgressDto(long bytesRead, long totalSize, int done) {

    }
}
