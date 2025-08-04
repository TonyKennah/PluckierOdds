# TK-API-NG

A simple Java application to interact with the Betfair API-NG.

## Prerequisites

1.  A Java Development Kit (JDK), version 8 or newer.
2.  Your Betfair Application Key and a `client-2048.p12` certificate file. See the Betfair documentation for details.
3.  The required dependency JAR files in the project's root directory.

## Configuration

1.  Place your `client-2048.p12` certificate file in the root directory of the project.
2.  Create a file named `config.properties` in the project's root directory.
3.  Add your personal credentials to `config.properties` with the following keys:

    ```properties
    betfair.appid=YOUR_APP_ID
    betfair.username=YOUR_BETFAIR_USERNAME
    betfair.password=YOUR_BETFAIR_PASSWORD
    betfair.cert.password=YOUR_CERTIFICATE_PASSWORD
    ```

    **Note:** This file contains sensitive information and should not be committed to public source control. If using Git, add `config.properties` to your `.gitignore` file.



## Building and Running

1.  **Compile the code:**
    Open a command prompt in the project root and run the following command. This creates a `bin` directory for the compiled `.class` files.
    ```bash
    javac -d bin -cp ".;betfair-aping-1.0.jar;client-combined-3.1.0-nodeps.jar;commons-exec-1.3.jar;commons-logging-1.2.jar;gson-2.3.1.jar;guava-21.0.jar;httpclient-4.5.2.jar;httpcore-4.4.4.jar;httpmime-4.5.2.jar;jna-4.1.0.jar;jna-platform-4.1.0.jar;jsoup-1.10.3.jar" Betfair.java BetfairFace.java
    ```

2.  **Run the application:**
    ```bash
    java -cp ".;bin;betfair-aping-1.0.jar;client-combined-3.1.0-nodeps.jar;commons-exec-1.3.jar;commons-logging-1.2.jar;gson-2.3.1.jar;guava-21.0.jar;httpclient-4.5.2.jar;httpcore-4.4.4.jar;httpmime-4.5.2.jar;jna-4.1.0.jar;jna-platform-4.1.0.jar;jsoup-1.10.3.jar" Betfair
    ```

3.  **Running on Java 9+:**
    If you are using Java 9 or newer, you may need to add the `--add-opens` flag to the `java` command to avoid reflection errors with older libraries:
    ```bash
    java --add-opens java.base/java.lang=ALL-UNNAMED -cp ".;bin;betfair-aping-1.0.jar;client-combined-3.1.0-nodeps.jar;commons-exec-1.3.jar;commons-logging-1.2.jar;gson-2.3.1.jar;guava-21.0.jar;httpclient-4.5.2.jar;httpcore-4.4.4.jar;httpmime-4.5.2.jar;jna-4.1.0.jar;jna-platform-4.1.0.jar;jsoup-1.10.3.jar" Betfair
    ```
