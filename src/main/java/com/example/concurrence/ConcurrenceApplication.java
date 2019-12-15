package com.example.concurrence;

import com.example.concurrence.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class ConcurrenceApplication {

	@Autowired
	private ProductService productService;
	public static void main(String[] args) {
		SpringApplication.run(ConcurrenceApplication.class, args);

	}

	// 计划任务 每隔 0.5秒 获取 redis 订单列表存入 mysql
	@Scheduled(fixedDelay = 500)
	public void autoRun() {
		productService.setOrderToMysql();
	}
}
