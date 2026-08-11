package com.linkfit.admin.scheduler;

import com.linkfit.admin.mapper.CrmDailyStatsMapper;
import com.linkfit.admin.service.ReRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DailyStatsScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyStatsScheduler.class);

    private final CrmDailyStatsMapper dailyStatsMapper;
    private final ReRegistrationService reRegistrationService;

    public DailyStatsScheduler(CrmDailyStatsMapper dailyStatsMapper,
                                 ReRegistrationService reRegistrationService) {
        this.dailyStatsMapper = dailyStatsMapper;
        this.reRegistrationService = reRegistrationService;
    }

    /** 매일 01:00 — 전날 통계 집계 */
    @Scheduled(cron = "0 0 1 * * *")
    public void aggregateDailyStats() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        List<Long> gymIds = dailyStatsMapper.findAllGymIds();
        if (gymIds.isEmpty()) { log.info("[Stats] 등록된 gym 없음, 건너뜀"); return; }
        for (Long gymId : gymIds) {
            try {
                dailyStatsMapper.aggregate(gymId, yesterday);
                log.info("[Stats] gymId={} date={} 집계 완료", gymId, yesterday);
            } catch (Exception e) {
                log.error("[Stats] gymId={} 집계 실패: {}", gymId, e.getMessage());
            }
        }
    }

    /** 매일 06:00 — 재등록 자동 분류 */
    @Scheduled(cron = "0 0 6 * * *")
    public void autoClassifyReRegistration() {
        List<Long> gymIds = dailyStatsMapper.findAllGymIds();
        for (Long gymId : gymIds) {
            try {
                int created = reRegistrationService.autoClassify(gymId);
                if (created > 0) log.info("[ReReg] gymId={} 자동 분류 {}건", gymId, created);
            } catch (Exception e) {
                log.error("[ReReg] gymId={} 자동 분류 실패: {}", gymId, e.getMessage());
            }
        }
    }

    /** 수동 트리거 — 오늘 날짜 기준 통계 즉시 집계 */
    public void aggregateToday(Long gymId) {
        String today = LocalDate.now().toString();
        dailyStatsMapper.aggregate(gymId, today);
        log.info("[Stats] 수동 집계 gymId={} date={}", gymId, today);
    }
}
