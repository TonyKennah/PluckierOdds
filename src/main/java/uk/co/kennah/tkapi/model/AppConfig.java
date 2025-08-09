package uk.co.kennah.tkapi.model;

/**
 * Represents the application's configuration properties as an immutable record.
 * This data class holds credentials and keys required to interact with the Betfair API.
 *
 * @param appId The application ID for the Betfair API.
 * @param username The Betfair account username.
 * @param password The Betfair account password.
 * @param certPassword The password for the client certificate.
 */
public record AppConfig(String appId, String username, String password, String certPassword) {
}