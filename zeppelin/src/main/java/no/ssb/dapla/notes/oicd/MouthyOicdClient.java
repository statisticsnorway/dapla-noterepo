package no.ssb.dapla.notes.oicd;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.extractor.CredentialsExtractor;
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
public class MouthyOicdClient<U extends OidcProfile, V extends OidcConfiguration> extends ForwardingOicdClient<U, V> {

    private OidcClient<U, V> delegate;
    private Map<String, OidcCredentials> credentialsMap = new ConcurrentHashMap<>();

    public void setDelegate(OidcClient<U, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    OidcClient<U, V> delegate() {
        return delegate;
    }

    @Override
    public CredentialsExtractor<OidcCredentials> getCredentialsExtractor() {
        return new CredentialsThief();
    }

    private class CredentialsThief implements CredentialsExtractor<OidcCredentials> {

        @Override
        public OidcCredentials extract(WebContext webContext) {
            OidcCredentials credentials = getCredentialsExtractor().extract(webContext);
            credentialsMap.put(credentials.getUserProfile().getUsername(), credentials);
            return credentials;
        }
    }

}
