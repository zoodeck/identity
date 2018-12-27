package com.zoodeck.identity.data;

import com.zoodeck.common.config.ConfigService;

public class DataServiceFactory {
    public static DataService getDataService(ConfigService configService) {
        return new MockDataService();
    }
}
