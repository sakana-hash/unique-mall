package io.github.sakana.user.service;

import io.github.sakana.user.pojo.entity.Address;

public interface AddressService {

    Address getByIdAndUserId(Long id,  Long userId);
}
