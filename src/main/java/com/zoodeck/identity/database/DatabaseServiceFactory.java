package com.zoodeck.identity.database;

import com.zoodeck.common.config.ConfigService;

public class DatabaseServiceFactory {
    public static DatabaseService getDatabaseService(ConfigService configService) {
        return new MockDatabaseService();
    }
}
