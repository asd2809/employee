package cn.bugstack.jinzhongdemo.security;

import cn.bugstack.jinzhongdemo.domain.Employee;
import cn.bugstack.jinzhongdemo.mapper.EmployeeMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 从 MySQL 的 employee 表加载用户，供 DaoAuthenticationProvider 与 JWT 过滤器使用。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final EmployeeMapper employeeMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeMapper.selectOne(
                Wrappers.<Employee>lambdaQuery().eq(Employee::getAccount, username));
        if (employee == null) {
            // 不暴露“账号不存在”，统一提示，避免账号枚举
            throw new UsernameNotFoundException("账号或密码错误");
        }
        return new LoginUser(employee);
    }
}
