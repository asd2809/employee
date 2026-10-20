package cn.bugstack.jinzhongdemo.security;

import cn.bugstack.jinzhongdemo.domain.Employee;
import cn.bugstack.jinzhongdemo.vo.LoginVO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 的登录用户，包装数据库中的 employee 记录。
 * 该表没有独立的启用/禁用字段，因此账号默认始终启用。
 */
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /** employee.role：1 表示管理员，其余为普通员工 */
    private static final int ROLE_ADMIN = 1;

    private final Employee employee;
    private final String roleName;

    public LoginUser(Employee employee) {
        this.employee = employee;
        this.roleName = employee.getRole() != null && employee.getRole() == ROLE_ADMIN ? "ADMIN" : "USER";
    }

    public Employee getEmployee() {
        return employee;
    }

    public Long getId() {
        return employee.getId();
    }

    /** 登录账号对应 employee.account，Spring Security 以“用户名”方式使用 */
    @Override
    public String getUsername() {
        return employee.getAccount();
    }

    @Override
    public String getPassword() {
        return employee.getPassword();
    }

    /** 姓名，JWT 中作为 nickname 声明 */
    public String getNickname() {
        return employee.getName();
    }

    /** 角色名（ADMIN / USER），JWT 中作为 role 声明 */
    public String getRole() {
        return roleName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 角色统一加 ROLE_ 前缀，便于使用 hasRole('ADMIN') 表达式
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /** 对外暴露的用户信息，不含密码 */
    public LoginVO toLoginVO() {
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
}
