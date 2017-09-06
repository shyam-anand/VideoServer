package com.shyamanand.fileupload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Shyam Anand (shyamwdr@gmail.com)
 *         03/09/17
 */
@EnableWebMvc
@SpringBootApplication
public class FileUpload {

    public static void main(String[] args) {
        SpringApplication.run(FileUpload.class, args);
    }
}
