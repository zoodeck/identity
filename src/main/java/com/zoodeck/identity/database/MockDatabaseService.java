package com.zoodeck.identity.database;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.*;

public class MockDatabaseService implements DatabaseService {
    private static Logger logger = LoggerFactory.getLogger(MockDatabaseService.class);

    private List<JSONObject> users;

    public MockDatabaseService() {
        users = new ArrayList<>();
    }

    @Override
    public JSONObject authorizeUser(String email, String password, String token) {
        JSONObject result = new JSONObject();

        try {
            Optional<JSONObject> optionalUser = users.stream().filter(u -> u.getString("email").equals(email)).findFirst();
            if (optionalUser.isPresent()) {
                JSONObject user = optionalUser.get();
                logger.info("authorizeUser user: " + user.toString());

                byte[] salt = decode(user.getString("salt"));

                KeySpec spec = getKeySpec(password, salt);
                SecretKeyFactory factory = getSecretKeyFactory();
                byte[] hashedPassword = factory.generateSecret(spec).getEncoded();
                String stringHashedPassword = encode(hashedPassword);

                String storedHashedPassword = user.getString("hashedPassword");
                if (storedHashedPassword.equals(stringHashedPassword)) {
                    result.put("success", true);
                    return result;
                }
            }

            result.put("success", false);
            return result;
        } catch (Exception e) {
            result.put("success", false);
            return result;
        }
    }

    @Override
    public JSONObject registerUser(String email, String password) {
        JSONObject result = new JSONObject();

        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            KeySpec spec = getKeySpec(password, salt);
            SecretKeyFactory factory = getSecretKeyFactory();
            byte[] hashedPassword = factory.generateSecret(spec).getEncoded();

            JSONObject user = new JSONObject();
            user.put("email", email);
            user.put("hashedPassword", encode(hashedPassword));
            user.put("salt", encode(salt));

            logger.info("registerUser user: " + user.toString());

            users.add(user);

            result.put("success", true);
            return result;
        } catch (Exception e) {
            logger.error("Exception in registerUser", e);
            result.put("success", false);
            return result;
        }
    }

    private String encode(byte[] src) {
        return Base64.getEncoder().encodeToString(src);
    }

    private byte[] decode(String src) {
        return Base64.getDecoder().decode(src);
    }

    private SecretKeyFactory getSecretKeyFactory() throws Exception {
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    }

    private KeySpec getKeySpec(String password, byte[] salt) {
        return new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
    }
}
