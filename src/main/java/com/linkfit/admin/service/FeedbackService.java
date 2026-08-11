package com.linkfit.admin.service;

import com.linkfit.admin.domain.FeedbackRequest;

import java.util.List;
import java.util.Optional;

public interface FeedbackService {

    // Sector 10 — requests
    List<FeedbackRequest> findRequests(Long gymId, String status, String trainerId, int page, int size);
    long countRequests(Long gymId, String status, String trainerId);
    Optional<FeedbackRequest> findRequestById(String id);
    void assignRequestTrainer(String id, String trainerId);
    void respondToRequest(String id, String response);
    void updateRequestStatus(String id, String status);
}
