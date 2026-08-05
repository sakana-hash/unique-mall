package io.github.sakana.user.service;

import io.github.sakana.user.pojo.dto.LoginDTO;
import io.github.sakana.user.pojo.dto.RegisterDTO;

public interface AuthService {

    String register(RegisterDTO registerDTO);
    String login(LoginDTO loginDTO);
}
