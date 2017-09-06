package com.shyamanand.fileupload.web.controllers;

import com.shyamanand.fileupload.storage.FileOpenFailedException;
import com.shyamanand.fileupload.storage.FileStorage;
import com.shyamanand.fileupload.storage.FileStorageFailedException;
import com.shyamanand.fileupload.web.models.ApiResponse;
import com.shyamanand.fileupload.web.models.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Shyam Anand (shyamwdr@gmail.com)
 *         03/09/17
 */
@RestController
@CrossOrigin
@RequestMapping("/files")
public class FilesController {
    private static final Logger logger = LoggerFactory.getLogger(FilesController.class);

    @Autowired
    private FileStorage fileStorage;

    @RequestMapping(value = "/parts", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity uploadHandler(@RequestParam("chunk") MultipartFile part,
                                        @RequestParam("checksum") String checksum,
                                        HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        StringBuilder parameters = new StringBuilder();
        params.forEach((key, value) ->
                parameters.append(String.format("%s => %s | ", key, Arrays.asList(value).stream()
                        .filter(s -> s.length() > 0 && !s.isEmpty())
                        .reduce("", (a, b) -> a.length() > 0 ? a + "; " + b : b))));
        try {
            parameters.append(String.format("chunk %s, size: %db", part.getOriginalFilename(), part.getBytes().length));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("{}", parameters.toString());


        logger.debug("Storing part {}", part.getOriginalFilename());
        try {
            fileStorage.storePart(part, checksum);
            return new ResponseEntity<>(new ApiResponse<>("File saved successfully"), HttpStatus.CREATED);
        } catch (FileStorageFailedException e) {
            logger.error(e.getMessage());
            ErrorDetails errorDetails = new ErrorDetails();
            errorDetails.setTitle("Failed to save part '" + part.getOriginalFilename() + "'");
            errorDetails.setDetails(e.getMessage());
            ApiResponse apiResponse = new ApiResponse(errorDetails);
            return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/{checksum}", method = RequestMethod.GET)
    public ResponseEntity getFile(@PathVariable("checksum") String checksum) throws FileOpenFailedException, FileNotFoundException {
        logger.debug("Loading file {}", checksum);

        Path filePath = fileStorage.getOriginalFile(checksum);
        logger.info("Loaded file {}", filePath);

        ApiResponse response = new ApiResponse<>(filePath.getFileName().toString());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/play/{filename}", method = RequestMethod.GET)
    public StreamingResponseBody play(@PathVariable("filename") String fileName) throws FileOpenFailedException, FileNotFoundException {
        ByteArrayResource content = (ByteArrayResource) fileStorage.load(fileName);

        return outputStream -> outputStream.write(content.getByteArray());
    }

    @RequestMapping(value = "/", method = RequestMethod.DELETE)
    public ResponseEntity deleteAll() {
        fileStorage.deleteAll();
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity list() {
        List<Path> files = fileStorage.loadAll().collect(Collectors.toList());
        ApiResponse response = new ApiResponse<>(files);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
