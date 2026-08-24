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

    // 회원 일괄 등록(엑셀 임포트) 시 락커번호만으로 배정 대상 라커를 찾기 위함.
    // 여러 구역에 같은 번호가 있을 수 있어 List로 받고, 서비스단에서 미배정(membershipId=null)인
    // 것 중 하나를 고른다.
    List<Locker> findAvailableByGymAndNumber(@Param("gymId") Long gymId, @Param("lockerNumber") int lockerNumber);
}
