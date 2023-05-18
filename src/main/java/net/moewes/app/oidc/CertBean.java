package net.moewes.app.oidc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;


import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jose4j.base64url.Base64;

@ApplicationScoped
public class CertBean {

    private KeyPair keyPair;
    private final String kid = UUID.randomUUID().toString();

    @PostConstruct
    public void init() {

        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            keyPair = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public Jwk getJwk() {

        RSAPublicKey key = (RSAPublicKey) getPublicKey();

        String encodedModulus = Base64.encode(key.getModulus().toByteArray());
        String encodedExponent = Base64.encode((key.getPublicExponent().toByteArray()));

        return Jwk.builder()
                .alg("RS256")
                .kid(kid)
                .kty("RSA")
                .e(encodedExponent)
                .use("sig")
                .n(encodedModulus)
                .build();
    }

    public String getKeyId() {
        return kid;
    }
}
