package com.zoodeck.identity.database;

import org.json.JSONObject;

public interface DatabaseService {
    JSONObject authorizeUser(String email, String password, String token);
    JSONObject registerUser(String email, String password);
}
