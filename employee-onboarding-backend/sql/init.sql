-- 1. 如果不存在，则创建数据库
CREATE DATABASE IF NOT EXISTS employee_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 切换到这个数据库
USE employee_system;

-- 3. 创建员工表
CREATE TABLE employee (
                          id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                          account VARCHAR(50) NOT NULL COMMENT '登录账号',
                          password VARCHAR(255) NOT NULL COMMENT '密码哈希',
                          name VARCHAR(50) NOT NULL COMMENT '员工姓名',
                          gender TINYINT NOT NULL COMMENT '性别',
                          phone VARCHAR(20) NOT NULL COMMENT '手机号',
                          email VARCHAR(100) NOT NULL COMMENT '邮箱',
                          role TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0普通员工，1管理员',
                          create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          PRIMARY KEY (id),
                          UNIQUE KEY uk_employee_account (account)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='员工表';

-- 4. 预置管理员账号，明文密码 123456，MD5哈希入库
INSERT INTO employee(account, password, name, gender, phone, email, role)
VALUES ('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 1, '13900138000', 'admin@test.com', 1);
-- 5. 预置普通员工账号 test01，密码123456
INSERT INTO employee(account, password, name, gender, phone, email, role)
VALUES ('test01', 'e10adc3949ba59abbe56e057f20f883e', '普通测试员工', 0, '13900139000', 'test01@test.com', 0);
