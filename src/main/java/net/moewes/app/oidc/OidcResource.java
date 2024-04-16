package net.moewes.app.oidc;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import net.moewes.app.oidc.logging.LogBean;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import static net.moewes.app.oidc.AuthRequestsBean.CLIENT_ID;
import static net.moewes.app.oidc.AuthRequestsBean.REDIRECT_URI;
import static net.moewes.app.oidc.AuthRequestsBean.RESPONSE_TYPE;
import static net.moewes.app.oidc.AuthRequestsBean.STATE;

@Path("/")
public class OidcResource {

    private static final String MESSAGE = "message";

    @Inject
    Logger log;

    @Inject
    LogBean logBean;

    @Inject
    ConfigurationBean configBean;

    @Inject
    CertBean certBean;

    @Inject
    AuthRequestsBean authRequestsBean;

    @GET
    @Path("/auth")
    public Response authorize(@Context UriInfo uriInfo, @CookieParam("SID") String sid) {

        logBean.log("/auth", uriInfo);

        log.debug("/auth called");

        UriBuilder locationUriBuilder;

        AuthRequest authorizationRequest =
                authRequestsBean.extractRequestParameter(uriInfo);

        log.debug("Request " + authorizationRequest.toString());

        if (authorizationRequest.getScope() == null ||
                !authorizationRequest.getScope().contains("openid")) {
            return Response.status(400, "Undefined Scope").build();
        }

        if (authorizationRequest.getResponseType() == null ) {
            locationUriBuilder =
                    UriBuilder.fromUri(authorizationRequest.getRedirectUri()).queryParam("error", "unsupported_response_type");

            if (authorizationRequest.getState()!=null) {
                locationUriBuilder.queryParam("state",authorizationRequest.getState());
            }

            return Response.status(
                    Response.Status.FOUND
            ).location(locationUriBuilder.build()).build();
        }

        authRequestsBean.saveAuthRequest(authorizationRequest);



        sid = authRequestsBean.getSession(sid) == null ? null : sid;

        if (sid == null) { // or sid not valid // FIXME
            locationUriBuilder = getLoginFormLocationUri(authorizationRequest,null).queryParam(
                    "state",authorizationRequest.getState());
        } else {
            AuthSession session = authRequestsBean.getSession(sid);
            session.setNonce(authorizationRequest.getNonce());
            locationUriBuilder = UriBuilder.fromUri(authorizationRequest.getRedirectUri());
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

        logBean.log("/.well-known/openid-configuration",null);
        log.debug("/.well-known/openid-configuration called");
        return Response.ok().entity(configBean.getMetaData()).build();
    }

    @Path("/jwks")
    @GET
    public Response jwks(@Context UriInfo uriInfo) {
        logBean.log("/jwks", uriInfo);
        log.debug("/jwks called");

        JwksResponse result = new JwksResponse();
        result.setKeys(List.of(certBean.getJwk()));

        return Response.ok().entity(result).build();
    }

    @Path("/logout")
    @GET
    public Response logout(@Context UriInfo uriInfo, @CookieParam("SID") String sid) {
        logBean.log("/logout",uriInfo);
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

        logBean.log("GET /login",uriInfo);

        String message = uriInfo.getQueryParameters().getFirst(MESSAGE);
        String loginForm = FormLayout.getLoginForm(message);
        return Response.ok().entity(loginForm).build();
    }

    @Path("/login")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response login(@Context UriInfo uriInfo, MultivaluedMap<String, String> form) {

        logBean.log("POST /login", uriInfo);
        log.debug("/login called");

        AuthRequest authRequest =
                authRequestsBean.extractRequestParameter(uriInfo);

        authRequest = authRequestsBean.getRequestByState(authRequest.getState());
        // FIXME when es nicht gefunden wird

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
        String code = authRequestsBean.createSession(username,authRequest);

        String redirectPath;
        if (authRequest.getRedirectUri()!=null) { // Request of external App
            redirectPath = authRequest.getRedirectUri().toASCIIString();
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

        logBean.log("/revoke",uriInfo);
        log.debug("/revoke called");

        return Response.ok().build();
    }

    @Path("/token")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public Response token(@Context UriInfo uriInfo, MultivaluedMap<String, String> form) {

        logBean.log("/token",uriInfo);
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

        logBean.log("/userinfo",uriInfo);
        log.debug("/userinfo called");

        JwtClaimsBuilder builder = Jwt.subject("admin").claim(Claims.full_name,"the admin");

        Map<String,String> result = new HashMap<>();

        result.put(Claims.sub.name(),"admin");
        result.put(Claims.full_name.name(), "the admin");

        return Response.ok(result).build();
    }

    private String getIdToken(Logger log, String clientId, String code) {

        AuthSession session = authRequestsBean.getSession(code);
        ZonedDateTime ztime = ZonedDateTime.now();
        ztime.toEpochSecond();
        ZonedDateTime etime = ztime.plusMinutes(15);

        if (clientId==null) {
            clientId = "frontend"; // FIXME
        }

        JwtClaimsBuilder builder = Jwt.issuer(configBean.getIssuer())
                .subject(session.getUsername())
                .audience(clientId)
                .expiresAt(etime.toEpochSecond())
                .issuedAt(ztime.toEpochSecond())
                .preferredUserName(session.getUsername());

        if (session.getNonce()!=null) {
            builder = builder.claim(Claims.nonce, session.getNonce());
        }
        else {
            builder = builder.claim(Claims.nonce, "ID_T");
        }

        String s = builder.jws().keyId(certBean.getKeyId()).sign(certBean.getPrivateKey());

        log.debug(s);
        return s;
    }

    private String getAccessToken(Logger log, String clientId, String code) {

        AuthSession session = authRequestsBean.getSession(code);
        ZonedDateTime ztime = ZonedDateTime.now();
        ztime.toEpochSecond();
        ZonedDateTime etime = ztime.plusMinutes(15);
        if (clientId==null) {
            clientId = "frontend"; // FIXME
        }

        JwtClaimsBuilder builder = Jwt.issuer(configBean.getIssuer())
                .subject(session.getUsername())
                .audience(clientId)
                .expiresAt(etime.toEpochSecond())
                .issuedAt(ztime.toEpochSecond());

        if (session.getNonce()!=null) {
            builder = builder.claim(Claims.nonce, session.getNonce());
        }
            else {
                builder = builder.claim(Claims.nonce, "ACC_T");
            }

        String s = builder.sign(certBean.getPrivateKey());

        log.debug(s);
        return s;
    }

    private UriBuilder getLoginFormLocationUri(AuthRequest authorizationRequest, String message) {
        UriBuilder locationUriBuilder;

        Logger.getLogger("xxx").info("baseUrl : " + configBean.getBaseUrl());

        locationUriBuilder = UriBuilder.fromPath(configBean.getBaseUrl() + "/login");
    //    locationUriBuilder.scheme("https"); // TODO
    //    locationUriBuilder.host("cloudui-oidc.azurewebsites.net"); // TODO
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
