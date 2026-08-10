package com.linkfit.admin;

import com.linkfit.admin.domain.Membership;
import com.linkfit.admin.domain.ReRegistration;
import com.linkfit.admin.mapper.MemberMapper;
import com.linkfit.admin.mapper.ReRegistrationMapper;
import com.linkfit.admin.service.mybatis.MyBatisReRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyBatisReRegistrationServiceTest {

    @Mock ReRegistrationMapper mapper;
    @Mock MemberMapper memberMapper;

    MyBatisReRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new MyBatisReRegistrationService(mapper, memberMapper);
    }

    @Test
    void autoClassify_noExpiringMemberships_returnsZero() {
        when(memberMapper.findExpiringMemberships(30, 0, 500))
                .thenReturn(Collections.emptyList());

        int result = service.autoClassify(1L);

        assertThat(result).isEqualTo(0);
        verify(mapper, never()).insert(any());
    }

    @Test
    void autoClassify_newExpiry_createsRecord() {
        Membership m = new Membership();
        m.setMemberId("member-001");
        when(memberMapper.findExpiringMemberships(30, 0, 500)).thenReturn(List.of(m));
        when(mapper.existsByMemberAndReason("member-001", 1L, "membership_expiry")).thenReturn(false);

        int result = service.autoClassify(1L);

        assertThat(result).isEqualTo(1);
        ArgumentCaptor<ReRegistration> captor = ArgumentCaptor.forClass(ReRegistration.class);
        verify(mapper).insert(captor.capture());
        ReRegistration created = captor.getValue();
        assertThat(created.getMemberId()).isEqualTo("member-001");
        assertThat(created.getGymId()).isEqualTo(1L);
        assertThat(created.getReason()).isEqualTo("membership_expiry");
        assertThat(created.getStatus()).isEqualTo("pending");
        assertThat(created.getId()).isNotNull();
    }

    @Test
    void autoClassify_alreadyExists_skipsInsert() {
        Membership m = new Membership();
        m.setMemberId("member-001");
        when(memberMapper.findExpiringMemberships(30, 0, 500)).thenReturn(List.of(m));
        when(mapper.existsByMemberAndReason("member-001", 1L, "membership_expiry")).thenReturn(true);

        int result = service.autoClassify(1L);

        assertThat(result).isEqualTo(0);
        verify(mapper, never()).insert(any());
    }

    @Test
    void autoClassify_nullMemberId_skips() {
        Membership m = new Membership();
        m.setMemberId(null);
        when(memberMapper.findExpiringMemberships(30, 0, 500)).thenReturn(List.of(m));

        int result = service.autoClassify(1L);

        assertThat(result).isEqualTo(0);
        verify(mapper, never()).insert(any());
    }

    @Test
    void autoClassify_multipleMembers_createsForNewOnly() {
        Membership m1 = new Membership(); m1.setMemberId("mem-001");
        Membership m2 = new Membership(); m2.setMemberId("mem-002");
        when(memberMapper.findExpiringMemberships(30, 0, 500)).thenReturn(List.of(m1, m2));
        when(mapper.existsByMemberAndReason("mem-001", 1L, "membership_expiry")).thenReturn(false);
        when(mapper.existsByMemberAndReason("mem-002", 1L, "membership_expiry")).thenReturn(true);

        int result = service.autoClassify(1L);

        assertThat(result).isEqualTo(1);
        verify(mapper, times(1)).insert(any());
    }

    @Test
    void statusSummary_returnsAllStatuses() {
        when(mapper.countByStatus(1L, "pending")).thenReturn(5);
        when(mapper.countByStatus(1L, "in_progress")).thenReturn(3);
        when(mapper.countByStatus(1L, "success")).thenReturn(10);
        when(mapper.countByStatus(1L, "failed")).thenReturn(1);
        when(mapper.countByStatus(1L, "hold")).thenReturn(2);

        Map<String, Integer> summary = service.statusSummary(1L);

        assertThat(summary).containsEntry("pending", 5)
                           .containsEntry("in_progress", 3)
                           .containsEntry("success", 10)
                           .containsEntry("failed", 1)
                           .containsEntry("hold", 2);
    }

    @Test
    void findAll_delegatesToMapper() {
        when(mapper.findAll(1L, "pending", "membership_expiry", null, null, null, 0, 10))
                .thenReturn(Collections.emptyList());

        service.findAll(1L, "pending", "membership_expiry", null, null, null, 0, 10);

        verify(mapper).findAll(1L, "pending", "membership_expiry", null, null, null, 0, 10);
    }

    @Test
    void updateStatus_delegatesToMapper() {
        service.updateStatus("id-1", "success");
        verify(mapper).updateStatus("id-1", "success", null);
    }

    @Test
    void findById_delegatesToMapper() {
        when(mapper.findById("id-1")).thenReturn(Optional.empty());
        service.findById("id-1");
        verify(mapper).findById("id-1");
    }
}
