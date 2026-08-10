package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.LockerZone;
import com.linkfit.admin.domain.Membership;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.LockerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lockers")
public class LockerApiController {

    private static final Logger log = LoggerFactory.getLogger(LockerApiController.class);

    private final LockerService lockerService;

    public LockerApiController(LockerService lockerService) {
        this.lockerService = lockerService;
    }

    @GetMapping("/zones")
    public ApiResponse<List<LockerZone>> listZones(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Locker] GET /api/lockers/zones");
        return ApiResponse.ok(lockerService.listZones(principal.getGymId()));
    }

    @PostMapping("/zones")
    public ApiResponse<LockerZone> createZone(@RequestBody Map<String, Object> body,
                                               @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Locker] POST /api/lockers/zones - body={}", body);
        try {
            LockerZone zone = lockerService.createZone(principal.getGymId(),
                    (String) body.get("name"),
                    intOf(body, "rowsCount"), intOf(body, "colsCount"), intOf(body, "totalCount"));
            return ApiResponse.ok(zone);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 구역 이름/배치 수정. 총 개수를 늘리는 것(라커 추가)은 되지만, 이미 생성된 라커 수보다
    // 작게 줄이는 것은 허용하지 않는다(서비스 계층에서 검증) — 배정된 라커가 사라지는 것을 방지.
    @PutMapping("/zones/{id}")
    public ApiResponse<Void> updateZone(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                         @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Locker] PUT /api/lockers/zones/{} - body={}", id, body);
        try {
            lockerService.updateZone(id, principal.getGymId(),
                    (String) body.get("name"),
                    intOf(body, "rowsCount"), intOf(body, "colsCount"), intOf(body, "totalCount"));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
        return ApiResponse.ok();
    }

    @DeleteMapping("/zones/{id}")
    public ApiResponse<Void> deleteZone(@PathVariable Long id, @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Locker] DELETE /api/lockers/zones/{}", id);
        try {
            lockerService.deleteZone(id, principal.getGymId());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam Long zoneId,
                                                  @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Locker] GET /api/lockers - zoneId={}", zoneId);
        try {
            return ApiResponse.ok(lockerService.listLockers(zoneId, principal.getGymId()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 라커 배정 — 기존 "개별 상품 부여"(POST /api/members/{id}/memberships)와 동일한 필드를
    // 받아 그대로 membership(type=LOCKER) 레코드를 생성한다. 해제는 기존
    // DELETE /api/memberships/{id}를 그대로 재사용(별도 엔드포인트 없음).
    @PostMapping("/{id}/assign")
    public ApiResponse<Void> assign(@PathVariable Long id, @RequestBody Membership membership,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Locker] POST /api/lockers/{}/assign - memberId={}", id, membership.getMemberId());
        if (membership.getMemberId() == null || membership.getMemberId().isBlank()) {
            return ApiResponse.error("배정할 회원을 선택해주세요.");
        }
        if (membership.getStartDate() == null) {
            return ApiResponse.error("시작일을 입력해주세요.");
        }
        try {
            lockerService.assign(id, principal.getGymId(), membership);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
        return ApiResponse.ok();
    }

    private static int intOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }
}
