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
    public Optional<FeedbackRequest> findRequestById(String id) {
        return requestMapper.findById(id);
    }

    @Override
    public void assignRequestTrainer(String id, String trainerId) {
        requestMapper.assignTrainer(id, trainerId);
    }

    @Override
    public void respondToRequest(String id, String response) {
        requestMapper.respond(id, response);
    }

    @Override
    public void updateRequestStatus(String id, String status) {
        requestMapper.updateStatus(id, status);
    }
}
