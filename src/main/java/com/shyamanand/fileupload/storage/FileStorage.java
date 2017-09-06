package com.shyamanand.fileupload.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * @author Shyam Anand (shyamwdr@gmail.com)
 *         03/09/17
 */
public interface FileStorage {

    void init();

    void storePart(MultipartFile part, String checksum) throws FileStorageFailedException;

    Stream<Path> loadAll();

    Path getOriginalFile(String filename) throws FileOpenFailedException, FileNotFoundException;

    Resource load(String filename) throws FileOpenFailedException, FileNotFoundException;

    void deleteAll();

}
