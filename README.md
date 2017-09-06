# Video Server
 
This application receives files uploaded in parts, and joins them for play back.
 
## Installation

You will need maven to run the project. 

1. Download the sources
2. Run `mvn spring-boot:run` to directly run from command line
3. You can deploy this to existing Tomcat server as well. 
Run `mvn install` to build the war file. And then use the war file to deploy to Tomcat.

## Configuration

Configurations go inside `resources/application.properties`.

## Other Details

### Important source files

All the sources referred to are under `src/main/java/com/shyamanand/fileupload`.

#### `web.controllers.FilesController controller`

This class defines the REST endpoints to receive requests for uploading the files, and for the video URL. This depends on the `FileStorage` interface 
to store and retrieve files.

#### `storage.FileStorage interface`

This interface declares method to store, retrieve and delete files.

#### `storage.filesystem.FileSystemStorage`

This class implements `storage.FileStorage` and defines methods to store and retrieve files on the local filesystem.

### Receiving video file in chunks

The endpoint `/files/parts` accepts `POST` requests with a part of the file, and the checksum for the original file.

A subdirectory is created for the file, with the checksum as the directory name. All the parts for the file is stored under this dir. On receiving a `GET`
request on `/files/{checksum}` the file parts under the `{checksum}` directory are joined. This will return the path to the combined file.


### Playback 

The endpoint `/files/play/{filename}` returns the video file. This endpoint can be used as the `src` for rendering the `<video>` element in the front-end.  