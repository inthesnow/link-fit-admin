package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.CrmMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface CrmMessageMapper {

    List<CrmMessage> findSent(@Param("gymId") Long gymId, @Param("senderId") String senderId,
                               @Param("offset") int offset, @Param("size") int size);
    List<CrmMessage> findNotices(@Param("gymId") Long gymId,
                                  @Param("offset") int offset, @Param("size") int size);

    long countSent(@Param("gymId") Long gymId, @Param("senderId") String senderId);
    long countNotices(@Param("gymId") Long gymId);

    Optional<CrmMessage> findById(@Param("id") String id, @Param("gymId") Long gymId);
    void insert(CrmMessage message);
    int markRead(@Param("id") String id, @Param("gymId") Long gymId);
    int delete(@Param("id") String id, @Param("gymId") Long gymId);

    // "읽지않은 쪽지함" — 특정 관리자 개인이 아니라 이 지점 전체 기준으로, 회원이 보낸 미확인
    // 쪽지를 회원별로 묶어서 보여준다(메신저의 대화목록처럼). 회원 답장은 특정 관리자 앞으로
    // 오는 게 아니라 receiver_id='GYM'(지점 전체) 고정값으로 오기 때문에, 예전처럼 로그인한
    // 관리자 개인의 receiver_id로 필터링하면 회원 답장이 전혀 안 보이던 문제를 고침.
    List<Map<String, Object>> findUnreadMemberThreads(@Param("gymId") Long gymId);
    long countUnreadMemberMessages(@Param("gymId") Long gymId);
    void markAllMemberMessagesRead(@Param("gymId") Long gymId);

    // 회원 상세 화면의 "쪽지" 탭 — 관리자<->이 회원 간 전체 대화(양방향)
    List<CrmMessage> findThreadWithMember(@Param("gymId") Long gymId, @Param("memberId") String memberId);
    int markMemberMessagesRead(@Param("gymId") Long gymId, @Param("memberId") String memberId);

    // 단체쪽지 발송 대상 — gender('남자'/'여자'/null=전체), validity('valid'/'expired'/null=전체)
    List<String> findBroadcastTargets(@Param("gymId") Long gymId, @Param("gender") String gender,
                                       @Param("validity") String validity);
    long countBroadcastTargets(@Param("gymId") Long gymId, @Param("gender") String gender,
                                @Param("validity") String validity);
}
