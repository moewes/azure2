package net.moewes.app.oidc;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Jwk {

    private String kty;
    private String e;
    private String use;
    private String kid;
    private String alg;
    private String n;
}
