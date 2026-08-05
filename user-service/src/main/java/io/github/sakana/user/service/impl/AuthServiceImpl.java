package io.github.sakana.user.service.impl;

import io.github.sakana.common.properties.JWTProperty;
import io.github.sakana.common.utils.JWTUtil;
import io.github.sakana.snowflake.SnowflakeIdGenerator;
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
        String username = registerDTO.getUsername();
        String password = registerDTO.getPassword();
        String ip = registerDTO.getIp();
        String device = registerDTO.getDevice();

        if (username == null || username.isBlank()) {
            throw new RuntimeException("用户名不能为空");
        }

        if (password == null || password.isBlank()) {
            throw new RuntimeException("密码不能为空");
        }

        if (ip == null || ip.isBlank()) {
            throw new RuntimeException("ip不能为空");
        }

        Long id = snowflakeIdGenerator.nextId();
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setCreatedTime(now);
        user.setUpdatedTime(now);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new RuntimeException(String.format(
                    "重复的用户名: %s", username
            ));
        }

        Long profileId = snowflakeIdGenerator.nextId();
        now = LocalDateTime.now();
        UserProfile profile = new UserProfile();
        profile.setId(profileId);
        profile.setUserId(id);
        profile.setCreatedTime(now);
        profile.setUpdatedTime(now);
        profileMapper.insert(profile);

        Long logId = snowflakeIdGenerator.nextId();
        now = LocalDateTime.now();
        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setId(logId);
        loginLog.setUserId(user.getId());
        loginLog.setIp(ip);
        loginLog.setDevice(device);
        loginLog.setLoginTime(now);
        loginLog.setCreatedTime(now);
        loginLogMapper.insert(loginLog);
        return JWTUtil.issueJWT(jwtProperty.getSecretKey(), jwtProperty.getTtl(), id.toString());
    }

    @Override
    public String login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        String ip = loginDTO.getIp();
        String device = loginDTO.getDevice();

        if (username == null || username.isBlank()) {
            throw new RuntimeException("用户名不能为空");
        }

        if (password == null || password.isBlank()) {
            throw new RuntimeException("密码不能为空");
        }

        if (ip == null || ip.isBlank()) {
            throw new RuntimeException("ip不能为空");
        }

        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        Long logId = snowflakeIdGenerator.nextId();
        LocalDateTime now = LocalDateTime.now();
        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setId(logId);
        loginLog.setUserId(user.getId());
        loginLog.setIp(ip);
        loginLog.setDevice(device);
        loginLog.setLoginTime(now);
        loginLog.setCreatedTime(now);
        loginLogMapper.insert(loginLog);
        return JWTUtil.issueJWT(jwtProperty.getSecretKey(), jwtProperty.getTtl(), user.getId().toString());
    }
}
