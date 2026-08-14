package io.github.sakana.user.service.impl;

import io.github.sakana.common.exception.BusinessException;
import io.github.sakana.user.mapper.AddressMapper;
import io.github.sakana.user.pojo.entity.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressServiceImpl addressService;

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    @DisplayName("地址ID不合法时返回稳定业务异常")
    void shouldRejectInvalidAddressId(Long id) {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> addressService.getByIdAndUserId(id, 2001L)
        );

        assertBusinessError(
                exception, "USER_ADDRESS_ID_INVALID", "收货地址ID不合法", 400
        );
        verifyNoInteractions(addressMapper);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    @DisplayName("用户ID不合法时返回稳定业务异常")
    void shouldRejectInvalidUserId(Long userId) {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> addressService.getByIdAndUserId(1001L, userId)
        );

        assertBusinessError(exception, "USER_ID_INVALID", "用户ID不合法", 400);
        verifyNoInteractions(addressMapper);
    }

    @Test
    @DisplayName("指定用户的地址不存在时返回404并携带查询上下文")
    void shouldRejectMissingAddress() {
        when(addressMapper.selectByIdAndUserId(1001L, 2001L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> addressService.getByIdAndUserId(1001L, 2001L)
        );

        assertBusinessError(exception, "USER_ADDRESS_NOT_FOUND", "收货地址不存在", 404);
        assertEquals(Map.of("id", 1001L, "userId", 2001L), exception.getDetails());
    }

    @Test
    @DisplayName("地址存在时返回查询结果")
    void shouldReturnAddress() {
        Address address = new Address();
        address.setId(1001L);
        address.setUserId(2001L);
        when(addressMapper.selectByIdAndUserId(1001L, 2001L)).thenReturn(address);

        Address result = addressService.getByIdAndUserId(1001L, 2001L);

        assertSame(address, result);
    }

    private static void assertBusinessError(
            BusinessException exception,
            String expectedCode,
            String expectedMessage,
            int expectedHttpStatus
    ) {
        assertEquals(expectedCode, exception.getCode());
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(expectedHttpStatus, exception.getHttpStatus());
    }
}
