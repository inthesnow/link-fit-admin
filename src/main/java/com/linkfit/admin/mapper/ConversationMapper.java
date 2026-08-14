package com.linkfit.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ConversationMapper {

    // ── 발송 이력 ──────────────────────────────────────────────────────

    /** 공지·이벤트 대화 이력 (발송 배치 단위로 그룹핑) */
    List<Map<String, Object>> findBroadcasts(@Param("category") String category,
                                              @Param("offset") int offset,
                                              @Param("size") int size);

    long countBroadcasts(@Param("category") String category);

    // ── 발송 대상 조회 ─────────────────────────────────────────────────

    /** 활성 회원 user_id 목록 (tier 필터 선택) */
    List<String> findMemberTargets(@Param("gymId") Long gymId, @Param("tier") String tier,
                                   @Param("trainerUserId") String trainerUserId);

    /** 활성 트레이너 user_id 목록 */
    List<String> findTrainerTargets(@Param("gymId") Long gymId);

    /** 활성 트레이너 목록 (발신자 선택용 드롭다운) */
    List<Map<String, Object>> findSenders(@Param("gymId") Long gymId);

    /** 발송 대상 인원 수 미리보기 */
    long countTarget(@Param("gymId") Long gymId, @Param("targetType") String targetType,
                     @Param("tier") String tier,
                     @Param("trainerUserId") String trainerUserId);

    // ── INSERT ─────────────────────────────────────────────────────────

    /** 대화방 생성 (useGeneratedKeys → params.get("id")) */
    void insertConversation(Map<String, Object> params);

    /** 메시지 삽입 */
    void insertChatMessage(@Param("conversationId") long conversationId,
                           @Param("senderId") String senderId,
                           @Param("content") String content);
}
