package io.github.sakana.user.controller;

import io.github.sakana.common.constant.HeadersConstant;
import io.github.sakana.common.result.Result;
import io.github.sakana.user.pojo.dto.LoginDTO;
import io.github.sakana.user.pojo.dto.RegisterDTO;
import io.github.sakana.user.pojo.vo.LoginVO;
import io.github.sakana.user.pojo.vo.RegisterVO;
import io.github.sakana.user.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    Result<RegisterVO> register(@RequestBody RegisterDTO registerDTO,
                                @RequestHeader(HeadersConstant.USER_IP) String ip,
                                @RequestHeader(HeadersConstant.USER_DEVICE) String device) {
        registerDTO.setIp(ip);
        registerDTO.setDevice(device);
        String token = authService.register(registerDTO);

        RegisterVO registerVO = new RegisterVO();
        registerVO.setToken(token);
        return Result.success(registerVO);
    }

    @PostMapping("/login")
    Result<LoginVO> login(@RequestBody LoginDTO loginDTO,
                          @RequestHeader(HeadersConstant.USER_IP) String ip,
                          @RequestHeader(HeadersConstant.USER_DEVICE) String device) {
        loginDTO.setIp(ip);
        loginDTO.setDevice(device);
        String token = authService.login(loginDTO);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        return Result.success(loginVO);
    }
}
