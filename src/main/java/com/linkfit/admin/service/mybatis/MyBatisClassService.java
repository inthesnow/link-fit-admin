package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.ClassSession;
import com.linkfit.admin.mapper.ClassMapper;
import com.linkfit.admin.service.ClassService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MyBatisClassService implements ClassService {

    private final ClassMapper classMapper;

    public MyBatisClassService(ClassMapper classMapper) {
        this.classMapper = classMapper;
    }

    @Override
    public List<ClassSession> findAll(String type, String date, int page, int size, Long gymId) {
        return classMapper.findAll(type, date, page * size, size, gymId);
    }

    @Override
    public long count(String type, String date, Long gymId) {
        return classMapper.count(type, date, gymId);
    }

    @Override
    public Optional<ClassSession> findById(Long id, Long gymId) {
        return classMapper.findById(id, gymId);
    }

    @Override
    public ClassSession save(ClassSession session, Long gymId) {
        classMapper.insert(session, gymId);
        return session;
    }

    @Override
    public ClassSession update(Long id, ClassSession session, Long gymId) {
        session.setId(id);
        classMapper.update(session, gymId);
        return session;
    }

    @Override
    public void cancel(Long id, Long gymId) {
        classMapper.cancel(id, gymId);
    }

    @Override
    public void enroll(Long classId, String memberId, Long gymId) {
        // class_attendee 자체엔 gym_id가 없어 INSERT에 WHERE 조건을 걸 수 없다 —
        // 먼저 이 수업이 정말 이 지점 소속인지 확인한 뒤에만 등록을 진행한다.
        if (classMapper.findById(classId, gymId).isEmpty()) {
            throw new IllegalArgumentException("수업을 찾을 수 없습니다.");
        }
        classMapper.enroll(classId, memberId);
        classMapper.incrementEnrolled(classId, gymId);
    }

    @Override
    public void cancelEnrollment(Long classId, String memberId, Long gymId) {
        if (classMapper.findById(classId, gymId).isEmpty()) {
            throw new IllegalArgumentException("수업을 찾을 수 없습니다.");
        }
        classMapper.cancelEnrollment(classId, memberId);
        classMapper.decrementEnrolled(classId, gymId);
    }
}
