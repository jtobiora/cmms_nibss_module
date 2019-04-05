/*
package com.upl.nibss.bvn.controller;

import com.upl.nibss.bvn.UserManagementApplication;
import com.upl.nibss.bvn.config.cache.JWTRedisToken;
import com.upl.nibss.bvn.config.cache.SessionManager;
import com.upl.nibss.bvn.config.cache.UserTokenCacheService;
import com.upl.nibss.bvn.config.email.MailConfigImpl;
import com.upl.nibss.bvn.dto.UserDetail;
import com.upl.nibss.bvn.enums.EncryptionHeader;
import com.upl.nibss.bvn.enums.MakerCheckerType;
import com.upl.nibss.bvn.enums.UserType;
import DownloadTokenInterceptor;
import InterceptorConfig;
import com.upl.nibss.bvn.model.Country;
import com.upl.nibss.bvn.model.Nibss;
import com.upl.nibss.bvn.repo.*;
import com.upl.nibss.bvn.service.*;
import com.upl.nibss.bvn.util.email.EmailService;
import com.upl.nibss.bvn.util.email.SmtpMailSender;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.spring4.SpringTemplateEngine;

import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

*/
/**
 * Created by stanlee on 14/07/2018.
 *//*


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {UserManagementApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class NibssControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JWTRedisToken jwtRedisToken;

    @Test
    public void testGetAllNibssUser() throws Exception {

        ////To use this Integration Test script, you have to disable Interceptor or provide a valid token

        UserDetail userDetail = new UserDetail();
        userDetail.setUserAuthorizationType(MakerCheckerType.OPERATOR.getValue());
        userDetail.setUserType(UserType.NIBSS.getValue());
        userDetail.setUserName("admin");
        userDetail.setCode("00001");
        userDetail.setRole("Admin");
        userDetail.setUserId(Long.valueOf("1"));

        String token = jwtRedisToken.generateToken(userDetail);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("pageNumber", "0");
        jsonObject.put("pageSize", "10");

        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        multiValueMap.add("pageNumber", "0");
        multiValueMap.add("pageSize", "10");

        mockMvc.perform(get("/bvn/user/nibss?pageNumber=0&&pageSize=10")
//                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .contextPath("/bvn")
                .header(EncryptionHeader.AUTHORIZATION.getName(),token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

    }

}
*/
