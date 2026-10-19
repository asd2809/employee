package cn.bugstack.jinzhongdemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 进行健康测试
 */
@RestController
@RequestMapping("/demo")
@Slf4j
public class healthController {

    @GetMapping
    public String health() {
      log.info("项目启动成功");
      return "ok";
    }
}
