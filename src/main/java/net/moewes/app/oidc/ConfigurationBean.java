package net.moewes.app.oidc;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConfigurationBean {

    @ConfigProperty(name = "cloudui.oidc.baseurl")
    String baseUrl;

    @ConfigProperty(name = "cloudui.oidc.dblog",defaultValue = "false")
    boolean dblog;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getIssuer() {
        return baseUrl;
    }

    public  OpenIdMetaData getMetaData() {

        OpenIdMetaData meta = new OpenIdMetaData();
        meta.setAuthorization_endpoint(baseUrl + "/auth");
        meta.setIssuer(baseUrl);
        meta.setToken_endpoint(baseUrl + "/token");
        meta.setUserinfo_endpoint(baseUrl + "/userinfo");
        meta.setJwks_uri(baseUrl + "/jwks");
        meta.setRegistration_endpoint(baseUrl + "/register"); // FIXME points to nowhere
        meta.setScopes_supported("openid");
        return meta;
    }

    public boolean hasDbLog() {
        return dblog;
    }
}
