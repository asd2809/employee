package cn.bugstack.jinzhongdemo.dto;

import lombok.Data;

/**
 * 修改密码。
 * 本人修改时必须提供原密码；管理员重置他人员工密码时可不传原密码。
 */
@Data
public class ChangePasswordRequest {
    /** 原密码，管理员重置他人密码时可为空 */
    private String oldPassword;
    private String newPassword;
}
