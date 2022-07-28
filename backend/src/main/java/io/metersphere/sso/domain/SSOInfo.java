package io.metersphere.sso.domain;

import lombok.Data;

@Data
public class SSOInfo {

    private String url;
    private String clientId;
    private String secretKey;
    private String desKey;
    private String open;

}
