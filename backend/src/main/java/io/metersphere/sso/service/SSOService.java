package io.metersphere.sso.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.metersphere.base.domain.SystemParameter;
import io.metersphere.commons.constants.ParamConstants;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.service.SystemParameterService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;

@Service
public class SSOService {

    @Resource
    private SystemParameterService service;
    @Resource
    private SystemParameterService systemParameterService;

    // 解密
    private static String Decrypt(String sSrc, String sKey) throws Exception {
        try {
            // 判断Key是否正确
            if (sKey == null) {
                System.out.print("Key为空null");
                return null;
            }
            // 判断Key是否为16位
            if (sKey.length() != 16) {
                System.out.print("Key长度不是16位");
                return null;
            }
            byte[] raw = sKey.getBytes("utf-8");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] encrypted1 = Base64.decodeBase64(sSrc);;//先用base64解密
            try {
                byte[] original = cipher.doFinal(encrypted1);
                String originalString = new String(original,"utf-8");
                return originalString;
            } catch (Exception e) {
                System.out.println(e.toString());
                return null;
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
            return null;
        }
    }

    public JSONArray descAesString(String desString, String cKey) throws Exception {
        String enString = Decrypt(desString, cKey);
        JSONArray userArray = JSONArray.parseArray(enString);
        return userArray;
    }

    private JSONObject accessToken(String url, String clientId, String secretKey, String code, String uc) {
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params= new LinkedMultiValueMap<String, String>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", secretKey);
        params.add("code", code);
        params.add("uc", uc);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(url + "/token/accessToken", HttpMethod.POST, requestEntity, String.class);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            MSException.throwException("调用SSO失败");
        }

        return JSON.parseObject(responseEntity.getBody());
    }

    public String getMappingAttr(String attr, JSONObject jsonContext) {
        return jsonContext.getString(attr);
    }

    public JSONObject authenticate(String code, String uc) {
        String clientId = "";
        String domain = "";
        String secretKey = "";
        JSONObject jsonContext = null;
        try{
            List<SystemParameter> params = systemParameterService.getParamList("sso");

            if (!CollectionUtils.isEmpty(params)) {
                for (SystemParameter param : params) {
                    if (StringUtils.equals(param.getParamKey(), ParamConstants.SSO.URL.getValue())) {
                        domain = param.getParamValue();
                    } else if (StringUtils.equals(param.getParamKey(), ParamConstants.SSO.CLINETID.getValue())) {
                        clientId = param.getParamValue();
                    } else if (StringUtils.equals(param.getParamKey(), ParamConstants.SSO.SECRETKEY.getValue())) {
                        secretKey = param.getParamValue();
                    }
                }
            }

            jsonContext = accessToken(domain, clientId, secretKey, code, uc);

        }catch (Exception e){
        }
        return jsonContext;
    }

    public boolean isOpen() {
        String open = service.getValue(ParamConstants.SSO.OPEN.getValue());
        if (StringUtils.isBlank(open)) {
            return false;
        }
        return StringUtils.equals(Boolean.TRUE.toString(), open);
    }
}
