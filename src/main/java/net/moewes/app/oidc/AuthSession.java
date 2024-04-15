package net.moewes.app.oidc;

import lombok.Data;

@Data
public class AuthSession {

    private String code;
    private String username;
    private String nonce;
}
