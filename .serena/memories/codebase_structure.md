# Codebase Structure

```
TK-API-NG/
├── lib/
│   └── betfair-aping-nodep.jar   # Proprietary Betfair SDK
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── uk/co/kennah/tkapi/
│   │   │       ├── Betfair.java          # Main application entry point
│   │   │       ├── client/
│   │   │       │   └── Session.java        # Handles login/logout and session management
│   │   │       ├── config/
│   │   │       │   └── ConfigLoader.java   # Loads config.properties
│   │   │       ├── io/
│   │   │       │   └── Writer.java         # Writes odds data to a file
│   │   │       ├── model/
│   │   │       │   ├── AppConfig.java      # Holds configuration data
│   │   │       │   └── MyRunner.java       # Represents a single runner (horse)
│   │   │       └── process/
│   │   │           └── DataFetcher.java    # Fetches market data from API
│   │   └── resources/
│   │       ├── client-2048.p12         # SSL certificate for authentication
│   │       └── config.properties.template # Template for user credentials
├── pom.xml                         # Maven project configuration
└── README.md                       # Project documentation
```