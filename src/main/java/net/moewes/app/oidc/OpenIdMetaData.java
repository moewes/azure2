package net.moewes.app.oidc;

import lombok.Data;

@Data
public class OpenIdMetaData {

    private String issuer;
    private String authorization_endpoint;
    private String token_endpoint;
    private String userinfo_endpoint;
    private String jwks_uri;
    private String registration_endpoint;
    private String scopes_supported;

}
