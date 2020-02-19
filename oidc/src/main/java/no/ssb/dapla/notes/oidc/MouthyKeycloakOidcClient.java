package no.ssb.dapla.notes.oidc;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.KeycloakOidcClient;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.keycloak.KeycloakOidcProfile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This pac4j client exposes the tokens of the user tokens via REST.
 * <p>
 * Tokens are saved in-memory and will happily be given to whomever asks for
 * them. You have been warned.
 * <p>
 * One should set it up in the shiro configuration file as follow:
 * <pre>
 *     keycloakOidcClient = no.ssb.dapla.notes.oidc.MouthyKeycloakOidcClient
 *     keycloakOidcClient.port = 9877
 *     keycloakOidcClient.name = keycloakOidcClient
 *     keycloakOidcClient.configuration = $oidcConfig
 *     keycloakOidcClient.authorizationGenerator = $roleAdminAuthGenerator
 *
 * clients = org.pac4j.core.client.Clients
 * clients.callbackUrl = http://localhost:8080/api/callback
 * clients.clients = $mouthyOicdClient
 * </pre>
 */
public final class MouthyKeycloakOidcClient extends KeycloakOidcClient {

    private Map<String, OidcCredentials> credentialsMap = new ConcurrentHashMap<>();
    private int port = 9877;
    private String allowedClient = ".*";
    private Server server;

    int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    OidcCredentials get(String userName) {
        return credentialsMap.get(userName);
    }

    void set(String userName, OidcCredentials credentials) {
        credentialsMap.put(userName, credentials);
    }

    Server getServer() {
        if (server == null) {
            server = new Server(port);
            ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            ctx.setContextPath("/");
            ctx.addServlet(
                    new ServletHolder(new OidcServlet(this)),
                    "/oidc/*"
            );
            server.setHandler(ctx);
        }
        return server;
    }

    @Override
    protected void clientInit() {
        super.clientInit();
        setProfileCreator(new StealingProfileCreator(getProfileCreator()));
        try {
            getServer().start();
        } catch (Exception e) {
            throw new RuntimeException("Could not start the server", e);
        }
    }

    String getAllowedClient() {
        return allowedClient;
    }

    public void setAllowedClient(String allowedClient) {
        this.allowedClient = allowedClient;
    }

    private final class StealingProfileCreator implements ProfileCreator<OidcCredentials, KeycloakOidcProfile> {

        private final ProfileCreator<OidcCredentials, KeycloakOidcProfile> delegate;

        private StealingProfileCreator(ProfileCreator<OidcCredentials, KeycloakOidcProfile> delegate) {
            this.delegate = delegate;
        }

        @Override
        public KeycloakOidcProfile create(OidcCredentials oidcCredentials, WebContext webContext) {
            KeycloakOidcProfile profile = delegate.create(oidcCredentials, webContext);
            credentialsMap.put(profile.asPrincipal().getName(), oidcCredentials);
            return profile;
        }
    }
}
