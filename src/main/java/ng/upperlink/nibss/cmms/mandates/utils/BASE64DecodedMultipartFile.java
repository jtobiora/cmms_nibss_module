package ng.upperlink.nibss.cmms.mandates.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;


/*
* ByteArray to MultipartFile converter
*
* */
public class BASE64DecodedMultipartFile implements MultipartFile {
    private byte[] imgContent;

    private String fileName;

    private String contentType;

    private File file;

    @Value("${file.rootLocation}")
    private String uploadPath;

    public BASE64DecodedMultipartFile(byte[] imgContent,String name) {
        this.imgContent = imgContent;
        this.fileName = name;
        file = new File(uploadPath + fileName);
    }

    @Override
    public String getName() {

        return null;
    }

    //get the original file name
    @Override
    public String getOriginalFilename() {
        return fileName;
    }

    //get the content type of the file
    @Override
    public String getContentType() {

        return null;
    }

    @Override
    public boolean isEmpty() {
        return imgContent == null || imgContent.length == 0;
    }

    //get the size of the file
    @Override
    public long getSize() {
        return imgContent.length;
    }

    //get bytes
    @Override
    public byte[] getBytes() throws IOException {
        return imgContent;
    }

    //get input stream
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(imgContent);
    }

    //save file to destination
    @Override public void transferTo(File dest) throws IOException, IllegalStateException {
        try(OutputStream os = new FileOutputStream(dest)) { os.write(imgContent); }
    }
}