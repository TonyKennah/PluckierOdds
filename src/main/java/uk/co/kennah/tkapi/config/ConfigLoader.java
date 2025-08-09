package uk.co.kennah.tkapi.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import uk.co.kennah.tkapi.model.AppConfig;

/**
 * Loads application configuration from a properties file.
 * This class is responsible for reading the `config.properties` file from the
 * classpath, extracting necessary properties like API credentials, and creating
 * an {@link AppConfig} object.
 */
public class ConfigLoader {

    /**
     * Loads the configuration from the `config.properties` file.
     * @return An {@link AppConfig} object populated with the properties.
     * @throws RuntimeException if the properties file cannot be found or loaded.
     * @throws IllegalStateException if any required properties are missing or empty.
     */
    public AppConfig load() {
        Properties props = new Properties();
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new RuntimeException("Could not find config.properties on the classpath. Make sure it's in src/main/resources.");
            }
            props.load(in);

            String appId = props.getProperty("betfair.appid");
            String username = props.getProperty("betfair.username");
            String password = props.getProperty("betfair.password");
            String certPassword = props.getProperty("betfair.cert.password");

            if (appId == null || username == null || password == null || certPassword == null ||
                appId.isEmpty() || username.isEmpty() || password.isEmpty() || certPassword.isEmpty()) {
                throw new IllegalStateException("One or more required properties are missing from config.properties. Please check the file.");
            }
            return new AppConfig(appId, username, password, certPassword);
        } catch (IOException e) {
            throw new RuntimeException("Could not load config.properties. Make sure the file exists and is readable.", e);
        }
    }
}