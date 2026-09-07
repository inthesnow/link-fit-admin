package com.linkfit.admin.service;

import com.linkfit.admin.domain.GymJoinRequest;
import com.linkfit.admin.domain.GymJoinRequestLog;

import java.util.List;
import java.util.Optional;

public interface GymJoinRequestService {
    List<GymJoinRequest> findAll(Long gymId, String status, int page, int size);
    long count(Long gymId, String status);
    long countApprovedAppUsers(Long gymId);
    Optional<GymJoinRequest> findById(Long id);
    List<GymJoinRequestLog> findLogs(String userId, Long gymId);
    void approve(Long id, String actorId);
    void reject(Long id, String actorId, String memo);
}
