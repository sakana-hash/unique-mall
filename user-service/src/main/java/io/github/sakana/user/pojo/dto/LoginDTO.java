package io.github.sakana.user.pojo.dto;

import lombok.Data;

@Data
public class LoginDTO {

    private String username;
    private String password;

    private String ip;
    private String device;
}
