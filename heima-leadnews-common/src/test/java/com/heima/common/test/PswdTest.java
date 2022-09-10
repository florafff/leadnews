package com.heima.common.test;

import com.heima.utils.common.BCrypt;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.DigestUtils;

public class PswdTest {

    public static void main(String[] args) {
        //md5加密
       /* String password = "abc";
        String pswd = DigestUtils.md5DigestAsHex(password.getBytes());
        //900150983cd24fb0d6963f7d28e17f72
        System.out.println(pswd);*/

        //手动加盐
       /* String salt = RandomStringUtils.randomAlphanumeric(10);
        System.out.println(salt);
        String password = "abc";
        password =password+salt;
        String pswd = DigestUtils.md5DigestAsHex(password.getBytes());
        System.out.println(pswd);*/

        //自动加盐
        String gensalt = BCrypt.gensalt();
        System.out.println(gensalt);
        String password = "abcddd";
        String hashpw = BCrypt.hashpw(password, gensalt);
        System.out.println(hashpw);

        boolean checkpw = BCrypt.checkpw(password, "$2a$10$ad4QqX2nK8j.LuRbIcMnj.eqEqSayKtzHZ4yoFQmi5GEgrPM7UDgy");
        System.out.println(checkpw);
    }
}
