package ng.upperlink.nibss.cmms.interceptors;

import com.google.gson.Gson;
import ng.upperlink.nibss.cmms.config.cache.JWTRedisToken;
import ng.upperlink.nibss.cmms.config.cache.SessionManager;
import ng.upperlink.nibss.cmms.config.cache.UserTokenCacheService;
import ng.upperlink.nibss.cmms.dto.UserDetail;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.enums.Errors;
import ng.upperlink.nibss.cmms.errorHandler.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/*
* intercepts all the requests that are bound to this interceptor before they are processed - mainly downloads and uploads
*
* */
@Component
public class DownloadTokenInterceptor extends HandlerInterceptorAdapter {

    private final static Logger logger = LoggerFactory.getLogger(InterceptorConfig.class);

    private JWTRedisToken jwtRedisToken;
    private UserTokenCacheService userTokenCacheService;
    private SessionManager sessionManager;

    @Autowired
    public void setUserTokenCacheService(UserTokenCacheService userTokenCacheService) {
        this.userTokenCacheService = userTokenCacheService;
    }

    @Autowired
    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Autowired
    public void setJwtRedisToken(JWTRedisToken jwtRedisToken) {
        this.jwtRedisToken = jwtRedisToken;
    }

    //handle requests from the client before they are processed further
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())){
            return true;
        }

        String TOKEN_IN_THE_PARAM = "token";

        UserDetail userDetail = jwtRedisToken.decodeToken(request.getParameter(TOKEN_IN_THE_PARAM));

        if (userDetail == null){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print(new Gson().toJson(new ErrorDetails(new Date(),String.valueOf(HttpServletResponse.SC_UNAUTHORIZED), Errors.UNKNOWN_USER.getValue())));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Access-Control-Allow-Origin",request.getHeader("Origin"));
            return false;
        }

        if (!jwtRedisToken.isValidUserSession(userDetail.getSessionId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().print(new Gson().toJson(new ErrorDetails(new Date(),String.valueOf(HttpServletResponse.SC_FORBIDDEN),Errors.EXPIRED_SESSION.getValue())));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Access-Control-Allow-Origin",request.getHeader("Origin"));
            return false;
        }

        if (!jwtRedisToken.isValidUserToken(request.getParameter(TOKEN_IN_THE_PARAM), userDetail.getSessionId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().print(new Gson().toJson(new ErrorDetails(new Date(),String.valueOf(HttpServletResponse.SC_FORBIDDEN), Errors.EXPIRED_TOKEN.getValue())));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Access-Control-Allow-Origin",request.getHeader("Origin"));
            return false;
        }

        //update the sessions expire
        sessionManager.updateTimeout(userDetail.getSessionId());
        request.setAttribute(Constants.USER_DETAIL, userDetail);
        return true;

    }
}
