package no.ssb.dapla.notes.oicd;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.KeycloakOidcClient;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;
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
 * keycloakOidcClient = org.pac4j.oidc.client.KeycloakOidcClient
 * keycloakOidcClient.name = keycloakOidcClient
 * keycloakOidcClient.configuration = $oidcConfig
 * keycloakOidcClient.authorizationGenerator = $roleAdminAuthGenerator
 *
 * mouthyOicdClient = no.ssb.dapla.notes.oicd.MouthyOicdClient
 * mouthyOicdClient.delegate = $keycloakOidcClient
 *
 * clients = org.pac4j.core.client.Clients
 * clients.callbackUrl = http://localhost:8080/api/callback
 * clients.clients = $mouthyOicdClient
 * </pre>
 */
public class MouthyKeycloakOidcClient extends KeycloakOidcClient {

    private Map<String, OidcCredentials> credentialsMap = new ConcurrentHashMap<>();
    private int port;
    private Server server;

    OidcCredentials get(String userName) {
        return credentialsMap.get(userName);
    }

    @Override
    protected void clientInit() {
        super.clientInit();
        setProfileCreator(new StealingProfileCreator(getProfileCreator()));

        if (server == null) {
            server = new Server(9877);
            ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            ctx.setContextPath("/");
            ctx.addServlet(
                    new ServletHolder(new OicdServlet(this)),
                    "/oicd/*"
            );
            server.setHandler(ctx);
            try {
                server.start();
            } catch (Exception e) {
                throw new RuntimeException("Could not start the server", e);
            }
        }

    }

    @Override
    public OidcCredentials retrieveCredentials(WebContext webContext) {
        OidcCredentials credentials = super.retrieveCredentials(webContext);
        OidcProfile profile = super.getUserProfile(credentials, webContext);
        credentialsMap.put(profile.asPrincipal().getName(), credentials);
        return credentials;
    }

    private class StealingProfileCreator implements ProfileCreator<OidcCredentials, KeycloakOidcProfile> {

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
