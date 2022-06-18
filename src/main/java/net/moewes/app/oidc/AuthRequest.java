package net.moewes.app.oidc;

import lombok.Builder;
import lombok.Data;

import static net.moewes.app.oidc.AuthRequestsBean.*;
import static net.moewes.app.oidc.AuthRequestsBean.RESPONSE_TYPE;

@Data
@Builder
public class AuthRequest {

    private String scope;

    private String clientId;
    private String redirectUri;
    private String responseType;
    private String state;
    // response_mode
    private String nonce;
    // display
    // prompt
    // max_age -> important
    // ui_locales
    // id_token_hint
    // login_hint
    // acr_values

    public boolean isValid() {
        return state != null && redirectUri != null && clientId != null && responseType != null;
    }
}
