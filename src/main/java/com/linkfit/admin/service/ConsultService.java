package com.linkfit.admin.service;

import com.linkfit.admin.domain.Consult;
import java.util.List;
import java.util.Optional;

public interface ConsultService {
    List<Consult> findAll(String type, int page, int size, Long gymId);
    long count(String type, Long gymId);
    Optional<Consult> findById(Long id, Long gymId);
    Consult saveNew(Consult consult, Long gymId);
    Consult saveExisting(Consult consult, Long gymId);
    Consult update(Long id, Consult consult, Long gymId);
    void delete(Long id, Long gymId);
}
