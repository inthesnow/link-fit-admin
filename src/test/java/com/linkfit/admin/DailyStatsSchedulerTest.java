package com.linkfit.admin;

import com.linkfit.admin.mapper.CrmDailyStatsMapper;
import com.linkfit.admin.scheduler.DailyStatsScheduler;
import com.linkfit.admin.service.ReRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyStatsSchedulerTest {

    @Mock CrmDailyStatsMapper dailyStatsMapper;
    @Mock ReRegistrationService reRegistrationService;

    DailyStatsScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DailyStatsScheduler(dailyStatsMapper, reRegistrationService);
    }

    @Test
    void aggregateDailyStats_noGyms_skips() {
        when(dailyStatsMapper.findAllGymIds()).thenReturn(Collections.emptyList());
        scheduler.aggregateDailyStats();
        verify(dailyStatsMapper, never()).aggregate(any(), any());
    }

    @Test
    void aggregateDailyStats_multipleGyms_aggregatesAll() {
        List<Long> gymIds = List.of(1L, 2L, 3L);
        when(dailyStatsMapper.findAllGymIds()).thenReturn(gymIds);
        String yesterday = LocalDate.now().minusDays(1).toString();

        scheduler.aggregateDailyStats();

        for (Long gymId : gymIds) {
            verify(dailyStatsMapper).aggregate(gymId, yesterday);
        }
    }

    @Test
    void autoClassifyReRegistration_callsServicePerGym() {
        when(dailyStatsMapper.findAllGymIds()).thenReturn(List.of(1L, 2L));
        when(reRegistrationService.autoClassify(anyLong())).thenReturn(0);

        scheduler.autoClassifyReRegistration();

        verify(reRegistrationService).autoClassify(1L);
        verify(reRegistrationService).autoClassify(2L);
    }
}
