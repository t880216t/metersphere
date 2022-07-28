package io.metersphere.sso.domain;

import lombok.Data;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.DnAttribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;

@Data
@Entry(objectClasses = {"person", "top"})
public class Person {

    @Id
    private Name id;
    @DnAttribute(value = "url", index = 0)
    private String url;
    @Attribute(name = "clientId")
    private String clientId;
    @Attribute(name = "secretKey")
    private String secretKey;
    @Attribute(name = "desKey")
    private String desKey;
    @Attribute(name = "open")
    private String open;

}