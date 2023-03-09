package com.moredian.serverhandler;

import com.moredian.serverhandler.handler.NettyHttpServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

/**
 * @author wk
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.moredian.serverhandler.handler")
//@ImportResource("classpath:spring.xml")
public class ServerHandlerApplication {

    public static void main(String[] args) throws InterruptedException {

        SpringApplication.run(ServerHandlerApplication.class, args);
    }
}
