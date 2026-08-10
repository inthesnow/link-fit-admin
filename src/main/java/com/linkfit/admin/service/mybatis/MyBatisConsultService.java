package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.Consult;
import com.linkfit.admin.mapper.ConsultMapper;
import com.linkfit.admin.service.ConsultService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MyBatisConsultService implements ConsultService {

    private final ConsultMapper consultMapper;

    public MyBatisConsultService(ConsultMapper consultMapper) {
        this.consultMapper = consultMapper;
    }

    @Override
    public List<Consult> findAll(String type, int page, int size) {
        return consultMapper.findAll(type, page * size, size);
    }

    @Override
    public long count(String type) {
        return consultMapper.count(type);
    }

    @Override
    public Optional<Consult> findById(Long id) {
        return consultMapper.findById(id);
    }

    @Override
    public Consult saveNew(Consult consult) {
        consult.setType("NEW");
        consult.setPhone(stripNonDigits(consult.getPhone()));
        consultMapper.insert(consult);
        return consult;
    }

    @Override
    public Consult saveExisting(Consult consult) {
        consult.setType("EXISTING");
        consult.setPhone(stripNonDigits(consult.getPhone()));
        consultMapper.insert(consult);
        return consult;
    }

    @Override
    public Consult update(Long id, Consult consult) {
        consult.setId(id);
        consult.setPhone(stripNonDigits(consult.getPhone()));
        consultMapper.update(consult);
        return consult;
    }

    private static String stripNonDigits(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    @Override
    public void delete(Long id) {
        consultMapper.delete(id);
    }
}
