# Commands

### Dependency Management

- **Install Local Betfair SDK:**
  ```bash
  mvn install:install-file -Dfile=lib/betfair-aping-nodep.jar -DgroupId=com.betfair.aping -DartifactId=betfair-aping-sdk -Dversion=1.0 -Dpackaging=jar
  ```

### Build

- **Clean and Package:**
  ```bash
  mvn clean package
  ```

### Testing

- **Run Tests:**
  ```bash
  mvn test
  ```

### Execution

- **Run Application:**
  ```bash
  java --add-opens java.base/java.lang=ALL-UNNAMED -jar target/tk-api-ng-1.0.jar [YYYY-MM-DD]
  ```
  *(The date argument is optional. If not provided, the current date will be used.)*