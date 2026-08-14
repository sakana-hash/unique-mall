package io.github.sakana.user.service.impl;

import io.github.sakana.user.enumeration.UserErrorCode;
import io.github.sakana.user.mapper.AddressMapper;
import io.github.sakana.user.pojo.entity.Address;
import io.github.sakana.user.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public Address getByIdAndUserId(Long id, Long userId) {
        if (id == null || id <= 0) {
            throw UserErrorCode.ADDRESS_ID_INVALID.exception();
        }
        if (userId == null || userId <= 0) {
            throw UserErrorCode.USER_ID_INVALID.exception();
        }

        Address address = addressMapper.selectByIdAndUserId(id, userId);
        if (address == null) {
            throw UserErrorCode.ADDRESS_NOT_FOUND.exception(Map.of(
                    "id", id,
                    "userId", userId
            ));
        }

        return address;
    }
}
