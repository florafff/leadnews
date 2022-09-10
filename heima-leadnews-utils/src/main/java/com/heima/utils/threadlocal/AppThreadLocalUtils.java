package com.heima.utils.threadlocal;

import com.heima.model.user.pojos.ApUser;

public class AppThreadLocalUtils {

    private static final ThreadLocal<ApUser> USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUser(ApUser wmUser){
        USER_THREAD_LOCAL.set(wmUser);
    }

    public static ApUser getUser(){
        return USER_THREAD_LOCAL.get();
    }
}
