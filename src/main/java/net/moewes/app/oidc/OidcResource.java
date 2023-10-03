package net.moewes.app.oidc;

import java.time.ZonedDateTime;
import java.util.List;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import static net.moewes.app.oidc.AuthRequestsBean.CLIENT_ID;
import static net.moewes.app.oidc.AuthRequestsBean.REDIRECT_URI;
import static net.moewes.app.oidc.AuthRequestsBean.RESPONSE_TYPE;
import static net.moewes.app.oidc.AuthRequestsBean.STATE;

@Path("/")
public class OidcResource {

    private static final String MESSAGE = "message";

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

        UriBuilder locationUriBuilder;

        sid = authRequestsBean.getSession(sid) == null ? null : sid;

        if (sid == null) { // or sid not valid // FIXME
            locationUriBuilder = getLoginFormLocationUri(authorizationRequest,null);
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
        result.setKeys(List.of(certBean.getJwk()));

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

        String message = uriInfo.getQueryParameters().getFirst(MESSAGE);
        String loginForm = FormLayout.getLoginForm(message);
        return Response.ok().entity(loginForm).build();
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
        String password = form.getFirst("password");
        if ( !authRequestsBean.checkUser(username,password) ) {
            String message = "username or password is incorrect";
            return Response.status(
                    Response.Status.FOUND
            ).location(getLoginFormLocationUri(authRequest,message).build()).build();
        }
        String code = authRequestsBean.createSession(username);

        String redirectPath;
        if (authRequest.getRedirectUri()!=null) { // Request of external App
            redirectPath = authRequest.getRedirectUri();
        } else {
            redirectPath = "/error";
        }

        UriBuilder locationUriBuilder = UriBuilder.fromPath(redirectPath);
        if ("code".equals(authRequest.getResponseType())) {
            locationUriBuilder.queryParam("state", authRequest.getState())
                    .queryParam("code", code);
        } else {
            String s = getIdToken(log, authRequest.getClientId(), code); // FIXME
            if (authRequest.getState()!=null) {
                locationUriBuilder.queryParam(STATE, authRequest.getState());
            }
            locationUriBuilder
                    .queryParam("id_token", s)
                    .queryParam("token_type", "bearer")
                    .queryParam("access_token", "1234");
        }
        NewCookie.Builder builder = new NewCookie.Builder("SID");
        builder.value(code);
        NewCookie sessionCookie = builder.build();

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
        response.setExpires_in(900);

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
        ZonedDateTime etime = ztime.plusMinutes(15);

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
        ZonedDateTime etime = ztime.plusMinutes(15);

        JwtClaimsBuilder builder = Jwt.issuer("azure2")
                .subject(session.getUsername())
                .audience(clientId)
                .expiresAt(etime.toEpochSecond())
                .issuedAt(ztime.toEpochSecond());

        String s = builder.sign(certBean.getPrivateKey());

        log.debug(s);
        return s;
    }

    private UriBuilder getLoginFormLocationUri(AuthRequest authorizationRequest, String message) {
        UriBuilder locationUriBuilder;
        locationUriBuilder = UriBuilder.fromPath("/login");
        if (authorizationRequest.isValid()) {
            locationUriBuilder
                    .queryParam(STATE, authorizationRequest.getState())
                    .queryParam(REDIRECT_URI, authorizationRequest.getRedirectUri())
                    .queryParam(CLIENT_ID, authorizationRequest.getClientId())
                    .queryParam(RESPONSE_TYPE, authorizationRequest.getResponseType());
        }
        if (message!=null) {
            locationUriBuilder.queryParam(MESSAGE, message);
        }
        return locationUriBuilder;
    }
}
