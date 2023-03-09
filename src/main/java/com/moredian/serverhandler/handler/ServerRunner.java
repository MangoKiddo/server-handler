package com.moredian.serverhandler.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author wk
 * @date 2022/6/6 18:08
 */
@Component
public class ServerRunner implements ApplicationRunner {

    private final Logger logger = LoggerFactory.getLogger(NettyHttpServerHandler.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int port = 8001;
        logger.info("netty server started ,port:"+port);
        new NettyHttpServer(port).run();
    }

}
