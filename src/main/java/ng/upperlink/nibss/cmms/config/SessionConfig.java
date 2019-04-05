package ng.upperlink.nibss.cmms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

/**
 * To enable session management with Redis server.
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 10000000, redisNamespace = "mysession")
public class SessionConfig extends AbstractHttpSessionApplicationInitializer {

}