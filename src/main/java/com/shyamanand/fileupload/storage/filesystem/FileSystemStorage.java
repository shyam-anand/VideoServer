package com.shyamanand.fileupload.storage.filesystem;

import com.shyamanand.fileupload.storage.FileOpenFailedException;
import com.shyamanand.fileupload.storage.FileStorage;
import com.shyamanand.fileupload.storage.FileStorageFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Defines methods to store, retrieve and delete files on the local file system
 *
 * @author Shyam Anand (shyamwdr@gmail.com)
 *         03/09/17
 */
@Service
public class FileSystemStorage implements FileStorage {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemStorage.class);

    private final MessageDigest messageDigest;
    private final Path rootDir;
    final Pattern partPattern = Pattern.compile(".*part([0-9]+)");

    final SortedSet<Path> sortedPartsSet = new TreeSet<>(Comparator.comparingInt(value -> {
        String fileName = value.getFileName().toString();
        Matcher matcher = partPattern.matcher(fileName);

        if (matcher.matches()) {
            return Integer.valueOf(matcher.group(1));
        } else {
            return 0;
        }
    }));

    @Autowired
    public FileSystemStorage(Path rootDir, MessageDigest messageDigest) {
        this.rootDir = rootDir;
        this.messageDigest = messageDigest;
    }

    /**
     * Creates the uploads directory.
     */
    @PostConstruct
    @Override
    public void init() {
        try {
            if (!Files.exists(rootDir)) {
                Files.createDirectories(rootDir);
                logger.info("Uploads dir '{}' created", rootDir);
            } else if (Files.isWritable(rootDir))
                logger.info("Uploads dir '{}' exists, and is writable", rootDir);
            else
                throw new RuntimeException("Uploads directory is not writable [" + rootDir.toString() + "]");
        } catch (IOException e) {
            logger.error("Failed to create uploads directory, check folder permissions [" + rootDir + "]");
            e.printStackTrace();
        }
    }

    /**
     * Stores a file-part under a directory named checksum
     *
     * @param part     Part of the file
     * @param checksum Checksum for the original file. A subdirectory is created with this as the name.
     * @throws FileStorageFailedException
     */
    @Override
    public void storePart(final MultipartFile part, final String checksum) throws FileStorageFailedException {
        final String partName = StringUtils.cleanPath(part.getOriginalFilename());
        final Path subDir = Paths.get(rootDir + File.separator + checksum);
        if (!Files.exists(subDir)) {
            try {
                Files.createDirectory(subDir);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Failed to create directory '{}'", e.getMessage());
                throw new FileStorageFailedException("Failed to create subdirectory " + subDir + ". " + e.getMessage());
            }
        }
        try {
            if (part.isEmpty())
                throw new IllegalArgumentException("File is empty [" + partName + "]");
            if (partName.contains(".."))
                throw new IllegalArgumentException("Illegal path in file name [" + partName + "]");
            Files.copy(part.getInputStream(), subDir.resolve(partName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            throw new FileStorageFailedException(e.getMessage());
        }

        logger.info("Saved file {}", part.getOriginalFilename());
    }

    /**
     * Lists all files under the uploads directory, filtering directories and part files.
     *
     * @return Stream of Path objects for the files.
     */
    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.list(rootDir).filter(path -> !path.toFile().isDirectory() && path.toString().contains(".part"));
        } catch (IOException e) {
            logger.error(e.getMessage() + " while listing directory '" + rootDir + "'");
            return null;
        }
    }

    /**
     * Returns the Path to the original file, combining the parts under checksum directory
     *
     * @param checksum Checksum for the original file
     * @return Path to the combined file.
     * @throws FileOpenFailedException
     * @throws FileNotFoundException
     */
    @Override
    public Path getOriginalFile(final String checksum) throws FileOpenFailedException, FileNotFoundException {
        Path dir = Paths.get(rootDir.toString(), checksum);

        if (!Files.exists(dir)) {
            throw new FileNotFoundException("Directory not found: " + checksum);
        }

        if (Files.isDirectory(dir)) { // Found directory with the specified name, which should contain the parts.
            Path video = Paths.get(joinParts(dir).getPath());

            // ToDo - Fix checksum mismatch
//            try {
//                if (verifyChecksum(checksum, video.toFile())) {
//                    return video;
//                } else throw new FileOpenFailedException("Checksum validation failed");
//            } catch (IOException e) {
//                throw new FileOpenFailedException("Error while validating checksum. " + e.getMessage());
//            }

            try {
                Files.delete(dir);
            } catch (IOException e) {
                logger.error("Could not delete directory " + e.getMessage());
            }

            return video;

        } else throw new IllegalStateException("No directory named " + checksum);
    }

    /**
     * Joins the file parts under the subdirectory
     *
     * @param checksum Checksum for the original file, which is also the subdirectory name.
     * @return Resulting File
     * @throws FileOpenFailedException
     */
    private File joinParts(final Path checksum) throws FileOpenFailedException {
        logger.debug("Joining parts in checksum {}", checksum.toString());
        sortedPartsSet.clear();
        try {
            Collection<Path> partNames = Files.list(checksum).collect(Collectors.toList());
            logger.debug("{} files in {}", partNames.size(), checksum);

            Collection<String> filteredPartNames = partNames.stream().map(Path::toString).filter(partPattern.asPredicate()).collect(Collectors.toList());
            logger.debug("{} files after filter", filteredPartNames.size());

            filteredPartNames.forEach(partName -> sortedPartsSet.add(Paths.get(partName)));
        } catch (IOException e) {
            throw new FileOpenFailedException(e.getMessage());
        }

        if (sortedPartsSet.isEmpty()) {
            throw new FileOpenFailedException("No file parts found for " + checksum.toString());
        }

        String partName = sortedPartsSet.first().getFileName().toString();
        String fileName = partName.substring(0, partName.indexOf(".part"));

        // Create file in the root directory
        final File outputFile = new File(rootDir.toFile(), fileName);
        logger.debug("Creating file ", outputFile.getPath());

        try {
            OutputStream outputStream = Files.newOutputStream(outputFile.toPath(), StandardOpenOption.CREATE);
            for (Path part : sortedPartsSet) {
                try {
                    outputStream.write(Files.readAllBytes(part));
                } catch (IOException e) {
                    logger.error("Error while reading part '" + part + "'. " + e.getMessage());
                    throw new FileOpenFailedException("Error while reading part '" + part + "'. " + e.getMessage());
                }
            }
            outputStream.close();
        } catch (IOException e) {
            logger.error(e.getMessage() + " while trying to open " + outputFile + " for writing");
        }
        logger.debug("Output file size: {}b", outputFile.length());
        return outputFile;
    }

    /**
     * Verifies the checksum for the generated file against the checksum sent by the frontend.
     *
     * @param checksum Checksum sent by front-end
     * @param file     File for which the verification should be done.
     * @return TRUE on success.
     * @throws IOException
     */
    private boolean verifyChecksum(String checksum, File file) throws IOException {
        messageDigest.update(Files.readAllBytes(file.toPath()));
        byte[] digest = messageDigest.digest();
        String fileChecksum = DatatypeConverter.printHexBinary(digest).toLowerCase();
        logger.info("Checksum - file: {}, received: {} ", fileChecksum, checksum);
        return fileChecksum.equalsIgnoreCase(checksum);
    }

    /**
     * Returns the file for playback as a Resource object
     *
     * @param filename Video file name
     * @return Resource object to the file
     * @throws FileOpenFailedException
     * @throws FileNotFoundException
     */
    @Override
    public Resource load(String filename) throws FileOpenFailedException, FileNotFoundException {
        Path filePath = Paths.get(rootDir.toString(), filename);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Cannot find file '" + filename + "'");
        }
        InputStream inputStream = new FileInputStream(filePath.toFile());

        byte content[] = new byte[(int) filePath.toFile().length()];
        try {
            inputStream.read(content);
        } catch (IOException e) {
            throw new FileOpenFailedException("Could not open file for reading. " + filePath.toString());
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ByteArrayResource(content);
    }

    /**
     * Deletes all files and directories under uploads dir.
     */
    @Override
    public void deleteAll() {
        try {
            Files.list(rootDir).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    logger.error(e.getMessage() + " while trying to delete '" + path + "'");
                }
            });
        } catch (IOException e) {
            logger.error(e.getMessage() + " while listing directory " + rootDir);
        }
    }
}
