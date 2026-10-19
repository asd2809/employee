package cn.bugstack.jinzhongdemo.service.impl;

import cn.bugstack.jinzhongdemo.domain.Employee;
import cn.bugstack.jinzhongdemo.dto.CreateEmployeeRequest;
import cn.bugstack.jinzhongdemo.dto.LoginRequest;
import cn.bugstack.jinzhongdemo.mapper.EmployeeMapper;
import cn.bugstack.jinzhongdemo.service.EmployeeService;
import cn.bugstack.jinzhongdemo.vo.EmployeeVO;
import cn.bugstack.jinzhongdemo.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee>
        implements EmployeeService {

    @Override
    public LoginVO login(LoginRequest request) {
        String passwordHash = DigestUtils.md5DigestAsHex(
                request.getPassword().getBytes(StandardCharsets.UTF_8));

        Employee employee = this.getOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getAccount, request.getAccount())
                .eq(Employee::getPassword, passwordHash));

        if (employee == null) {
            return null;
        }

        LoginVO vo = new LoginVO();
        vo.setId(employee.getId());
        vo.setAccount(employee.getAccount());
        vo.setName(employee.getName());
        vo.setGender(employee.getGender());
        vo.setPhone(employee.getPhone());
        vo.setEmail(employee.getEmail());
        vo.setRole(employee.getRole());
        return vo;
    }

    @Override
    public Long createEmployee(CreateEmployeeRequest request) {
        String passwordHash = DigestUtils.md5DigestAsHex(
                request.getPassword().getBytes(StandardCharsets.UTF_8));

        Employee employee = new Employee();
        employee.setAccount(request.getAccount());
        employee.setPassword(passwordHash);
        employee.setName(request.getName());
        employee.setGender(request.getGender());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setRole(0);
        employee.setCreateTime(new Date());
        employee.setUpdateTime(new Date());

        this.save(employee);
        return employee.getId();
    }

    @Override
    public List<EmployeeVO> listEmployees() {
        List<Employee> employees = this.list(
                new LambdaQueryWrapper<Employee>()
                        .select(Employee::getAccount, Employee::getName));

        return employees.stream().map(e -> {
            EmployeeVO vo = new EmployeeVO();
            vo.setAccount(e.getAccount());
            vo.setName(e.getName());
            return vo;
        }).collect(Collectors.toList());
    }
}