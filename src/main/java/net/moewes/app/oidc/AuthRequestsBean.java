package net.moewes.app.oidc;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthRequestsBean {

    public static final String SCOPE = "scope";
    public static final String CLIENT_ID = "client_id";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String RESPONSE_TYPE = "response_type";
    public static final String STATE = "state";
    public static final String NONCE = "nonce";

    @Inject
    Logger log;

    private final Map<String, AuthSession> sessions = new HashMap<>();

    public AuthRequest extractRequestParameter(UriInfo uriInfo) {

        String redirectURIasString = uriInfo.getQueryParameters().getFirst(REDIRECT_URI);
        if (redirectURIasString == null) redirectURIasString = ""; // TODO
        URI redirectUri = URI.create(redirectURIasString);

        // TODO:
        // display
        // prompt

        // max_age -> important
        // ui_locales
        // id_token_hint
        // login_hint
        // acr_values

        return AuthRequest.builder()
                .scope(uriInfo.getQueryParameters().getFirst(SCOPE))
                .clientId(uriInfo.getQueryParameters().getFirst(CLIENT_ID))
               // .redirectUri(uriInfo.getQueryParameters().getFirst(REDIRECT_URI))
                .redirectUri(redirectUri)
                .responseType(uriInfo.getQueryParameters().getFirst(RESPONSE_TYPE))
                .state(uriInfo.getQueryParameters().getFirst(STATE))
                .nonce(uriInfo.getQueryParameters().getFirst(NONCE))
                .build();
    }

    public String createSession(String username) {

        AuthSession session = new AuthSession();
        session.setUsername(username);
        session.setCode(UUID.randomUUID().toString());
        sessions.put(session.getCode(), session);
        return session.getCode();
    }

    public AuthSession getSession(String code) {
        return sessions.get(code);
    }

    public void logout(AuthSession session) {

        sessions.remove(session.getCode());
    }

    public boolean checkUser(String username, String password) {

        if ("admin".equals(username) && "secret".equals(password)) {
            return true;
        } else {
            return false;
        }
    }

    // TODO getIdToken(String code)
}
