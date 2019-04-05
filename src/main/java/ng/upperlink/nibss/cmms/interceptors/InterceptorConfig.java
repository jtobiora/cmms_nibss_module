package ng.upperlink.nibss.cmms.interceptors;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.config.cache.JWTRedisToken;
import ng.upperlink.nibss.cmms.config.cache.SessionManager;
import ng.upperlink.nibss.cmms.config.cache.UserTokenCacheService;
import ng.upperlink.nibss.cmms.dto.UserDetail;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.enums.Errors;
import ng.upperlink.nibss.cmms.enums.SecurityConstants;
import ng.upperlink.nibss.cmms.errorHandler.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/*
* intercepts all the requests that are bound to this interceptor before they are processed
*
* */
@Component
@Slf4j
public class InterceptorConfig extends HandlerInterceptorAdapter{

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

    //handles requests before they are routed to the webservice called
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())){
            return true;
        }

        if (((HandlerMethod)handler).getMethod().getName().contains("ThirdParty")){
            return true;
        }

        UserDetail userDetail = jwtRedisToken.decodeToken(request.getHeader(SecurityConstants.HEADER_STRING.getValue()));

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

        if (!jwtRedisToken.isValidUserToken(request.getHeader(SecurityConstants.HEADER_STRING.getValue()), userDetail.getSessionId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().print(new Gson().toJson(new ErrorDetails(new Date(), String.valueOf(HttpServletResponse.SC_FORBIDDEN), Errors.EXPIRED_TOKEN.getValue())));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            return false;
        }

        List<String> task = userTokenCacheService.getTask(userDetail.getSessionId(), request.getHeader(SecurityConstants.HEADER_STRING.getValue()));

        //confirm that the this user have the right visit this url
//        if (!isUserAuthorized(task,(HandlerMethod) handler)){
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.getWriter().print(new Gson().toJson(new ErrorDetails(new Date(),String.valueOf(HttpServletResponse.SC_UNAUTHORIZED),Errors.NOT_PERMITTED.getValue())));
//            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//            response.setHeader("Access-Control-Allow-Origin",request.getHeader("Origin"));
//            return false;
//        }


        //update the sessions expiry time
        sessionManager.updateTimeout(userDetail.getSessionId());
        addToSecurityContextAndServletRequest(userDetail,request);

        return true;
    }

    //handles requests after they are post processed
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    private boolean isUserAuthorized(List<String> tasks, HandlerMethod handlerMethod){

        String methodName = handlerMethod.getMethod().getName();

        if (!methodName.toLowerCase().contains("create") && !methodName.toLowerCase().contains("update") && !methodName.toLowerCase().contains("toggle")){
            return true;
        }

        if (tasks.stream().anyMatch(s -> s.equals(methodName))) {
            return true;
        }
        return false;
    }

    //add authenticated user to the spring context
    private void addToSecurityContextAndServletRequest(UserDetail userDetail, HttpServletRequest request){
        request.setAttribute(Constants.USER_DETAIL, userDetail);

        String username = userDetail.getEmailAddress();
        log.trace("user detail from token: {}", userDetail);

        Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(authentication);

    }

}

