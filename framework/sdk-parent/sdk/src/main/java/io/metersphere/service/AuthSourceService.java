package io.metersphere.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.metersphere.base.domain.AuthSource;
import io.metersphere.base.domain.AuthSourceExample;
import io.metersphere.base.mapper.AuthSourceMapper;
import io.metersphere.commons.utils.PageUtils;
import io.metersphere.commons.utils.Pager;
import io.metersphere.request.AuthSourceRequest;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

@Service
public class AuthSourceService {
    @Resource
    private AuthSourceMapper authSourceMapper;

    public List<AuthSource> listAllEnable() {
        AuthSourceExample example = new AuthSourceExample();
        example.createCriteria().andStatusEqualTo("ENABLE");
        return authSourceMapper.selectByExampleWithBLOBs(example);
    }

    public Pager<List<AuthSource>> listAllAuthSource(AuthSourceRequest request, int goPage, int pageSize) {
        Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
        AuthSourceExample example = new AuthSourceExample();
        List<AuthSource> resList = authSourceMapper.selectByExampleWithBLOBs(example);
        return PageUtils.setPageInfo(page, resList);
    }

    public void saveAuthSource(AuthSource record) {
        record.setId(UUID.randomUUID().toString());
        record.setStatus("ENABLE");;
        record.setCreateTime(System.currentTimeMillis());
        record.setUpdateTime(System.currentTimeMillis());
        authSourceMapper.insert(record);
    }

    public void updateAuthSource(String authSourceId, String status) {
        AuthSource record = authSourceMapper.selectByPrimaryKey(authSourceId);
        record.setStatus(status);
        authSourceMapper.updateByPrimaryKeySelective(record);
    }

    public void updateAuthSourceByRecord(AuthSourceRequest record) {
        authSourceMapper.updateByPrimaryKeySelective(record);
    }

    public AuthSource getAuthSource(String id) {
        return authSourceMapper.selectByPrimaryKey(id);
    }

}
