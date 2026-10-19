package cn.bugstack.jinzhongdemo.vo;

import lombok.Data;

@Data
public class LoginVO {
    private Long id;
    private String account;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private Integer role;
}