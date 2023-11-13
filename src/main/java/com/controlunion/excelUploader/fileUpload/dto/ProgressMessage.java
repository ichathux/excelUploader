package com.controlunion.excelUploader.fileUpload.dto;

import lombok.Data;

@Data
public class ProgressMessage {
    private final long bytesRead;
    private final long contentLength;
    private final boolean done;

    public ProgressMessage(long bytesRead, long contentLength, boolean done) {
        this.bytesRead = bytesRead;
        this.contentLength = contentLength;
        this.done = done;
    }
}
