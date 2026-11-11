package com.example.carinsurance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.carinsurance.mapper")
public class CarInsuranceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarInsuranceApplication.class, args);
    }
}
