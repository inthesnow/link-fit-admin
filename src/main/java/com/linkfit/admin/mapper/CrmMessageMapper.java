package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.CrmMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CrmMessageMapper {

    List<CrmMessage> findReceived(@Param("gymId") Long gymId, @Param("receiverId") String receiverId,
                                   @Param("offset") int offset, @Param("size") int size);
    List<CrmMessage> findSent(@Param("gymId") Long gymId, @Param("senderId") String senderId,
                               @Param("offset") int offset, @Param("size") int size);
    List<CrmMessage> findNotices(@Param("gymId") Long gymId,
                                  @Param("offset") int offset, @Param("size") int size);

    long countReceived(@Param("gymId") Long gymId, @Param("receiverId") String receiverId);
    long countSent(@Param("gymId") Long gymId, @Param("senderId") String senderId);
    long countNotices(@Param("gymId") Long gymId);
    long countUnread(@Param("gymId") Long gymId, @Param("receiverId") String receiverId);

    Optional<CrmMessage> findById(@Param("id") String id, @Param("gymId") Long gymId);
    void insert(CrmMessage message);
    int markRead(@Param("id") String id, @Param("gymId") Long gymId);
    void markAllRead(@Param("gymId") Long gymId, @Param("receiverId") String receiverId);
    int delete(@Param("id") String id, @Param("gymId") Long gymId);
}
