package net.moewes.app.oidc;

import java.util.List;

import lombok.Data;

@Data
public class JwksResponse {

    private List<Jwk> keys;
}
