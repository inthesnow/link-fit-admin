package com.linkfit.admin.service;

import com.linkfit.admin.domain.Staff;
import java.util.List;
import java.util.Optional;

public interface StaffService {
    List<Staff> findAll(String role, int page, int size);
    long count(String role);
    Optional<Staff> findById(String id);
    Optional<Staff> findAppUserByNameAndPhone(String name, String phone);
    Long findTrainerGymId(String id);
    Staff promoteToTrainer(String id);
    Staff update(String id, Staff staff);
    void revokeTrainer(String id);
    void updateRole(String id, String role);
}
