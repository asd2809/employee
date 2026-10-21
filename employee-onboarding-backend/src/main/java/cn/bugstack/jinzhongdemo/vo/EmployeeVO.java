package cn.bugstack.jinzhongdemo.vo;

import lombok.Data;

/**
 * 员工信息（不含密码），用于列表、详情与资料编辑回显。
 */
@Data
public class EmployeeVO {
    private Long id;
    private String account;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private Integer role;
}
