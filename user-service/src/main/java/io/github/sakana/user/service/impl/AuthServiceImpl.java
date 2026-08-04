package io.github.sakana.user.service.impl;

import io.github.sakana.common.properties.JWTProperty;
import io.github.sakana.common.utils.JWTUtil;
import io.github.sakana.snowflake.SnowflakeIdGenerator;
import io.github.sakana.user.mapper.UserMapper;
import io.github.sakana.user.pojo.entity.User;
import io.github.sakana.user.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private JWTProperty jwtProperty;

    @Override
    public String register(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new RuntimeException("用户名不能为空");
        }

        if (password == null || password.isBlank()) {
            throw new RuntimeException("密码不能为空");
        }

        Long id = snowflakeIdGenerator.nextId();
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new RuntimeException(String.format(
                    "重复的用户名: %s", username
            ));
        }

        return JWTUtil.issueJWT(jwtProperty.getSecretKey(), jwtProperty.getTtl(), id.toString());
    }
}
