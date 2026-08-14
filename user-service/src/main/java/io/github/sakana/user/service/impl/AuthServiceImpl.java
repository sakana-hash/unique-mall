package io.github.sakana.user.service.impl;

import io.github.sakana.common.properties.JWTProperty;
import io.github.sakana.common.utils.JWTUtil;
import io.github.sakana.snowflake.SnowflakeIdGenerator;
import io.github.sakana.user.enumeration.UserErrorCode;
import io.github.sakana.user.mapper.UserLoginLogMapper;
import io.github.sakana.user.mapper.UserMapper;
import io.github.sakana.user.mapper.UserProfileMapper;
import io.github.sakana.user.pojo.dto.LoginDTO;
import io.github.sakana.user.pojo.dto.RegisterDTO;
import io.github.sakana.user.pojo.entity.User;
import io.github.sakana.user.pojo.entity.UserLoginLog;
import io.github.sakana.user.pojo.entity.UserProfile;
import io.github.sakana.user.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserProfileMapper profileMapper;
    @Autowired
    private UserLoginLogMapper loginLogMapper;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private JWTProperty jwtProperty;

    @Override
    @Transactional
    public String register(RegisterDTO registerDTO) {
        if (registerDTO == null) {
            throw UserErrorCode.REGISTER_REQUEST_REQUIRED.exception();
        }

        String username = registerDTO.getUsername();
        String password = registerDTO.getPassword();
        String ip = registerDTO.getIp();
        String device = registerDTO.getDevice();

        if (username == null || username.isBlank()) {
            throw UserErrorCode.USERNAME_REQUIRED.exception();
        }

        if (password == null || password.isBlank()) {
            throw UserErrorCode.PASSWORD_REQUIRED.exception();
        }

        if (ip == null || ip.isBlank()) {
            throw UserErrorCode.CLIENT_IP_REQUIRED.exception();
        }

        Long id = snowflakeIdGenerator.nextId();
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw UserErrorCode.USERNAME_ALREADY_EXISTS.exception(null, ex);
        }

        UserProfile profile = new UserProfile();
        profile.setUserId(id);
        profileMapper.insert(profile);

        LocalDateTime now = LocalDateTime.now();
        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setUserId(user.getId());
        loginLog.setIp(ip);
        loginLog.setDevice(device);
        loginLog.setLoginTime(now);
        loginLogMapper.insert(loginLog);
        return JWTUtil.issueJWT(jwtProperty.getSecretKey(), jwtProperty.getTtl(), id.toString());
    }

    @Override
    public String login(LoginDTO loginDTO) {
        if (loginDTO == null) {
            throw UserErrorCode.LOGIN_REQUEST_REQUIRED.exception();
        }

        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        String ip = loginDTO.getIp();
        String device = loginDTO.getDevice();

        if (username == null || username.isBlank()) {
            throw UserErrorCode.USERNAME_REQUIRED.exception();
        }

        if (password == null || password.isBlank()) {
            throw UserErrorCode.PASSWORD_REQUIRED.exception();
        }

        if (ip == null || ip.isBlank()) {
            throw UserErrorCode.CLIENT_IP_REQUIRED.exception();
        }

        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw UserErrorCode.INVALID_CREDENTIALS.exception();
        }

        LocalDateTime now = LocalDateTime.now();
        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setUserId(user.getId());
        loginLog.setIp(ip);
        loginLog.setDevice(device);
        loginLog.setLoginTime(now);
        loginLogMapper.insert(loginLog);
        return JWTUtil.issueJWT(jwtProperty.getSecretKey(), jwtProperty.getTtl(), user.getId().toString());
    }
}
