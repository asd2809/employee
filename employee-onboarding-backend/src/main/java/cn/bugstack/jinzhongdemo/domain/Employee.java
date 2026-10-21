package cn.bugstack.jinzhongdemo.domain;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("employee")
public class Employee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String account;
    private String password;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private Integer role;

    /** 逻辑删除标记：0 正常，1 已删除（离职）。标记后查询自动过滤，账号无法再登录 */
    @TableLogic
    private Integer deleted;

    private Date createTime;
    private Date updateTime;
}