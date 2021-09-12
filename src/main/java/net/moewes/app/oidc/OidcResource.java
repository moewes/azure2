package net.moewes.app.oidc;

import java.time.ZonedDateTime;
import java.util.Arrays;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import static net.moewes.app.oidc.AuthRequestsBean.CLIENT_ID;
import static net.moewes.app.oidc.AuthRequestsBean.REDIRECT_URI;
import static net.moewes.app.oidc.AuthRequestsBean.RESPONSE_TYPE;
import static net.moewes.app.oidc.AuthRequestsBean.STATE;

@Path("/")
public class OidcResource {

    @ConfigProperty(name = "cloudui.oidc.baseurl")
    String baseUrl;

    @Inject
    Logger log;

    @Inject
    CertBean certBean;

    @Inject
    AuthRequestsBean authRequestsBean;

    @GET
    @Path("/auth")
    public Response authorize(@Context UriInfo uriInfo, @CookieParam("SID") String sid) {

        log.debug("/auth called");

        AuthRequest authorizationRequest =
                authRequestsBean.extractRequestParameter(uriInfo);

        log.debug("Request " + authorizationRequest.toString());

        if (authorizationRequest.getScope() == null ||
                !authorizationRequest.getScope().contains("openid")) {
            return Response.status(400, "Undefined Scope").build();
        }

        UriBuilder locationUriBuilder = UriBuilder.fromPath("/error");

        sid = authRequestsBean.getSession(sid) == null ? null : sid;

        if (sid == null) { // or sid not valid // FIXME
            locationUriBuilder = UriBuilder.fromPath("/login");
            locationUriBuilder
                    .queryParam(STATE, authorizationRequest.getState())
                    .queryParam(REDIRECT_URI, authorizationRequest.getRedirectUri())
                    .queryParam(CLIENT_ID, authorizationRequest.getClientId())
                    .queryParam(RESPONSE_TYPE, authorizationRequest.getResponseType());
        } else {
            locationUriBuilder = UriBuilder.fromPath(authorizationRequest.getRedirectUri());
            if ("code".equals(authorizationRequest.getResponseType())) {
                locationUriBuilder.queryParam("state", authorizationRequest.getState())
                        .queryParam("code", sid);
            } else {
                String s = getIdToken(log, authorizationRequest.getClientId(), sid); // FIXME
                locationUriBuilder.queryParam("state", authorizationRequest.getState())
                        .queryParam("id_token", s)
                        .queryParam("token_type", "bearer")
                        .queryParam("access_token", "1234");
            }
        }

        return Response.status(
                Response.Status.FOUND
        ).location(locationUriBuilder.build()).build();
    }

    @Path("/.well-known/openid-configuration")
    @GET
    public Response getMetaData() {

        log.debug("/.well-known/openid-configuration called");

        OpenIdMetaData meta = new OpenIdMetaData();
        meta.setAuthorization_endpoint(baseUrl + "/auth");
        meta.setIssuer("azure2");
        meta.setToken_endpoint(baseUrl + "/token");
        meta.setUserinfo_endpoint(baseUrl + "/userinfo");
        meta.setJwks_uri(baseUrl + "/jwks");

        return Response.ok().entity(meta).build();
    }

    @Path("/jwks")
    @GET
    public Response jwks(@Context UriInfo uriInfo) {
        log.debug("/jwks called");

        JwksResponse result = new JwksResponse();
        result.setKeys(Arrays.asList(certBean.getJwk()));

        return Response.ok().entity(result).build();
    }

    @Path("/logout")
    @GET
    public Response logout(@Context UriInfo uriInfo, @CookieParam("SID") String sid) {
        log.debug("/logout called");

        AuthSession session = authRequestsBean.getSession(sid);

        if (session != null) {
            authRequestsBean.logout(session);
        }

        return Response.ok().entity("logout").build();
    }

