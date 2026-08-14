package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.CrmCsTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CrmCsTicketMapper {

    List<CrmCsTicket> findAll(@Param("gymId") Long gymId,
                               @Param("status") String status,
                               @Param("category") String category,
                               @Param("offset") int offset, @Param("size") int size);
    long count(@Param("gymId") Long gymId,
               @Param("status") String status,
               @Param("category") String category);

    Optional<CrmCsTicket> findById(@Param("id") String id, @Param("gymId") Long gymId);

    void insert(CrmCsTicket ticket);
    int updateStatus(@Param("id") String id, @Param("status") String status, @Param("gymId") Long gymId);
    int assign(@Param("id") String id, @Param("assignedTo") String assignedTo, @Param("gymId") Long gymId);
    int respond(@Param("id") String id, @Param("response") String response, @Param("gymId") Long gymId);

    int countByStatus(@Param("gymId") Long gymId, @Param("status") String status);
}
