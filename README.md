# Video Server
 
This application receives files uploaded in parts, and joins them for play back.
 
### Installation

You will need maven to run the project. 

1. Download the sources
2. Run `mvn spring-boot:run` to directly run from command line
3. You can deploy this to existing Tomcat server as well. 
Run `mvn install` to build the war file. And then use the war file to deploy to Tomcat.