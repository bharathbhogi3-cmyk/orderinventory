package com.apple.supply.orderinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class OrderinventoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderinventoryApplication.class, args);
	}

}
