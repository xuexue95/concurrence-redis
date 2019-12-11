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
	@Scheduled(fixedDelay = 500)
	public void autoRun() throws InterruptedException {
		productService.setOrderToMysql();
	}

}
