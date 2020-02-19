package no.ssb.dapla.notes.oidc;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;

class MouthyOicdClientTest {

    private Server server;

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
        server.destroy();
    }

    @BeforeEach
    void setUp() throws Exception {
        server = new Server(9877);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath("/");
        ctx.addServlet(
                new ServletHolder(new OidcServlet(null)),
                "/oicd/*"
        );
        server.setHandler(ctx);
        //oicdClient.getCredentials(null);
        server.start();
    }

    @Test
    void testJerseyServer() {

    }

    private static class TestOicdClient extends IndirectClient<OidcCredentials, OidcProfile> {

        @Override
        public String getName() {
            return "TEST";
        }

        @Override
        protected OidcCredentials retrieveCredentials(WebContext context) {
            OidcCredentials credentials = new OidcCredentials();
            credentials.setAccessToken(new BearerAccessToken("accessToken"));
            credentials.setRefreshToken(new RefreshToken("refreshToken"));
            CommonProfile profile = new CommonProfile();
            profile.setId("id");
            credentials.setUserProfile(profile);
            return credentials;
        }

        @Override
        protected void clientInit() {

        }
    }
}