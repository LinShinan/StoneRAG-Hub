package com.stone.rag.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;


public class FileUtils {

    private FileUtils(){}


    public static String save(MultipartFile file,String uploadDir){
        File dir = new File(uploadDir).getAbsoluteFile();
        if(!dir.exists()){
            dir.mkdirs();
        }
        String extension = getExtension(file.getOriginalFilename());
        String newFilename = UUID.randomUUID().toString() + "." + extension;
        File dest = new File(dir, newFilename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }

        return dest.getAbsolutePath();
    }


    public static String getExtension(String filename){
        if(filename==null || !filename.contains(".")){
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".")+1).toLowerCase();
    }

}
