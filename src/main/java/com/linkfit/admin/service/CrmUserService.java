package com.linkfit.admin.service;

import com.linkfit.admin.domain.CrmUser;

import java.util.Optional;

public interface CrmUserService {
    Optional<CrmUser> findByBranchCodeAndUsername(String branchCode, String username);
    Optional<CrmUser> findById(String id);
    void updateSecondPassword(String id, String secondPasswordHash);
    void updatePassword(String id, String passwordHash);
    void updateLockedCategories(String id, String lockedCategories);
    void completeFirstLogin(String id, String passwordHash, String secondPasswordHash);
}
