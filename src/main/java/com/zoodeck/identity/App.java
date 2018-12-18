package com.zoodeck.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zoodeck.identity.config.ConfigService;
import com.zoodeck.identity.config.ConfigServiceFactory;

public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        logger.info("starting identity...");
        ConfigService configService = ConfigServiceFactory.getConfigService();
        IdentityWorker identityWorker = new IdentityWorker(configService);
    }
}
