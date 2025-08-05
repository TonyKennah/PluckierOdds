package uk.co.kennah.tkapi.config;

public class AppConfig {
    private final String appId;
    private final String username;
    private final String password;
    private final String certPassword;

    public AppConfig(String appId, String username, String password, String certPassword) {
        this.appId = appId;
        this.username = username;
        this.password = password;
        this.certPassword = certPassword;
    }

    public String getAppId() {
        return appId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCertPassword() {
        return certPassword;
    }
}