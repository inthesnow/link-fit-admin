package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.GymJoinRequest;
import com.linkfit.admin.domain.GymJoinRequestLog;
import com.linkfit.admin.mapper.GymJoinRequestMapper;
import com.linkfit.admin.service.GymJoinRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MyBatisGymJoinRequestService implements GymJoinRequestService {

    private final GymJoinRequestMapper mapper;

    public MyBatisGymJoinRequestService(GymJoinRequestMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<GymJoinRequest> findAll(Long gymId, String status, int page, int size) {
        return mapper.findAll(gymId, status, page * size, size);
    }

    @Override
    public long count(Long gymId, String status) {
        return mapper.count(gymId, status);
    }

    @Override
    public long countApprovedAppUsers(Long gymId) {
        return mapper.countApprovedAppUsers(gymId);
    }

    @Override
    public Optional<GymJoinRequest> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public List<GymJoinRequestLog> findLogs(String userId, Long gymId) {
        return mapper.findLogs(userId, gymId);
    }

    @Override
    @Transactional
    public void approve(Long id, String actorId) {
        GymJoinRequest req = mapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));
        mapper.approve(id);
        mapper.insertLog(req.getMemberId(), req.getGymId(), "APPROVED", null, actorId);
    }

    @Override
    @Transactional
    public void reject(Long id, String actorId, String memo) {
        GymJoinRequest req = mapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));
        mapper.reject(id);
        mapper.insertLog(req.getMemberId(), req.getGymId(), "REJECTED", memo, actorId);
    }
}
