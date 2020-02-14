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
 */
public class MouthyOicdClient<U extends OidcProfile, V extends OidcConfiguration> extends OidcClient<U, V> {

    private OidcClient<U, V> delegate;
    private Map<String, OidcCredentials> credentials = new ConcurrentHashMap<>();

    @Override
    public CredentialsExtractor<OidcCredentials> getCredentialsExtractor() {
        return new CredentialsExtractor<OidcCredentials>() {
            @Override
            public OidcCredentials extract(WebContext webContext) {
                OidcCredentials credentials = getCredentialsExtractor().extract(webContext);
                return credentials;
            }
        };
    }

    @Override
    protected OidcCredentials retrieveCredentials(WebContext context) {
        delegate.getCredentials(context);
        return null;
    }

}
