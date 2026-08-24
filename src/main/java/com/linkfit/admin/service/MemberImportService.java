package com.linkfit.admin.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 타사 CRM에서 이관해오는 회원 정보를 우리 엑셀 템플릿 형식으로 일괄 등록하는 기능.
 * 구독권/인앱 티켓 등 앱 전용 데이터는 다루지 않는다 — 순수 헬스장 정보(기본정보/이용권/
 * 락커/운동복)만 대상.
 */
public interface MemberImportService {
    byte[] generateTemplate();

    /** @return 행별 처리 결과 목록(rowNumber/name/phone/success/messages) */
    List<Map<String, Object>> importExcel(InputStream excel, Long gymId);
}
