# Code Style and Conventions

- **Naming Conventions:**
  - **Classes:** `PascalCase` (e.g., `DataFetcher`, `AppConfig`).
  - **Methods:** `camelCase` (e.g., `getOdds`, `printMarketCatalogue`).
  - **Variables:** `camelCase` (e.g., `marketFilter`, `sessionToken`).
- **Comments:**
  - No formal JavaDoc comments are used.
  - Single-line comments are used to explain specific code sections.
- **Formatting:**
  - **Indentation:** Tabs are used.
  - **Braces:** Opening braces are placed on the same line as the class or method declaration.
  - **Spacing:** Code is well-formatted with whitespace for readability.
- **Error Handling:**
  - Errors are printed to the standard error stream using `System.err.println`.
  - Exception stack traces are printed using `e.printStackTrace()`.