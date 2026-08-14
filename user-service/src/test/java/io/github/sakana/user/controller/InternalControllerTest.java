package io.github.sakana.user.controller;

import io.github.sakana.common.constant.HeadersConstant;
import io.github.sakana.common.exception.BusinessException;
import io.github.sakana.exception.GlobalExceptionHandler;
import io.github.sakana.user.enumeration.UserErrorCode;
import io.github.sakana.user.pojo.entity.Address;
import io.github.sakana.user.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalControllerTest {

    private final StubAddressService addressService = new StubAddressService();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        addressService.failure = null;
        addressService.address = null;

        InternalController controller = new InternalController();
        ReflectionTestUtils.setField(controller, "addressService", addressService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("地址不存在时返回统一的404业务错误")
    void shouldReturnAddressNotFoundError() throws Exception {
        addressService.failure = UserErrorCode.ADDRESS_NOT_FOUND.exception(Map.of(
                "id", 1001L,
                "userId", 2001L
        ));

        mockMvc.perform(get("/internal/user/address/1001")
                        .header(HeadersConstant.USER_ID, "2001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_ADDRESS_NOT_FOUND"))
                .andExpect(jsonPath("$.msg").value("收货地址不存在"))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.userId").value(2001));
    }

    @Test
    @DisplayName("地址查询成功时返回SUCCESS和地址数据")
    void shouldReturnAddress() throws Exception {
        Address address = new Address();
        address.setId(1001L);
        address.setUserId(2001L);
        address.setReceiver("张三");
        address.setPhone("13800000000");
        address.setProvince("浙江省");
        address.setCity("杭州市");
        address.setDistrict("西湖区");
        address.setDetail("文三路1号");
        addressService.address = address;

        mockMvc.perform(get("/internal/user/address/1001")
                        .header(HeadersConstant.USER_ID, "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.receiver").value("张三"))
                .andExpect(jsonPath("$.data.detail").value("文三路1号"));
    }

    private static class StubAddressService implements AddressService {

        private Address address;
        private BusinessException failure;

        @Override
        public Address getByIdAndUserId(Long id, Long userId) {
            if (failure != null) {
                throw failure;
            }
            return address;
        }
    }
}
