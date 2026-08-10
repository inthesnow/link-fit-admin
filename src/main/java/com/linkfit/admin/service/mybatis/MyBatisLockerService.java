package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.Locker;
import com.linkfit.admin.domain.LockerZone;
import com.linkfit.admin.domain.Membership;
import com.linkfit.admin.mapper.LockerMapper;
import com.linkfit.admin.mapper.MemberMapper;
import com.linkfit.admin.service.LockerService;
import com.linkfit.admin.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MyBatisLockerService implements LockerService {

    // 라커는 물리적으로 세로로 쌓을 수 있는 단수가 실제로 크지 않아 최대 10칸으로 고정한다.
    private static final int MAX_VERTICAL = 10;

    private final LockerMapper lockerMapper;
    private final MemberMapper memberMapper;
    private final MemberService memberService;

    public MyBatisLockerService(LockerMapper lockerMapper, MemberMapper memberMapper, MemberService memberService) {
        this.lockerMapper  = lockerMapper;
        this.memberMapper  = memberMapper;
        this.memberService = memberService;
    }

    @Override
    public List<LockerZone> listZones(Long gymId) {
        return lockerMapper.findZones(gymId);
    }

    @Override
    @Transactional
    public LockerZone createZone(Long gymId, String name, int rowsCount, int colsCount, int totalCount) {
        validateName(name);
        validateLayoutNumbers(rowsCount, colsCount, totalCount);
        LockerZone zone = new LockerZone();
        zone.setGymId(gymId);
        zone.setName(name.trim());
        zone.setRowsCount(rowsCount);
        zone.setColsCount(colsCount);
        zone.setTotalCount(totalCount);
        zone.setDisplayOrder(lockerMapper.findZones(gymId).size());
        lockerMapper.insertZone(zone);
        for (int n = 1; n <= totalCount; n++) {
            lockerMapper.insertLocker(zone.getId(), n);
        }
        return zone;
    }

    @Override
    @Transactional
    public void updateZone(Long zoneId, Long gymId, String name, int rowsCount, int colsCount, int totalCount) {
        requireZoneInGym(zoneId, gymId);
        validateName(name);
        validateLayoutNumbers(rowsCount, colsCount, totalCount);
        int existing = lockerMapper.countLockersInZone(zoneId);
        if (totalCount < existing) {
            throw new IllegalArgumentException("이미 생성된 라커(" + existing + "개)보다 적게 설정할 수 없습니다.");
        }
        lockerMapper.updateZone(zoneId, name.trim(), rowsCount, colsCount, totalCount);
        for (int n = existing + 1; n <= totalCount; n++) {
            lockerMapper.insertLocker(zoneId, n);
        }
    }

    @Override
    @Transactional
    public void deleteZone(Long zoneId, Long gymId) {
        requireZoneInGym(zoneId, gymId);
        if (lockerMapper.countOccupiedInZone(zoneId) > 0) {
            throw new IllegalArgumentException("배정된 라커가 있는 구역은 삭제할 수 없습니다. 먼저 배정을 해제해주세요.");
        }
        lockerMapper.deleteZone(zoneId);
    }

    @Override
    public Map<String, Object> listLockers(Long zoneId, Long gymId) {
        LockerZone zone = requireZoneInGym(zoneId, gymId);
        List<Locker> lockers = lockerMapper.findAllByZone(zoneId);
        int total = lockerMapper.countLockersInZone(zoneId);
        int occupied = lockerMapper.countOccupiedInZone(zoneId);
        return Map.of(
                "lockers", lockers,
                "total", total,
                "occupied", occupied,
                "colsCount", zone.getColsCount(),
                "rowsCount", zone.getRowsCount());
    }

    @Override
    @Transactional
    public void assign(Long lockerId, Long gymId, Membership membership) {
        Locker locker = lockerMapper.findById(lockerId)
                .orElseThrow(() -> new IllegalArgumentException("라커를 찾을 수 없습니다."));
        if (!locker.getGymId().equals(gymId)) {
            throw new IllegalArgumentException("라커를 찾을 수 없습니다.");
        }
        if (locker.getMembershipId() != null) {
            throw new IllegalArgumentException("이미 배정된 라커입니다. 먼저 해제해주세요.");
        }
        if (!memberMapper.existsInGym(membership.getMemberId(), gymId)) {
            throw new IllegalArgumentException("대상 회원을 찾을 수 없습니다.");
        }
        membership.setType("LOCKER");
        membership.setLockerId(lockerId);
        memberService.addMembership(membership, gymId);
    }

    private LockerZone requireZoneInGym(Long zoneId, Long gymId) {
        LockerZone zone = lockerMapper.findZoneById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("구역을 찾을 수 없습니다."));
        if (!zone.getGymId().equals(gymId)) {
            throw new IllegalArgumentException("구역을 찾을 수 없습니다.");
        }
        return zone;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("구역 이름을 입력해주세요.");
        }
    }

    private void validateLayoutNumbers(int rowsCount, int colsCount, int totalCount) {
        if (rowsCount <= 0 || colsCount <= 0 || totalCount <= 0) {
            throw new IllegalArgumentException("가로/세로/총 개수는 1 이상이어야 합니다.");
        }
        if (rowsCount > MAX_VERTICAL) {
            throw new IllegalArgumentException("세로는 최대 " + MAX_VERTICAL + "칸까지 설정할 수 있습니다.");
        }
        if (totalCount > rowsCount * colsCount) {
            throw new IllegalArgumentException("총 라커 개수가 가로×세로 칸 수보다 많을 수 없습니다.");
        }
    }
}
