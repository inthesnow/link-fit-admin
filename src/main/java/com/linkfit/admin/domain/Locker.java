package com.linkfit.admin.domain;

import java.time.LocalDate;

public class Locker {
    private Long id;
    private Long zoneId;
    private int lockerNumber;
    // 조회 시 locker_zone과 조인해서 채워짐 — 소속 헬스장 검증용
    private Long gymId;
    // 현재 배정 정보 (조회 시 membership/user_profiles와 조인해서 채워짐, 비어있으면 null)
    private Long membershipId;
    private String memberId;
    private String memberName;
    private LocalDate startDate;
    private LocalDate endDate;

    public Locker() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public int getLockerNumber() { return lockerNumber; }
    public void setLockerNumber(int lockerNumber) { this.lockerNumber = lockerNumber; }
    public Long getGymId() { return gymId; }
    public void setGymId(Long gymId) { this.gymId = gymId; }
    public Long getMembershipId() { return membershipId; }
    public void setMembershipId(Long membershipId) { this.membershipId = membershipId; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
