package cn.bugstack.jinzhongdemo.mapper;

import cn.bugstack.jinzhongdemo.domain.Employee;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;

/**
* @author leneve
* @description 针对表【employee(员工表)】的数据库操作Mapper
* @createDate 2026-10-19 15:04:26
* @Entity cn.bugstack.jinzhongdemo.domain.Employee
*/
@MapperScan
public interface EmployeeMapper extends BaseMapper<Employee> {

}




