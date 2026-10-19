package cn.bugstack.jinzhongdemo.service;

import cn.bugstack.jinzhongdemo.domain.Employee;
import cn.bugstack.jinzhongdemo.dto.CreateEmployeeRequest;
import cn.bugstack.jinzhongdemo.dto.LoginRequest;
import cn.bugstack.jinzhongdemo.vo.EmployeeVO;
import cn.bugstack.jinzhongdemo.vo.LoginVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EmployeeService extends IService<Employee> {

    LoginVO login(LoginRequest request);

    Long createEmployee(CreateEmployeeRequest request);

    List<EmployeeVO> listEmployees();
}