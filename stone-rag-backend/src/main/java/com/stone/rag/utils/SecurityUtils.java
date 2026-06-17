package com.stone.rag.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class SecurityUtils {

    private static final int COST = 12;

    /** 加密密码 */
    public static String encode(String rawPassword) {
        return BCrypt.withDefaults()
                .hashToString(COST, rawPassword.toCharArray());
    }

    /** 校验密码 */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.verifyer()
                .verify(rawPassword.toCharArray(), encodedPassword)
                .verified;
    }
}
