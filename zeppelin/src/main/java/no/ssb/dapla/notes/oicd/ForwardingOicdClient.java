package no.ssb.dapla.notes.oicd;

import org.pac4j.core.authorization.generator.AuthorizationGenerator;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.credentials.extractor.CredentialsExtractor;
import org.pac4j.core.http.ajax.AjaxRequestResolver;
import org.pac4j.core.http.callback.CallbackUrlResolver;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.core.logout.LogoutActionBuilder;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.core.redirect.RedirectAction;
import org.pac4j.core.redirect.RedirectActionBuilder;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;

import java.util.List;
import java.util.Map;

/**
 * Decorator pattern around OidcClient
 */
public abstract class ForwardingOicdClient<U extends OidcProfile, V extends OidcConfiguration> extends OidcClient<U, V> {

    abstract OidcClient<U, V> delegate();

    @Override
    public V getConfiguration() {
        return delegate().getConfiguration();
    }

    @Override
    public void setConfiguration(V configuration) {
        delegate().setConfiguration(configuration);
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

    @Override
    public RedirectAction getRedirectAction(WebContext context) {
        return delegate().getRedirectAction(context);
    }

    @Override
    public String computeFinalCallbackUrl(WebContext context) {
        return delegate().computeFinalCallbackUrl(context);
    }

    @Override
    public String getCallbackUrl() {
        return delegate().getCallbackUrl();
    }

    @Override
    public void setCallbackUrl(String callbackUrl) {
        delegate().setCallbackUrl(callbackUrl);
    }

    @Override
    public UrlResolver getUrlResolver() {
        return delegate().getUrlResolver();
    }

    @Override
    public void setUrlResolver(UrlResolver urlResolver) {
        delegate().setUrlResolver(urlResolver);
    }

    @Override
    public CallbackUrlResolver getCallbackUrlResolver() {
        return delegate().getCallbackUrlResolver();
    }

    @Override
    public void setCallbackUrlResolver(CallbackUrlResolver callbackUrlResolver) {
        delegate().setCallbackUrlResolver(callbackUrlResolver);
    }

    @Override
    public AjaxRequestResolver getAjaxRequestResolver() {
        return delegate().getAjaxRequestResolver();
    }

    @Override
    public void setAjaxRequestResolver(AjaxRequestResolver ajaxRequestResolver) {
        delegate().setAjaxRequestResolver(ajaxRequestResolver);
    }

    @Override
    public RedirectActionBuilder getRedirectActionBuilder() {
        return delegate().getRedirectActionBuilder();
    }

    @Override
    public void setRedirectActionBuilder(RedirectActionBuilder redirectActionBuilder) {
        delegate().setRedirectActionBuilder(redirectActionBuilder);
    }


    @Override
    public LogoutActionBuilder<U> getLogoutActionBuilder() {
        return delegate().getLogoutActionBuilder();
    }

    @Override
    public void setLogoutActionBuilder(LogoutActionBuilder<U> logoutActionBuilder) {
        delegate().setLogoutActionBuilder(logoutActionBuilder);
    }

    @Override
    public String getName() {
        return delegate().getName();
    }

    @Override
    public void setName(String name) {
        delegate().setName(name);
    }

    @Override
    public void notifySessionRenewal(String oldSessionId, WebContext context) {
        delegate().notifySessionRenewal(oldSessionId, context);
    }

    @Override
    public List<AuthorizationGenerator<U>> getAuthorizationGenerators() {
        return delegate().getAuthorizationGenerators();
    }

    @Override
    public void setAuthorizationGenerators(List<AuthorizationGenerator<U>> authorizationGenerators) {
        delegate().setAuthorizationGenerators(authorizationGenerators);
    }

    @Override
    public void setAuthorizationGenerators(AuthorizationGenerator<U>... authorizationGenerators) {
        delegate().setAuthorizationGenerators(authorizationGenerators);
    }

    @Override
    public void setAuthorizationGenerator(AuthorizationGenerator<U> authorizationGenerator) {
        delegate().setAuthorizationGenerator(authorizationGenerator);
    }

    @Override
    public void addAuthorizationGenerator(AuthorizationGenerator<U> authorizationGenerator) {
        delegate().addAuthorizationGenerator(authorizationGenerator);
    }

    @Override
    public void addAuthorizationGenerators(List<AuthorizationGenerator<U>> authorizationGenerators) {
        delegate().addAuthorizationGenerators(authorizationGenerators);
    }

    @Override
    public CredentialsExtractor<OidcCredentials> getCredentialsExtractor() {
        return delegate().getCredentialsExtractor();
    }

    @Override
    public void setCredentialsExtractor(CredentialsExtractor<OidcCredentials> credentialsExtractor) {
        delegate().setCredentialsExtractor(credentialsExtractor);
    }

    @Override
    public Authenticator<OidcCredentials> getAuthenticator() {
        return delegate().getAuthenticator();
    }

    @Override
    public void setAuthenticator(Authenticator<OidcCredentials> authenticator) {
        delegate().setAuthenticator(authenticator);
    }

    @Override
    public ProfileCreator<OidcCredentials, U> getProfileCreator() {
        return delegate().getProfileCreator();
    }

    @Override
    public void setProfileCreator(ProfileCreator<OidcCredentials, U> profileCreator) {
        delegate().setProfileCreator(profileCreator);
    }

    @Override
    public Map<String, Object> getCustomProperties() {
        return delegate().getCustomProperties();
    }

    @Override
    public void setCustomProperties(Map<String, Object> customProperties) {
        delegate().setCustomProperties(customProperties);
    }

    @Override
    public void init() {
        delegate().init();
    }
}
