package cn.bugstack.jinzhongdemo.controller;

import cn.bugstack.jinzhongdemo.common.Result;
import cn.bugstack.jinzhongdemo.dto.CreateEmployeeRequest;
import cn.bugstack.jinzhongdemo.dto.LoginRequest;
import cn.bugstack.jinzhongdemo.service.EmployeeService;
import cn.bugstack.jinzhongdemo.vo.EmployeeVO;
import cn.bugstack.jinzhongdemo.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequest request) {
        log.info("管理员登录，账号：{}", request.getAccount());
        LoginVO vo = employeeService.login(request);
        if (vo == null) {
            return Result.error(401, "账号或密码错误");
        }
        return Result.success(vo);
    }

    @PostMapping
    public Result<Map<String, Long>> createEmployee(@RequestBody CreateEmployeeRequest request) {
        log.info("新建员工账号，账号：{}", request.getAccount());
        Long id = employeeService.createEmployee(request);
        Map<String, Long> data = new HashMap<>();
        data.put("id", id);
        return Result.success(data);
    }

    @GetMapping
    public Result<List<EmployeeVO>> listEmployees() {
        log.info("查询员工列表");
        List<EmployeeVO> list = employeeService.listEmployees();
        return Result.success(list);
    }
}