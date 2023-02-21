package io.metersphere.request;
import io.metersphere.base.domain.AuthSource;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class AuthSourceRequest extends AuthSource {
    private String name;
    private String description;
    private String type;
    private String configuration;
}
