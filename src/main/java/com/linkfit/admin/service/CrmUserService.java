package com.linkfit.admin.service;

import com.linkfit.admin.domain.CrmUser;

import java.util.Optional;

public interface CrmUserService {
    Optional<CrmUser> findByBranchCodeAndUsername(String branchCode, String username);
    Optional<CrmUser> findById(String id);
    void updateSecondPassword(String id, String secondPasswordHash);
    void updateLockedCategories(String id, String lockedCategories);
}
