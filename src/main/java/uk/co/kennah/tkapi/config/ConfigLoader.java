package uk.co.kennah.tkapi.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

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