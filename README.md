# 员工入职管理系统（前后端分离）
## 环境依赖
后端：JDK 17、MySQL 8.0
前端：Node.js 18

## 后端 employee-backend
1. 打开数据库客户端（DBeaver / Navicat / MySQL命令行）
2. 执行项目内 `sql/init.sql`
   - 脚本自动创建数据库 `employee_system`、员工表 employee
   - 预置管理员账号：账号 `admin`，明文密码 `123456`（MD5加密入库）
3. 本地数据库配置
   - `src/main/resources/application.yml` 为公共配置，提交Git仓库
   - **新建 `application-local.yml`**，填写本机MySQL用户名、密码，该文件会被.gitignore忽略，不会上传远程仓库
   ```yaml
   spring:
     datasource:
       username: 你的mysql用户名
       password: 你的mysql密码
4. IDEA 导入项目，启动 SpringBoot 主类
后端服务地址：[http://localhost:8080](http://localhost:8080)

### 后端接口清单
#### 1. POST / 创建员工

**请求 JSON 示例（创建管理员）**

```
{
    "account": "admin3",
    "password": "123456",
    "name": "管理员三号",
    "gender": 1,
    "phone": "13911112222",
    "email": "admin3@employee.com",
    "role": 1
}
```

- role=1：管理员；role=0：普通员工
- 后端自动将 password 做 MD5 哈希存入数据库
- account 账号唯一，重复账号提交会报错

#### 2. POST /login 登录接口

**请求 JSON 示例**

```
{
    "account": "admin",
    "password": "123456"
}
```

- 后端将前端传入的明文密码 MD5 加密，与数据库内哈希值比对校验

### 测试指引

1. 执行 init.sql 后，自带预置管理员账号 admin / 123456，可直接登录测试
2. 使用 POST / 接口新建员工，新建完成后，可用新账号登录
3. 普通员工无需在 SQL 脚本预置，通过创建员工接口生成测试数据

## 前端 employee-frontend

1. 进入前端项目目录
2. 执行 `npm install` 安装依赖
3. 执行 `npm run dev` 启动前端
   前端访问地址：[http://localhost:5173](http://localhost:5173)

## Git 规范说明

1. `.gitignore` 忽略本地配置文件 `application-local.yml`，防止本地数据库密码上传到远程仓库
2. 前端 `node_modules`、打包产物 `dist` 不提交代码仓库，使用者本地自行安装依赖