    @Path("/login")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response loginForm(@Context UriInfo uriInfo) {

        StringBuilder sb = new StringBuilder();

        sb.append("<!doctype html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<meta charset=\"utf-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        sb.append("<script src=\"/webjars/cloud-ui-oidc-ui5/0.1.0/index.js\"></script>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<div style=\"height: 100vh;\">");
        sb.append("<ui5-page background-design=\"Solid\">");
        sb.append("<form method=\"post\">");
        sb.append("<ui5-label for=\"fname\">Username</ui5-label>");
        sb.append("<ui5-input type=\"text\" id=\"username\" name=\"username\"></ui5-input><br><br>");
        sb.append("<ui5-label for=\"lname\">Password</ui5-label>");
        sb.append("<ui5-input type=\"text\" id=\"lname\" name=\"lname\"></ui5-input><br><br>");
        sb.append("<ui5-button submits=\"true\" >Login</ui5-button>");
        sb.append("</form>");
        sb.append("</ui5-page>");
        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");

        return Response.ok().entity(sb.toString()).build();
    }

    @Path("/login")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response login(@Context UriInfo uriInfo, MultivaluedMap<String, String> form) {

        log.debug("/login called");

        AuthRequest authRequest =
                authRequestsBean.extractRequestParameter(uriInfo);

        log.debug("Request " + authRequest.toString());

        // Kenne ich den Request
        // -> Nein Bad Request
        // Verify User/Password
        String username = form.getFirst("username");
        // -> Nein Redirect Login Form mit Message
        // Redirect zur Anwendung
        String code = authRequestsBean.createSession(username);

        UriBuilder locationUriBuilder = UriBuilder.fromPath(authRequest.getRedirectUri());
        if ("code".equals(authRequest.getResponseType())) {
            locationUriBuilder.queryParam("state", authRequest.getState())
                    .queryParam("code", code);
        } else {
            String s = getIdToken(log, authRequest.getClientId(), username); // FIXME
            locationUriBuilder.queryParam("state", authRequest.getState())
                    .queryParam("id_token", s)
                    .queryParam("token_type", "bearer")
                    .queryParam("access_token", "1234");
        }
        NewCookie sessionCookie = new NewCookie("SID", code);

        return Response.status(
                Response.Status.FOUND
        ).location(locationUriBuilder.build()).cookie(sessionCookie).build();
    }

    @Path("/revoke")
    @POST
    public Response revoke(@Context UriInfo uriInfo) {
        log.debug("/revoke called");

        return Response.ok().build();
    }

    @Path("/token")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public Response token(@Context UriInfo uriInfo, MultivaluedMap<String, String> form) {

        log.debug("/token called");
        log.debug(form.toString());

        String code = form.getFirst("code");
        String clientId = form.getFirst(CLIENT_ID);

        TokenResponse response = new TokenResponse();
        response.setId_token(getIdToken(log, clientId, code));
        response.setToken_type("bearer");
        response.setAccess_token(getAccessToken(log, clientId, code));
        response.setRefresh_token("RE1234"); // FIXME
        response.setExpires_in(180);

        return Response.ok().entity(response).build();
    }

    @Path("/userinfo")
    @POST
    @GET
    public Response userinfo(@Context UriInfo uriInfo) {

        log.debug("/userinfo called");

        return Response.ok().build();
    }

    private String getIdToken(Logger log, String clientId, String code) {

        AuthSession session = authRequestsBean.getSession(code);
        ZonedDateTime ztime = ZonedDateTime.now();
        ztime.toEpochSecond();
        ZonedDateTime etime = ztime.plusMinutes(3);

        JwtClaimsBuilder builder = Jwt.issuer("azure2")
                .subject(session.getUsername())
                .audience(clientId)
                .expiresAt(etime.toEpochSecond())
                .issuedAt(ztime.toEpochSecond())
                .preferredUserName(session.getUsername());

        String s = builder.jws().keyId(certBean.getKeyId()).sign(certBean.getPrivateKey());

        log.debug(s);
        return s;
    }

    private String getAccessToken(Logger log, String clientId, String code) {

        AuthSession session = authRequestsBean.getSession(code);
        ZonedDateTime ztime = ZonedDateTime.now();
        ztime.toEpochSecond();
        ZonedDateTime etime = ztime.plusMinutes(3);

        JwtClaimsBuilder builder = Jwt.issuer("azure2")
                .subject(session.getUsername())
                .audience(clientId)
                .expiresAt(etime.toEpochSecond())
                .issuedAt(ztime.toEpochSecond());

        String s = builder.sign(certBean.getPrivateKey());

        log.debug(s);
        return s;
    }
}
