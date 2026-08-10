package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.CrmUser;
import com.linkfit.admin.mapper.CrmUserMapper;
import com.linkfit.admin.service.CrmUserService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MyBatisCrmUserService implements CrmUserService {

    private final CrmUserMapper crmUserMapper;

    public MyBatisCrmUserService(CrmUserMapper crmUserMapper) {
        this.crmUserMapper = crmUserMapper;
    }

    @Override
    public Optional<CrmUser> findByBranchCodeAndUsername(String branchCode, String username) {
        return crmUserMapper.findByBranchCodeAndUsername(branchCode, username);
    }

    @Override
    public Optional<CrmUser> findById(String id) {
        return crmUserMapper.findById(id);
    }

    @Override
    public void updateSecondPassword(String id, String secondPasswordHash) {
        crmUserMapper.updateSecondPassword(id, secondPasswordHash);
    }

    @Override
    public void updateLockedCategories(String id, String lockedCategories) {
        crmUserMapper.updateLockedCategories(id, lockedCategories);
    }
}
