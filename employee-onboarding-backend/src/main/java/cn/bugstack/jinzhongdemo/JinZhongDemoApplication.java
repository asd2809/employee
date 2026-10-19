package cn.bugstack.jinzhongdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.bugstack.jinzhongdemo.mapper")
public class JinZhongDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(JinZhongDemoApplication.class, args);
    }

}