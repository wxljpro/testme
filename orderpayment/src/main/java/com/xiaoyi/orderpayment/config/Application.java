package com.xiaoyi.orderpayment.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages="com.xiaoyi.orderpayment.utilities.exceptionhandler,com.xiaoyi.orderpayment.config,com.xiaoyi.orderpayment.controller,com.xiaoyi.orderpayment.service,com.xiaoyi.orderpayment.service.imp")
//@EnableScheduling
public class Application extends SpringBootServletInitializer {

	@Autowired
	public AppConfig appConfig;
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application){
		return application.sources(Application.class);
	}
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
