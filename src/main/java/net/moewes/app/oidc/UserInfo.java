package net.moewes.app.oidc;

import lombok.Data;

@Data
public class UserInfo {

    private String sub;
    private String name;
    private String given_name;
    private String family_name;

}
