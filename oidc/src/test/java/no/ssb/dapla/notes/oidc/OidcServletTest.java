package no.ssb.dapla.notes.oidc;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.oidc.credentials.OidcCredentials;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

class OidcServletTest {

    private MouthyKeycloakOidcClient client = new MouthyKeycloakOidcClient();

    private static String toString(InputStream input) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    @AfterEach
    void tearDown() throws Exception {
        client.getServer().stop();
    }

    @BeforeEach
    void setUp() throws Exception {
        client.getServer().start();
    }

    @Test
    void testSupportsMissing() throws IOException {
        String accessToken = toString(new URL("http://localhost:9877/oidc/missing/access").openStream());
        String refreshToken = toString(new URL("http://localhost:9877/oidc/missing/refresh").openStream());
        assertThat(accessToken).isEmpty();
        assertThat(refreshToken).isEmpty();
    }

    @Test
    void testCanReadTokens() throws Exception {

        OidcCredentials credentials = new OidcCredentials();
        credentials.setAccessToken(new BearerAccessToken("access token"));
        credentials.setRefreshToken(new RefreshToken("refresh token"));
        client.set("userName", credentials);

        String accessToken = toString(new URL("http://localhost:9877/oidc/userName/access").openStream());
        String refreshToken = toString(new URL("http://localhost:9877/oidc/userName/refresh").openStream());

        assertThat(accessToken).isEqualTo("access token");
        assertThat(refreshToken).isEqualTo("refresh token");

    }

    @Test
    void testSupportsWeirdNames() throws Exception {

        OidcCredentials credentials = new OidcCredentials();
        credentials.setAccessToken(new BearerAccessToken("access token"));
        credentials.setRefreshToken(new RefreshToken("refresh token"));
        client.set("a weird+&$#userName@domain.com", credentials);


        URL accessUrl = new URI(
                "http",
                null,
                "localhost",
                9877,
                "/oidc/a weird+&$#userName@domain.com/access",
                null,
                null
        ).toURL();

        URL refreshUrl = new URI(
                "http",
                null,
                "localhost",
                9877,
                "/oidc/a weird+&$#userName@domain.com/refresh",
                null,
                null
        ).toURL();

        String accessToken = toString(accessUrl.openStream());
        String refreshToken = toString(refreshUrl.openStream());

        assertThat(accessToken).isEqualTo("access token");
        assertThat(refreshToken).isEqualTo("refresh token");

    }
}