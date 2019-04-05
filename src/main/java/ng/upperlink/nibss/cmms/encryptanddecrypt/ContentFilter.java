package ng.upperlink.nibss.cmms.encryptanddecrypt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ng.upperlink.nibss.cmms.config.dto.DataExchangeObject;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.enums.ServiceName;
import ng.upperlink.nibss.cmms.enums.URLAuthenticated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;


/*
*  Handles filtering of requests
*
* */
public class ContentFilter implements Filter {

    private Logger LOG = LoggerFactory.getLogger(ContentFilter.class);

    private NIBSSAESEncryption nibssaesEncryption;

    @Autowired
    public void setNibssaesEncryption(NIBSSAESEncryption nibssaesEncryption) {
        this.nibssaesEncryption = nibssaesEncryption;
    }

    @Override
    public void destroy() {

    }

    //filters every request before processing
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        final String url = ((HttpServletRequest)request).getServletPath();
        System.out.println("URL +++++++++++++++++++++++++++++++++++++++  " + url);
        if (ContentFilter.isToBeEncrypted(url)){
            LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            LOG.info("RemoteAddress : {}", request.getRemoteAddr());
            LOG.info("Http Method : {}", ((HttpServletRequest)request).getMethod());
            LOG.info("URL : {}", ((HttpServletRequest)request).getRequestURL());
            LOG.info("ServletPath : {}", ((HttpServletRequest)request).getServletPath());
            LOG.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        }

        String secretKey = "";
        String iv = "";

        //validate Header
        if (ContentFilter.isToBeEncrypted(url)){

            Object validationResult = nibssaesEncryption.validateHeaderValue((HttpServletRequest) request, ServiceName.BVN_AGENT_MANAGEMENT);

            if (validationResult instanceof String){
                //then there is an error
                ((HttpServletResponse) response).setStatus(HttpStatus.BAD_REQUEST.value());
                ((HttpServletResponse) response).setContentType(MediaType.APPLICATION_JSON_VALUE);
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("ResponseCode", (String) validationResult);
                LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                LOG.info("The response is {}", jsonObject.toString());
                LOG.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                response.getWriter().write(jsonObject.toString());
                return;
            }

            Map<String, String> credentials = (Map<String, String>) validationResult;
            secretKey = credentials.get(Constants.SECRET_KEY);
            String username = credentials.get(Constants.USERNAME);
            Authentication authentication = new UsernamePasswordAuthenticationToken(username,username, AuthorityUtils.NO_AUTHORITIES);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            iv = credentials.get(Constants.IV);
        }

        //Request
        HttpServletRequestWritableWrapper requestWrapper = new HttpServletRequestWritableWrapper((HttpServletRequest) request, url, secretKey, iv);

        //Response
        HttpServletResponseReadableWrapper capturingResponseWrapper = new HttpServletResponseReadableWrapper((HttpServletResponse) response);

        filterChain.doFilter(requestWrapper, capturingResponseWrapper);

        if (ContentFilter.isToBeEncrypted(url)){
            String content = capturingResponseWrapper.getCaptureAsString();
            LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            LOG.info("response is {}", content);
            LOG.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            DataExchangeObject exchangeObject = new DataExchangeObject(NIBSSAESEncryption.encryptAES(content , secretKey, iv));
            response.getWriter().write(new Gson().toJson(exchangeObject));
        } else {
            response.getWriter().write(capturingResponseWrapper.getCaptureAsString());
        }

    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {

    }

    public static boolean isToBeEncrypted(String url){

        if (Pattern.compile(URLAuthenticated.AGENTS.replace("{agt-mgr-code}","[a-z|A-Z]*[0-9]+")).matcher(url).matches()){
            return true;
        }else if(Pattern.compile(URLAuthenticated.AGENT_DETAIL.replace("{agt-mgr-code}","[a-z|A-Z]*[0-9]+").replace("{id}","[0-9]+")).matcher(url).matches()){
            return true;
        }if (Pattern.compile(URLAuthenticated.AGT_BRANCH.replace("{agent-id}","[0-9]+")).matcher(url).matches()){
            return true;
        }else if (Pattern.compile(URLAuthenticated.AGT_BRANCH_DETAIL.replace("{agent-id}","[0-9]+").replace("{id}","[0-9]+")).matcher(url).matches()){
            return true;
        }else if (Pattern.compile(URLAuthenticated.AGT_BRANCH_ENROLLERS.replace("{agent-id}","[0-9]+").replace("{id}","[0-9]+")).matcher(url).matches()){
            return true;
        }else if (Pattern.compile(URLAuthenticated.AGT_MGR_REPORT).matcher(url).matches()){
            return true;
        }else if (Pattern.compile(URLAuthenticated.ENROLLERS.replace("{agent-id}","[0-9]+")).matcher(url).matches()){
            return true;
        }else if (Pattern.compile(URLAuthenticated.ENROLLER_DETAIL.replace("{agent-id}","[0-9]+").replace("{id}","[0-9]+")).matcher(url).matches()){
            return true;
        }else if (Pattern.compile(URLAuthenticated.COUNTRY).matcher(url).matches()) {
            return true;
        }else if (Pattern.compile(URLAuthenticated.STATE).matcher(url).matches()) {
            return true;
        }else if (Pattern.compile(URLAuthenticated.LGA.replace("{state-id}","[0-9]+")).matcher(url).matches()) {
            return true;
        }
        return false;
    }

}