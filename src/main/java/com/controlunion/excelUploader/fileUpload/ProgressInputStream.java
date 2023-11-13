//package com.controlunion.excelUploader.fileUpload;
//
//import org.apache.tomcat.util.http.fileupload.ProgressListener;
//
//import java.io.IOException;
//import java.io.InputStream;
//
//public class ProgressInputStream extends InputStream {
//
//    private final InputStream inputStream;
//    private final ProgressListener progressListener;
//    private final long totalSize;
//    private long bytesRead;
//
//    public ProgressInputStream(InputStream inputStream, ProgressListener progressListener, long totalSize) {
//        this.inputStream = inputStream;
//        this.progressListener = progressListener;
//        this.totalSize = totalSize;
//    }
//
//    @Override
//    public int read() throws IOException {
//        int b = inputStream.read();
//        if (b != -1) {
//            bytesRead++;
//            progressListener.update(bytesRead, totalSize, 0);
//        } else {
//            progressListener.update(totalSize, totalSize, 1);
//        }
//        return b;
//    }
//
//    // Other read methods from InputStream interface
//}