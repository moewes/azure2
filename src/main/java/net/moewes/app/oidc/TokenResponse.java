package net.moewes.app.oidc;

import lombok.Data;

@Data
public class TokenResponse {

    private String access_token;
    private String token_type;
    private String refresh_token;
    private int expires_in;
    private String id_token;

}
