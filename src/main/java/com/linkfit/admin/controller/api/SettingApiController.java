package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.GymBanner;
import com.linkfit.admin.domain.GymHoliday;
import com.linkfit.admin.domain.GymSetting;
import com.linkfit.admin.mapper.GymBannerMapper;
import com.linkfit.admin.mapper.GymHolidayMapper;
import com.linkfit.admin.mapper.GymSettingMapper;
import com.linkfit.admin.security.CrmUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
public class SettingApiController {

    private static final Logger log = LoggerFactory.getLogger(SettingApiController.class);

    private final GymSettingMapper gymSettingMapper;
    private final GymHolidayMapper gymHolidayMapper;
    private final GymBannerMapper gymBannerMapper;

    @Value("${app.upload.banner-dir:src/main/resources/static/uploads/banners}")
    private String bannerDir;

    public SettingApiController(GymSettingMapper gymSettingMapper,
                                GymHolidayMapper gymHolidayMapper,
                                GymBannerMapper gymBannerMapper) {
        this.gymSettingMapper = gymSettingMapper;
        this.gymHolidayMapper = gymHolidayMapper;
        this.gymBannerMapper  = gymBannerMapper;
    }

    // ── 기본 설정 ────────────────────────────────────────────────

    @GetMapping("/gym")
    public ApiResponse<GymSetting> get(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Setting] GET /api/settings/gym");
        GymSetting setting = gymSettingMapper.find(principal.getGymId());
        if (setting == null) setting = new GymSetting();
        return ApiResponse.ok(setting);
    }

    @PutMapping("/gym")
    public ApiResponse<Void> save(@RequestBody GymSetting setting,
                                   @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Setting] PUT /api/settings/gym");
        gymSettingMapper.upsert(setting, principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/gym/open")
    public ApiResponse<Void> toggleOpen(@RequestBody Map<String, Boolean> body,
                                         @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Setting] PATCH /api/settings/gym/open");
        gymSettingMapper.updateOpenStatus(Boolean.TRUE.equals(body.get("isOpen")), principal.getGymId());
        return ApiResponse.ok();
    }

    // ── 휴일 설정 ────────────────────────────────────────────────

    @GetMapping("/holidays")
    public ApiResponse<List<GymHoliday>> listHolidays(
            @RequestParam(defaultValue = "0") int year,
            @AuthenticationPrincipal CrmUserDetails principal) {
        int y = year > 0 ? year : LocalDate.now().getYear();
        log.info("[Setting] GET /api/settings/holidays - year={}", y);
        return ApiResponse.ok(gymHolidayMapper.findAll(y, principal.getGymId()));
    }

    @PostMapping("/holidays")
    public ApiResponse<GymHoliday> addHoliday(@RequestBody GymHoliday holiday,
                                               @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Setting] POST /api/settings/holidays - date={}, type={}", holiday.getHolidayDate(), holiday.getType());
        gymHolidayMapper.insert(holiday, principal.getGymId());
        return ApiResponse.ok(holiday);
    }

    @DeleteMapping("/holidays/{id}")
    public ApiResponse<Void> deleteHoliday(@PathVariable Long id,
                                            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Setting] DELETE /api/settings/holidays/{}", id);
        gymHolidayMapper.delete(id, principal.getGymId());
        return ApiResponse.ok();
    }

    // ── 배너 관리 ────────────────────────────────────────────────

    @GetMapping("/banners")
    public ApiResponse<List<GymBanner>> listBanners(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Setting] GET /api/settings/banners");
        return ApiResponse.ok(gymBannerMapper.findAll(principal.getGymId()));
    }

    @PostMapping(value = "/banners", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GymBanner> uploadBanner(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", defaultValue = "") String title,
            @AuthenticationPrincipal CrmUserDetails principal) throws IOException {
        log.info("[Setting] POST /api/settings/banners - filename={}", file.getOriginalFilename());

        String ext = getExt(file.getOriginalFilename());
        if (!List.of("jpg", "jpeg", "png", "gif", "webp").contains(ext)) {
            return ApiResponse.error("이미지 파일만 업로드 가능합니다. (jpg/png/gif/webp)");
        }

        Path dir = Paths.get(bannerDir);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + "." + ext;
        Files.copy(file.getInputStream(), dir.resolve(filename));

        GymBanner banner = new GymBanner();
        banner.setImageUrl("/uploads/banners/" + filename);
        banner.setTitle(title);
        gymBannerMapper.insert(banner, principal.getGymId());
        return ApiResponse.ok(banner);
    }

    @DeleteMapping("/banners/{id}")
    public ApiResponse<Void> deleteBanner(@PathVariable Long id,
                                           @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Setting] DELETE /api/settings/banners/{}", id);
        gymBannerMapper.delete(id, principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/banners/{id}/active")
    public ApiResponse<Void> toggleBannerActive(@PathVariable Long id,
                                                 @RequestBody Map<String, Boolean> body,
                                                 @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Setting] PATCH /api/settings/banners/{}/active", id);
        gymBannerMapper.toggleActive(id, Boolean.TRUE.equals(body.get("isActive")), principal.getGymId());
        return ApiResponse.ok();
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
