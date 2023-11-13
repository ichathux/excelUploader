//package com.controlunion.excelUploader.fileUpload;
//
//import org.apache.tomcat.util.http.fileupload.ProgressListener;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.io.InputStream;
//
//public class ProgressMultipartFileWrapper implements MultipartFile {
//
//    private final MultipartFile file;
//    private final ProgressListener progressListener;
//
//    public ProgressMultipartFileWrapper(MultipartFile file, ProgressListener progressListener) {
//        this.file = file;
//        this.progressListener = progressListener;
//    }
//
//    @Override
//    public String getName() {
//        return file.getName();
//    }
//
//    @Override
//    public String getOriginalFilename() {
//        return file.getOriginalFilename();
//    }
//
//    @Override
//    public String getContentType() {
//        return file.getContentType();
//    }
//
//    @Override
//    public boolean isEmpty() {
//        return file.isEmpty();
//    }
//
//    @Override
//    public long getSize() {
//        return file.getSize();
//    }
//
//    @Override
//    public byte[] getBytes() throws IOException {
//        byte[] bytes = file.getBytes();
//        progressListener.update(bytes.length, bytes.length, 1);
//        return bytes;
//    }
//
//    @Override
//    public InputStream getInputStream() throws IOException {
//        return new ProgressInputStream(file.getInputStream(), progressListener, file.getSize());
//    }
//
//    @Override
//    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
//        file.transferTo(dest);
//        progressListener.update(file.getSize(), file.getSize(), 1);
//    }
//}