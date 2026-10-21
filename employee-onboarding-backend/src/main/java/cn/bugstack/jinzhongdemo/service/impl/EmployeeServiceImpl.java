package cn.bugstack.jinzhongdemo.service.impl;

import cn.bugstack.jinzhongdemo.domain.Employee;
import cn.bugstack.jinzhongdemo.dto.ChangePasswordRequest;
import cn.bugstack.jinzhongdemo.dto.CreateEmployeeRequest;
import cn.bugstack.jinzhongdemo.dto.UpdateEmployeeRequest;
import cn.bugstack.jinzhongdemo.mapper.EmployeeMapper;
import cn.bugstack.jinzhongdemo.security.LoginUser;
import cn.bugstack.jinzhongdemo.service.EmployeeService;
import cn.bugstack.jinzhongdemo.vo.EmployeeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee>
        implements EmployeeService {

    /** employee.role：1 表示管理员 */
    private static final int ROLE_ADMIN = 1;

    private static final int MIN_PASSWORD_LENGTH = 6;

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
                new LambdaQueryWrapper<Employee>().orderByAsc(Employee::getId));

        return employees.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public EmployeeVO getEmployee(Long id, LoginUser operator) {
        assertSelfOrAdmin(id, operator);
        return toVO(requireEmployee(id));
    }

    @Override
    public EmployeeVO updateEmployee(Long id, UpdateEmployeeRequest request, LoginUser operator) {
        assertSelfOrAdmin(id, operator);
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("员工姓名不能为空");
        }
        Employee employee = requireEmployee(id);
        employee.setName(request.getName().trim());
        employee.setGender(request.getGender());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setUpdateTime(new Date());
        this.updateById(employee);
        return toVO(employee);
    }

    @Override
    public void changePassword(Long id, ChangePasswordRequest request, LoginUser operator) {
        Employee employee = requireEmployee(id);
        boolean self = Objects.equals(id, operator.getId());

        if (!self && !isAdmin(operator)) {
            throw new AccessDeniedException("只能修改自己的密码");
        }
        if (!self && isAdminEmployee(employee)) {
            throw new AccessDeniedException("不能重置其他管理员的密码");
        }
        // 本人修改必须校验原密码；管理员重置普通员工密码时无需原密码
        if (self) {
            if (!StringUtils.hasText(request.getOldPassword())) {
                throw new IllegalArgumentException("请输入原密码");
            }
            if (!passwordEncoder.matches(request.getOldPassword(), employee.getPassword())) {
                throw new IllegalArgumentException("原密码不正确");
            }
        }

        String newPassword = request.getNewPassword();
        if (!StringUtils.hasText(newPassword) || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("新密码至少需要 " + MIN_PASSWORD_LENGTH + " 位");
        }
        if (passwordEncoder.matches(newPassword, employee.getPassword())) {
            throw new IllegalArgumentException("新密码不能与原密码相同");
        }

        employee.setPassword(passwordEncoder.encode(newPassword));
        employee.setUpdateTime(new Date());
        this.updateById(employee);
    }

    @Override
    public void deleteEmployee(Long id, LoginUser operator) {
        if (!isAdmin(operator)) {
            throw new AccessDeniedException("仅管理员可以删除员工账号");
        }
        if (Objects.equals(id, operator.getId())) {
            throw new IllegalArgumentException("不能删除自己的账号");
        }
        Employee employee = requireEmployee(id);
        if (isAdminEmployee(employee)) {
            throw new IllegalArgumentException("管理员账号不允许删除");
        }
        // @TableLogic：removeById 实际执行 UPDATE employee SET deleted = 1
        this.removeById(id);
    }

    private Employee requireEmployee(Long id) {
        Employee employee = this.getById(id);
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在或已被删除");
        }
        return employee;
    }

    private void assertSelfOrAdmin(Long id, LoginUser operator) {
        if (!isAdmin(operator) && !Objects.equals(id, operator.getId())) {
            throw new AccessDeniedException("只能查看或修改自己的资料");
        }
    }

    private boolean isAdmin(LoginUser operator) {
        return operator != null && "ADMIN".equals(operator.getRole());
    }

    private boolean isAdminEmployee(Employee employee) {
        return employee.getRole() != null && employee.getRole() == ROLE_ADMIN;
    }

    private EmployeeVO toVO(Employee employee) {
        EmployeeVO vo = new EmployeeVO();
        vo.setId(employee.getId());
        vo.setAccount(employee.getAccount());
        vo.setName(employee.getName());
        vo.setGender(employee.getGender());
        vo.setPhone(employee.getPhone());
        vo.setEmail(employee.getEmail());
        vo.setRole(employee.getRole());
        return vo;
    }
}
