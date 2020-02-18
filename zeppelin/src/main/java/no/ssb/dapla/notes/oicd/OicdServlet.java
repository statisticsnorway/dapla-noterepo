package no.ssb.dapla.notes.oicd;

import org.pac4j.oidc.credentials.OidcCredentials;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple sevlet that exposes the tokens from MouthyOicdClient.
 * <p>
 * Paths:
 * /{id}/access
 * /{id}/token
 */
public class OicdServlet extends HttpServlet {

    private static final Pattern PATH_PATTERN = Pattern.compile("/(\\w+)/(access|refresh)");
    private final MouthyKeycloakOidcClient client;

    public OicdServlet(MouthyKeycloakOidcClient client) {
        this.client = client;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Matcher pathMatcher = PATH_PATTERN.matcher(req.getPathInfo());
        if (!pathMatcher.matches()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String userName = pathMatcher.group(1);
        String type = pathMatcher.group(2);
        OidcCredentials credentials = client.get(userName);
        if (credentials == null) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        if ("access".equals(type)) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().print(credentials.getAccessToken().getValue());
        } else if ("refresh".equals(type)) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().print(credentials.getRefreshToken().getValue());
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
