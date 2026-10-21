package cn.bugstack.jinzhongdemo.service;

import cn.bugstack.jinzhongdemo.domain.Employee;
import cn.bugstack.jinzhongdemo.dto.ChangePasswordRequest;
import cn.bugstack.jinzhongdemo.dto.CreateEmployeeRequest;
import cn.bugstack.jinzhongdemo.dto.UpdateEmployeeRequest;
import cn.bugstack.jinzhongdemo.security.LoginUser;
import cn.bugstack.jinzhongdemo.vo.EmployeeVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EmployeeService extends IService<Employee> {

    Long createEmployee(CreateEmployeeRequest request);

    List<EmployeeVO> listEmployees();

    /** 查询员工详情：管理员可查任意员工，普通员工只能查自己 */
    EmployeeVO getEmployee(Long id, LoginUser operator);

    /** 修改员工资料：管理员可改任意员工，普通员工只能改自己 */
    EmployeeVO updateEmployee(Long id, UpdateEmployeeRequest request, LoginUser operator);

    /** 修改密码：本人需验证原密码；管理员可重置普通员工密码，不能重置其他管理员 */
    void changePassword(Long id, ChangePasswordRequest request, LoginUser operator);

    /** 删除员工（逻辑删除/离职标记）：仅管理员，且不能删除自己与其他管理员 */
    void deleteEmployee(Long id, LoginUser operator);
}
