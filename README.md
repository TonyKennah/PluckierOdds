# TK-API-NG

A Java application for fetching odds data from the Betfair API.

## Prerequisites

*   Java Development Kit (JDK) 8 or higher.
*   Apache Maven.

## Setup

This project depends on a proprietary Betfair SDK which is not available in public Maven repositories. You must install it into your local Maven repository before you can build the project.

### 1. Install the Local Dependency

The required JAR file, `betfair-aping-nodep.jar`, is included in the `/lib` directory of this project.

From the root directory of the project, run the following command to install the JAR into your local Maven repository:
```bash
mvn install:install-file -Dfile=lib/betfair-aping-nodep.jar -DgroupId=com.betfair.aping -DartifactId=betfair-aping-sdk -Dversion=1.0 -Dpackaging=jar
```

### 2. Configure Credentials

The application requires your Betfair credentials to be configured.

1.  Navigate to `src/main/resources/`.
2.  Make a copy of `config.properties.template` and rename it to `config.properties`.
3.  Fill in your details in the new `config.properties` file.

*Note: `config.properties` is included in `.gitignore` to prevent you from accidentally committing your credentials.*

## Building the Project

Once the setup is complete, you can build the application using Maven. This will compile the code, run tests, and package everything into a single executable JAR file.

```bash
mvn clean package
```

The final JAR will be located in the `target/` directory.

## Running the Application

To run the application, execute the JAR from the project root. You can optionally provide a date in `YYYY-MM-DD` format as a command-line argument. If no date is provided, the application will use the current date.

### Running for a specific date

```bash
java --add-opens java.base/java.lang=ALL-UNNAMED -jar target/tk-api-ng-1.0-SNAPSHOT.jar YYYY-MM-DD
```

**Note on `--add-opens`:** This flag is required when running on Java 9 or newer. It allows the Gson library (a dependency) to function correctly with Java's module system.

## Application Output

If all is good you will see the length of the files in standard out

   - Length of file is: 37202 bytes
   - Length of file is: 37202 bytes

and two identical files will be created in the project root folder:

   - yyyy.mm.dd.ODDS.data
   - yyyy-mm-dd.ODDSlatest.data

   These files will contain horses from numerous markets that were specified in the code "GB", "IE", "ZA", "FR", "AE", and "US".  The names listed with current odds from the win only market (seperated by a #):

        Emilys Choice#11.5
        Montauk Memoirs#2.08
        Starzo Fal#1.28
        Purple Mood#1.28
        Grace Faraday#7.0
        Hooray Austin#1.15