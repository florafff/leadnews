package com.heima.wemedia.filter;

import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.threadlocal.WmThreadLocalUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Order(1)
@WebFilter(filterName = "wmTokenFilter",urlPatterns = "/*")
@Log4j2
public class WmTokenFilter extends GenericFilterBean {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        //1.获取请求对象和响应对象
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //2.从header中获取userId
//        String token = request.getHeader("token");
        String userId = request.getHeader("userId");
        if(StringUtils.isNotBlank(userId)){
            //3.如果存在，放到当前的线程中
            WmUser user = new WmUser();
            user.setId(Integer.valueOf(userId));
            WmThreadLocalUtils.setUser(user);
        }

        //4.放行
        filterChain.doFilter(request,response);

    }
}
