package com.heima.utils.threadlocal;

import com.heima.model.wemedia.pojos.WmUser;

public class WmThreadLocalUtils {

    private static final ThreadLocal<WmUser> USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUser(WmUser wmUser){
        USER_THREAD_LOCAL.set(wmUser);
    }

    public static WmUser getUser(){
        return USER_THREAD_LOCAL.get();
    }
}
