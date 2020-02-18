package no.ssb.dapla.notes.oicd;

import org.pac4j.core.client.Client;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.redirect.RedirectAction;


/**
 * Decorator pattern around Client
 */
public abstract class ForwardingClient<C extends Credentials, U extends CommonProfile> implements Client<C, U> {

    abstract Client<C, U> delegate();

    @Override
    public String getName() {
        return delegate().getName();
    }

    @Override
    public HttpAction redirect(WebContext webContext) {
        return delegate().redirect(webContext);
    }

    @Override
    public C getCredentials(WebContext webContext) {
        return delegate().getCredentials(webContext);
    }

    @Override
    public U getUserProfile(C c, WebContext webContext) {
        return delegate().getUserProfile(c, webContext);
    }

    @Override
    public RedirectAction getLogoutAction(WebContext webContext, U u, String s) {
        return delegate().getLogoutAction(webContext, u, s);
    }

}
