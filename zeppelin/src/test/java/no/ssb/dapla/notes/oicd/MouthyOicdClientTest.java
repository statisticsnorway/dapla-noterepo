package no.ssb.dapla.notes.oicd;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.client.Client;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.redirect.RedirectAction;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;

class MouthyOicdClientTest {

    @BeforeEach
    void setUp() throws Exception {
        Server server = new Server(9877);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath("/");
        MouthyOicdClient oicdClient = new MouthyOicdClient();
        oicdClient.setDelegate(new TestOicdClient());

        ctx.addServlet(
                new ServletHolder(new OicdServlet(oicdClient)),
                "/oicd/*"
        );
        server.setHandler(ctx);

        oicdClient.getCredentials(null);

        try {
            server.start();
            server.join();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            server.destroy();
        }
    }

    @Test
    void testJerseyServer() {

    }

    private static class TestOicdClient implements Client<OidcCredentials, OidcProfile> {

        @Override
        public String getName() {
            return "TEST";
        }

        @Override
        public HttpAction redirect(WebContext webContext) {
            return null;
        }

        @Override
        public OidcCredentials getCredentials(WebContext webContext) {
            OidcCredentials credentials = new OidcCredentials();
            credentials.setAccessToken(new BearerAccessToken("accessToken"));
            credentials.setRefreshToken(new RefreshToken("refreshToken"));
            CommonProfile profile = new CommonProfile();
            profile.setId("id");
            credentials.setUserProfile(profile);
            return credentials;
        }

        @Override
        public OidcProfile getUserProfile(OidcCredentials oidcCredentials, WebContext webContext) {
            return null;
        }

        @Override
        public RedirectAction getLogoutAction(WebContext webContext, OidcProfile oidcProfile, String s) {
            return null;
        }
    }
}