package io.github.sakana.user.controller;

import io.github.sakana.common.result.Result;
import io.github.sakana.user.pojo.dto.LoginDTO;
import io.github.sakana.user.pojo.dto.RegisterDTO;
import io.github.sakana.user.pojo.vo.LoginVO;
import io.github.sakana.user.pojo.vo.RegisterVO;
import io.github.sakana.user.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    Result<RegisterVO> register(@RequestBody RegisterDTO registerDTO) {
        String token = authService.register(registerDTO.getUsername(), registerDTO.getPassword());

        RegisterVO registerVO = new RegisterVO();
        registerVO.setToken(token);
        return Result.success(registerVO);
    }

    @PostMapping("/login")
    Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        String token = authService.login(loginDTO.getUsername(), loginDTO.getPassword());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        return Result.success(loginVO);
    }
}
