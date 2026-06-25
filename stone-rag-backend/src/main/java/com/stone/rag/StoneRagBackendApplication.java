package com.stone.rag;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@MapperScan("com.stone.rag.mapper")
public class StoneRagBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(StoneRagBackendApplication.class, args);
	}

}
