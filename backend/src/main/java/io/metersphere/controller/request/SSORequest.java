package io.metersphere.controller.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SSORequest {

    private String mode;

    private String code;

    private String uc;

    private String refer;
}
