package com.fwmagic.json;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author:fw
 * @Description:
 * @Date:Create in 2017/12/1
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class,args);
        System.out.println("Server startup!");
    }
}
