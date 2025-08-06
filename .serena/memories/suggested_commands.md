# Suggested Commands

This is a list of the most common commands you will need during development.

### Initial Setup

First, you must install the local Betfair SDK. This only needs to be done once.

```bash
mvn install:install-file -Dfile=lib/betfair-aping-nodep.jar -DgroupId=com.betfair.aping -DartifactId=betfair-aping-sdk -Dversion=1.0 -Dpackaging=jar
```

### Common Development Cycle

1.  **Build the project:**

    ```bash
    mvn clean package
    ```

2.  **Run the application:**

    ```bash
    java --add-opens java.base/java.lang=ALL-UNNAMED -jar target/tk-api-ng-1.0.jar
    ```

    To run for a specific date:

    ```bash
    java --add-opens java.base/java.lang=ALL-UNNAMED -jar target/tk-api-ng-1.0.jar YYYY-MM-DD
    ```

### Testing

Run the automated tests:

```bash
mvn test
```