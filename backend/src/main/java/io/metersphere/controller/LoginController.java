package io.metersphere.controller;

import io.metersphere.commons.constants.OperLogConstants;
import io.metersphere.commons.constants.OperLogModule;
import io.metersphere.commons.constants.UserSource;
import io.metersphere.commons.user.SessionUser;
import io.metersphere.commons.utils.RsaKey;
import io.metersphere.commons.utils.RsaUtil;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.controller.request.LoginRequest;
import io.metersphere.dto.UserDTO;
import io.metersphere.i18n.Translator;
import io.metersphere.log.annotation.MsAuditLog;
import io.metersphere.service.BaseDisplayService;
import io.metersphere.service.UserService;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;


import io.metersphere.base.domain.SystemParameter;
import io.metersphere.commons.constants.ParamConstants;
import io.metersphere.service.SystemParameterService;
import org.apache.commons.collections.CollectionUtils;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping
public class LoginController {

    @Resource
    private UserService userService;
    @Resource
    private BaseDisplayService baseDisplayService;
    @Resource
    private SystemParameterService systemParameterService;

    @GetMapping(value = "/isLogin")
    public ResultHolder isLogin() throws NoSuchAlgorithmException {
        RsaKey rsaKey = RsaUtil.getRsaKey();
        if (SecurityUtils.getSubject().isAuthenticated()) {
            UserDTO user = userService.getUserDTO(SessionUtils.getUserId());
            if (user == null) {
                return ResultHolder.error(rsaKey.getPublicKey());
            }
            if (StringUtils.isBlank(user.getLanguage())) {
                user.setLanguage(LocaleContextHolder.getLocale().toString());
            }
            userService.autoSwitch(user);
            SessionUser sessionUser = SessionUser.fromUser(user);
            SessionUtils.putUser(sessionUser);
            return ResultHolder.success(sessionUser);
        }
        return ResultHolder.error(rsaKey.getPublicKey());
    }

    @PostMapping(value = "/signin")
    @MsAuditLog(module = OperLogModule.AUTH_TITLE, type = OperLogConstants.LOGIN, title = "登录")
    public ResultHolder login(@RequestBody LoginRequest request) {
        SessionUser sessionUser = SessionUtils.getUser();
        if (sessionUser != null) {
            if (!StringUtils.equals(sessionUser.getId(), request.getUsername())) {
                return ResultHolder.error(Translator.get("please_logout_current_user"));
            }
        }
        SecurityUtils.getSubject().getSession().setAttribute("authenticate", UserSource.LOCAL.name());
        ResultHolder result = userService.login(request);
        // 登录是否提示修改密码
        boolean changePassword = userService.checkWhetherChangePasswordOrNot(request);
        result.setMessage(BooleanUtils.toStringTrueFalse(changePassword));
        return result;
    }

    @GetMapping(value = "/currentUser")
    public ResultHolder currentUser() {
        return ResultHolder.success(SecurityUtils.getSubject().getSession().getAttribute("user"));
    }

    @GetMapping(value = "/signout")
    @MsAuditLog(module = OperLogModule.AUTH_TITLE, beforeEvent = "#msClass.getUserId(id)", type = OperLogConstants.LOGIN, title = "登出", msClass = SessionUtils.class)
    public ResultHolder logout() throws Exception {
        userService.logout();
        String clientId = "";
        String domain = "";
        String open = "";
        String result = "";
        ResultHolder resultHolder = new ResultHolder();
        HashMap<Object, Object> map = new HashMap<>();
        try{
            List<SystemParameter> params = systemParameterService.getParamList("sso");

            if (!CollectionUtils.isEmpty(params)) {
                for (SystemParameter param : params) {
                    if (StringUtils.equals(param.getParamKey(), ParamConstants.SSO.URL.getValue())) {
                        domain = param.getParamValue();
                    } else if (StringUtils.equals(param.getParamKey(), ParamConstants.SSO.CLINETID.getValue())) {
                        clientId = param.getParamValue();
                    } else if (StringUtils.equals(param.getParamKey(), ParamConstants.SSO.OPEN.getValue())) {
                        open = param.getParamValue();;
                    }
                }
            }
            if (StringUtils.equals(Boolean.TRUE.toString(), open)){
                map.put("openSSO",open);
                result = domain + "/input?client_id="+clientId+"&logout=1";
                map.put("url",result);
            }
        }catch (Exception e){
        }
        SecurityUtils.getSubject().logout();
        resultHolder.setData(map);
        return resultHolder;
    }

    /*Get default language*/
    @GetMapping(value = "/language")
    public String getDefaultLanguage() {
        return userService.getDefaultLanguage();
    }

    @GetMapping("display/file/{imageName}")
    public ResponseEntity<byte[]> image(@PathVariable("imageName") String imageName) throws IOException {
        return baseDisplayService.getImage(imageName);
    }
}
