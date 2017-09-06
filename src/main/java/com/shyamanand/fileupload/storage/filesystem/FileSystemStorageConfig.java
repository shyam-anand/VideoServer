package com.shyamanand.fileupload.storage.filesystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Shyam Anand (shyamwdr@gmail.com)
 *         03/09/17
 */
@Configuration
public class FileSystemStorageConfig {

    private final String location;

    @Autowired
    public FileSystemStorageConfig(@Value("${file.storage.uploadsDir}") String location) {
        this.location = location;
    }

    @Bean
    public Path uploadsDir() {
        return Paths.get(location);
    }

    @Bean
    public MessageDigest getMD5() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

}
