package ng.upperlink.nibss.cmms.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.audit.WebAuditProcessor;
import ng.upperlink.nibss.cmms.model.User;
import ng.upperlink.nibss.cmms.repo.UserRepo;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/*
* customize Entity Manager
*
* */
@Configuration
@EnableJpaRepositories(basePackageClasses = UserRepo.class)
@Slf4j
public class RepositoryConfiguration {

    private DataSource dataSource;
    public RepositoryConfiguration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    //entity manager factory
    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(JpaProperties jpaProperties, WebAuditProcessor auditProcessor) {
        LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();

        bean.setPackagesToScan(User.class.getPackage().getName());
        bean.setJpaVendorAdapter(createVendorAdapter(jpaProperties));
        bean.setDataSource(dataSource);

        Map<String, Object> properties = new HashMap<>();
        Map<String, String> hibernateProperties = jpaProperties.getHibernateProperties(dataSource);

        if (null != hibernateProperties) {
            properties.putAll(hibernateProperties);
        }


        properties.put("hibernate.session_factory.interceptor", customHibernateInterceptor(auditProcessor));
        bean.getJpaPropertyMap().putAll(properties);
        return bean;
    }


    private JpaVendorAdapter createVendorAdapter(JpaProperties jpaProperties) {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();

        adapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        adapter.getJpaPropertyMap().putAll(jpaProperties.getProperties());

        adapter.setShowSql(jpaProperties.isShowSql());

        return adapter;
    }


    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }



    public EmptyInterceptor customHibernateInterceptor(WebAuditProcessor webAuditProcessor) {
        return new EmptyInterceptor() {

            @Override
            public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
                log.info("Entity Deleted: {}", entity.getClass().getSimpleName());
                log.info("Full JSON Object {}", convertToJson(entity));

                webAuditProcessor.captureDeleteAction(entity, id, state, propertyNames);
            }

            @Override
            public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {

//                log.info("Entity Updated: {}", entity.getClass().getSimpleName());
//                log.info("Full JSON Object {}", convertToJson(entity));
//                log.info("Current State: {}", Arrays.toString(currentState));
//                log.info("Previous State: {}", Arrays.toString(previousState));
//                log.info("Property Names: {}", Arrays.toString(propertyNames));

               // webAuditProcessor.captureUpdateAction(entity,id, currentState, previousState, propertyNames);
                return false;
            }

            @Override
            public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {

                log.info("Entity Saved: {}", entity.getClass().getSimpleName());
                log.info("Full JSON Object {}", convertToJson(entity));
                log.info("Object State: {}", Arrays.toString(state));
                log.info("Object Properties: {}", Arrays.toString(propertyNames));

                webAuditProcessor.captureInsertAction(entity,id,state, propertyNames);
                return false;
            }

            @Override
            public void postFlush(Iterator entities) {
               webAuditProcessor.saveAll();
            }
        };
    }

    private String convertToJson(Object object) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert audit object to JSON", e);
            return null;
        }
    }

}
