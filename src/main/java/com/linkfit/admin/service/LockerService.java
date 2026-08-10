package com.linkfit.admin.service;

import com.linkfit.admin.domain.LockerZone;
import com.linkfit.admin.domain.Membership;

import java.util.List;
import java.util.Map;

public interface LockerService {
    List<LockerZone> listZones(Long gymId);
    LockerZone createZone(Long gymId, String name, int rowsCount, int colsCount, int totalCount);
    void updateZone(Long zoneId, Long gymId, String name, int rowsCount, int colsCount, int totalCount);
    void deleteZone(Long zoneId, Long gymId);
    Map<String, Object> listLockers(Long zoneId, Long gymId);
    void assign(Long lockerId, Long gymId, Membership membership);
}
