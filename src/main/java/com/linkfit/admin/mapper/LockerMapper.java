package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.Locker;
import com.linkfit.admin.domain.LockerZone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface LockerMapper {
    List<LockerZone> findZones(@Param("gymId") Long gymId);

    Optional<LockerZone> findZoneById(@Param("id") Long id);

    void insertZone(LockerZone zone);

    void updateZone(@Param("id") Long id, @Param("name") String name, @Param("rowsCount") int rowsCount,
                     @Param("colsCount") int colsCount, @Param("totalCount") int totalCount);

    void deleteZone(@Param("id") Long id);

    int countLockersInZone(@Param("zoneId") Long zoneId);

    int countOccupiedInZone(@Param("zoneId") Long zoneId);

    void insertLocker(@Param("zoneId") Long zoneId, @Param("lockerNumber") int lockerNumber);

    List<Locker> findAllByZone(@Param("zoneId") Long zoneId);

    Optional<Locker> findById(@Param("id") Long id);
}
