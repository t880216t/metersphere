package io.metersphere.sso.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.metersphere.base.domain.SystemParameter;
import io.metersphere.base.domain.User;
import io.metersphere.base.mapper.UserMapper;
import io.metersphere.commons.constants.ParamConstants;
import io.metersphere.commons.constants.UserSource;
import io.metersphere.commons.constants.UserStatus;
import io.metersphere.commons.exception.MSException;
import io.metersphere.controller.ResultHolder;
import io.metersphere.controller.request.LoginRequest;
import io.metersphere.i18n.Translator;
import io.metersphere.service.SystemParameterService;
import io.metersphere.service.UserService;
import io.metersphere.sso.service.SSOService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/sso")
public class SSOController {

    @Resource
    private UserService userService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private SSOService SSOService;
    @Resource
    private SystemParameterService systemParameterService;

    @GetMapping("/signin")
    public ResultHolder login( @RequestParam("code") String code, @RequestParam("uc") String uc, HttpServletResponse response) throws IOException {
        ModelAndView view = new ModelAndView();
        String isOpen = systemParameterService.getValue(ParamConstants.SSO.OPEN.getValue());
        if (StringUtils.isBlank(isOpen) || StringUtils.equals(Boolean.FALSE.toString(), isOpen)) {
            MSException.throwException(Translator.get("sso_authentication_not_enabled"));
        }

        JSONObject jsonContext = SSOService.authenticate(code, uc);
        if(jsonContext == null) {
            return ResultHolder.error(Translator.get("login_fail"));
        }
        JSONObject userinfo = jsonContext.getJSONObject("userinfo");
        String email = SSOService.getMappingAttr("email", userinfo);
        String userId = SSOService.getMappingAttr("username", userinfo);

        SecurityUtils.getSubject().getSession().setAttribute("authenticate", UserSource.SSO.name());
        SecurityUtils.getSubject().getSession().setAttribute("email", email);

        if (StringUtils.isBlank(email)) {
            MSException.throwException(Translator.get("login_fail_email_null"));
        }
        // userId 或 email 有一个相同即为存在本地用户
        User u = userService.selectUser(userId, email);
        String name = SSOService.getMappingAttr("name", userinfo);
        String phone = SSOService.getMappingAttr("phone", userinfo);
        if (u == null) {
            User user = new User();
            user.setId(userId);
            user.setName(name);
            user.setEmail(email);

            if (StringUtils.isNotBlank(phone)) {
                user.setPhone(phone);
            }

            user.setSource(UserSource.SSO.name());
            userService.addSSOUser(user);
        }else if (StringUtils.equals(u.getStatus(), UserStatus.DISABLED)){
            // 更新用户状态
            User user = userService.getUserInfo(userId, true);
            user.setStatus("1");
            userMapper.updateByPrimaryKeySelective(user);
        }else{
            // 更新
            u.setName(name);
            u.setEmail(email);

            if (StringUtils.isNotBlank(phone)) {
                u.setPhone(phone);
            }

            userService.updateUser(u);
        }
        // 执行 SSORealm 中 SSO 登录逻辑
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(userId);
        ResultHolder loginUser = userService.login(loginRequest);
        try {
             response.sendRedirect("/");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loginUser;
    }

    @PostMapping("/signinFail")
    public ResultHolder loginFail( @RequestBody String desString, HttpServletResponse response) throws Exception {
        ModelAndView view = new ModelAndView();
        String isOpen = systemParameterService.getValue(ParamConstants.SSO.OPEN.getValue());
        String cKey = systemParameterService.getValue(ParamConstants.SSO.DESKEY.getValue());
        if (StringUtils.isBlank(isOpen) || StringUtils.equals(Boolean.FALSE.toString(), isOpen)) {
            MSException.throwException(Translator.get("sso_authentication_not_enabled"));
        }

        JSONArray userArray = SSOService.descAesString(desString, cKey);
        if(userArray == null) {
            return ResultHolder.error(Translator.get("login_fail"));
        }
        if(userArray.size()>0){
            for(int i=0;i<userArray.size();i++){
                JSONObject userInfo = userArray.getJSONObject(i);
                String userId = userInfo.getString("account");
                User user = userService.getUserInfo(userId);
                if (user != null){
                    user.setStatus("0");
                    user.setPassword(null);
                    user.setUpdateTime(System.currentTimeMillis());
                    userMapper.updateByPrimaryKeySelective(user);
                    // 踢掉在线用户
                    if (StringUtils.equals(user.getStatus(), UserStatus.DISABLED)) {
                        // SessionUtils.kickOutUser(userId);
                    }
                }else {
                    MSException.throwException(Translator.get("user_not_exist"));
                }
            }
        }
        ResultHolder resultHolder = new ResultHolder();
        return resultHolder;
    }

    private String getSSOLoginURL(){
        List<SystemParameter> params = systemParameterService.getParamList("sso");
        String domain = "";
        String clientId = "";
        if (!CollectionUtils.isEmpty(params)) {
            for (SystemParameter param : params) {
                if (StringUtils.equals(param.getParamKey(), ParamConstants.SSO.URL.getValue())) {
                    domain = param.getParamValue();
                } else if (StringUtils.equals(param.getParamKey(), ParamConstants.SSO.CLINETID.getValue())) {
                    clientId = param.getParamValue();
                }
            }
        }
        String url = domain + "/input?client_id="+ clientId + "&refer=";
        return url;
    }

    private String getSSOLogoutURL(){
        List<SystemParameter> params = systemParameterService.getParamList("sso");
        String domain = "";
        String clientId = "";
        if (!CollectionUtils.isEmpty(params)) {
            for (SystemParameter param : params) {
                if (StringUtils.equals(param.getParamKey(), ParamConstants.SSO.URL.getValue())) {
                    domain = param.getParamValue();
                } else if (StringUtils.equals(param.getParamKey(), ParamConstants.SSO.CLINETID.getValue())) {
                    clientId = param.getParamValue();
                }
            }
        }
        String url = domain + "/input?client_id="+ clientId + "&logout=1";
        return url;
    }

    @GetMapping("/open")
    public ResultHolder isOpen() {
        ResultHolder resultHolder = new ResultHolder();
        HashMap<Object, Object> map = new HashMap<>();
        map.put("openSSO",SSOService.isOpen());
        try{
            String loginUrl = getSSOLoginURL();
            map.put("url",loginUrl);
        }catch (Exception e){
            map.put("url","");
        }
        resultHolder.setData(map);
        return resultHolder;
    }

}
