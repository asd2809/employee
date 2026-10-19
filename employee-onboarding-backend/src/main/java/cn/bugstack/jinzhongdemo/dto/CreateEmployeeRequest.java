package cn.bugstack.jinzhongdemo.dto;

import lombok.Data;

@Data
public class CreateEmployeeRequest {
    private String account;
    private String password;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
}