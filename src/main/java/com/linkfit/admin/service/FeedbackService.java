package com.linkfit.admin.service;

import com.linkfit.admin.domain.FeedbackRequest;

import java.util.List;
import java.util.Optional;

public interface FeedbackService {

    // Sector 10 — requests
    List<FeedbackRequest> findRequests(Long gymId, String status, String trainerId, int page, int size);
    long countRequests(Long gymId, String status, String trainerId);
    Optional<FeedbackRequest> findRequestById(String id, Long gymId);
    boolean assignRequestTrainer(String id, String trainerId, Long gymId);
    boolean respondToRequest(String id, String response, Long gymId);
    boolean updateRequestStatus(String id, String status, Long gymId);
}
