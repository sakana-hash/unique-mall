package io.github.sakana.user.service.impl;

import io.github.sakana.user.mapper.AddressMapper;
import io.github.sakana.user.pojo.entity.Address;
import io.github.sakana.user.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public Address getByIdAndUserId(Long id, Long userId) {
        if (id == null || id <= 0) {
            throw new RuntimeException("不合法的地址id");
        }
        if (userId == null || userId <= 0) {
            throw new RuntimeException("不合法的用户id");
        }

        Address address = addressMapper.selectByIdAndUserId(id, userId);
        if (address == null) {
            throw new RuntimeException("地址id不存在, id:" +  id + ", userId:" + userId);
        }

        return address;
    }
}
