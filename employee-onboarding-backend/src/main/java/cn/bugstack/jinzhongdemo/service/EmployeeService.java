package cn.bugstack.jinzhongdemo.service;

import cn.bugstack.jinzhongdemo.domain.Employee;
import cn.bugstack.jinzhongdemo.dto.CreateEmployeeRequest;
import cn.bugstack.jinzhongdemo.vo.EmployeeVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EmployeeService extends IService<Employee> {

    Long createEmployee(CreateEmployeeRequest request);

    List<EmployeeVO> listEmployees();
}