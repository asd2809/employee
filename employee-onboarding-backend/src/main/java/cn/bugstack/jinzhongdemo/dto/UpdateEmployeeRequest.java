package cn.bugstack.jinzhongdemo.dto;

import lombok.Data;

/**
 * 修改员工资料：只允许改姓名、性别、手机号、邮箱。
 * 账号与角色不可通过该接口修改，防止越权。
 */
@Data
public class UpdateEmployeeRequest {
    private String name;
    private Integer gender;
    private String phone;
    private String email;
}
