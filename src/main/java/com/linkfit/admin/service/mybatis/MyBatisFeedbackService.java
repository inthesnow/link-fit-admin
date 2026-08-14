package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.FeedbackRequest;
import com.linkfit.admin.mapper.FeedbackRequestMapper;
import com.linkfit.admin.service.FeedbackService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MyBatisFeedbackService implements FeedbackService {

    private final FeedbackRequestMapper requestMapper;

    public MyBatisFeedbackService(FeedbackRequestMapper requestMapper) {
        this.requestMapper = requestMapper;
    }

    // ── Sector 10 ─────────────────────────────────────────────

    @Override
    public List<FeedbackRequest> findRequests(Long gymId, String status, String trainerId, int page, int size) {
        return requestMapper.findAll(gymId, status, trainerId, page * size, size);
    }

    @Override
    public long countRequests(Long gymId, String status, String trainerId) {
        return requestMapper.count(gymId, status, trainerId);
    }

    @Override
    public Optional<FeedbackRequest> findRequestById(String id, Long gymId) {
        return requestMapper.findById(id, gymId);
    }

    @Override
    public boolean assignRequestTrainer(String id, String trainerId, Long gymId) {
        return requestMapper.assignTrainer(id, trainerId, gymId) > 0;
    }

    @Override
    public boolean respondToRequest(String id, String response, Long gymId) {
        return requestMapper.respond(id, response, gymId) > 0;
    }

    @Override
    public boolean updateRequestStatus(String id, String status, Long gymId) {
        return requestMapper.updateStatus(id, status, gymId) > 0;
    }
}
