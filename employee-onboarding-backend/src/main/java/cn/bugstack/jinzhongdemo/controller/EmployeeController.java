package cn.bugstack.jinzhongdemo.controller;

import cn.bugstack.jinzhongdemo.common.Result;
import cn.bugstack.jinzhongdemo.dto.CreateEmployeeRequest;
import cn.bugstack.jinzhongdemo.dto.LoginRequest;
import cn.bugstack.jinzhongdemo.dto.LoginResponse;
import cn.bugstack.jinzhongdemo.security.LoginUser;
import cn.bugstack.jinzhongdemo.service.AuthService;
import cn.bugstack.jinzhongdemo.service.EmployeeService;
import cn.bugstack.jinzhongdemo.vo.EmployeeVO;
import cn.bugstack.jinzhongdemo.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AuthService authService;

    /** 登录换取令牌，唯一无需认证的接口 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("员工登录，账号：{}", request.getAccount());
        return Result.success(authService.login(request));
    }

    /** 根据请求头中的令牌返回当前登录用户，可用于校验令牌是否有效 */
    @GetMapping("/me")
    public Result<LoginVO> me(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(loginUser.toLoginVO());
    }

    /** 新建员工账号，仅管理员可用 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<Map<String, Long>> createEmployee(@RequestBody CreateEmployeeRequest request) {
        log.info("新建员工账号，账号：{}", request.getAccount());
        Long id = employeeService.createEmployee(request);
        Map<String, Long> data = new HashMap<>();
        data.put("id", id);
        return Result.success(data);
    }

    /** 查询员工列表，仅管理员可用 */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Result<List<EmployeeVO>> listEmployees() {
        log.info("查询员工列表");
        List<EmployeeVO> list = employeeService.listEmployees();
        return Result.success(list);
    }
}
