package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.ClassAttendee;
import com.linkfit.admin.domain.ClassSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ClassMapper {
    List<ClassSession> findAll(@Param("type") String type, @Param("date") String date,
                               @Param("offset") int offset, @Param("size") int size,
                               @Param("gymId") Long gymId);
    long count(@Param("type") String type, @Param("date") String date, @Param("gymId") Long gymId);
    Optional<ClassSession> findById(@Param("id") Long id, @Param("gymId") Long gymId);
    void insert(@Param("session") ClassSession session, @Param("gymId") Long gymId);
    void update(@Param("session") ClassSession session, @Param("gymId") Long gymId);
    void cancel(@Param("id") Long id, @Param("gymId") Long gymId);
    void enroll(@Param("classId") Long classId, @Param("memberId") String memberId);
    void incrementEnrolled(@Param("id") Long id, @Param("gymId") Long gymId);
    void cancelEnrollment(@Param("classId") Long classId, @Param("memberId") String memberId);
    void decrementEnrolled(@Param("id") Long id, @Param("gymId") Long gymId);
    List<ClassAttendee> findAttendees(@Param("classId") Long classId, @Param("gymId") Long gymId);
}
