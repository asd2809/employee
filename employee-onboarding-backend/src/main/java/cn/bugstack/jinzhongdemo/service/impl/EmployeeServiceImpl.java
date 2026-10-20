package cn.bugstack.jinzhongdemo.service.impl;

import cn.bugstack.jinzhongdemo.domain.Employee;
import cn.bugstack.jinzhongdemo.dto.CreateEmployeeRequest;
import cn.bugstack.jinzhongdemo.mapper.EmployeeMapper;
import cn.bugstack.jinzhongdemo.service.EmployeeService;
import cn.bugstack.jinzhongdemo.vo.EmployeeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee>
        implements EmployeeService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public Long createEmployee(CreateEmployeeRequest request) {
        String passwordHash = passwordEncoder.encode(request.getPassword());

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