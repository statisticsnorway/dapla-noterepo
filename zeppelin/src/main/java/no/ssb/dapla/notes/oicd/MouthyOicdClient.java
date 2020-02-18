package no.ssb.dapla.notes.oicd;

import org.pac4j.core.client.Client;
import org.pac4j.core.context.WebContext;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;

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
public class MouthyOicdClient extends ForwardingClient<OidcCredentials, OidcProfile> {

    private Client<OidcCredentials, OidcProfile> delegate;

    private Map<String, OidcCredentials> credentialsMap = new ConcurrentHashMap<>();

    OidcCredentials get(String userName) {
        return credentialsMap.get(userName);
    }

    public void setDelegate(Client<OidcCredentials, OidcProfile> delegate) {
        this.delegate = delegate;
    }

    @Override
    Client<OidcCredentials, OidcProfile> delegate() {
        return delegate;
    }

    @Override
    public OidcCredentials getCredentials(WebContext webContext) {
        OidcCredentials credentials = super.getCredentials(webContext);
        credentialsMap.put(credentials.getUserProfile().getId(), credentials);
        return credentials;
    }
}
