package ng.upperlink.nibss.cmms.service;

import ng.upperlink.nibss.cmms.CmmsNibssApplication;
import ng.upperlink.nibss.cmms.config.email.MailConfigImpl;
import ng.upperlink.nibss.cmms.encryptanddecrypt.NIBSSAESEncryption;
import ng.upperlink.nibss.cmms.service.auth.RoleService;
import ng.upperlink.nibss.cmms.service.contact.CountryService;
import ng.upperlink.nibss.cmms.service.contact.LgaService;
import ng.upperlink.nibss.cmms.service.contact.StateService;
import ng.upperlink.nibss.cmms.util.email.EmailService;
import ng.upperlink.nibss.cmms.util.email.SmtpMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring4.SpringTemplateEngine;


//@RunWith(SpringRunner.class)
//@DataJpaTest

@SpringBootTest(classes = CmmsNibssApplication.class) //this is used because we have more than one main class @SpringBootApplication, one from this project while the other from the lib project but we choose this project's main class.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class NibssServiceTest {

    @TestConfiguration
    static class NibssServiceTestContextConfiguration{

        @Autowired
        private Environment environment;

//        @Bean
//        public NibssUserService nibssUserService(){
//            return new NibssUserService();
//        }
/*
        @Bean
        public AgentTransactionReportService agentTransactionReportService(){
            return new AgentTransactionReportService();
        }*/

        @Bean
        public UserService userService(){
            return new UserService();
        }

        @Bean
        public RoleService roleService(){
            return new RoleService();
        }

        @Bean
        public MailConfigImpl mailConfig(){
            return new MailConfigImpl();
        }

        @Bean
        public EmailService emailService(){
            return new EmailService();
        }

        @Bean
        public SpringTemplateEngine springTemplateEngine(){
            return new SpringTemplateEngine();
        }

        @Bean
        public JavaMailSenderImpl javaMailSender(){
            return new JavaMailSenderImpl();
        }

        @Bean
        public SmtpMailSender smtpMailSender(){
            return new SmtpMailSender(environment,mailConfig(),emailService());
        }

        @Bean
        public LgaService lgaService(){
            return new LgaService();
        }

        @Bean
        public StateService stateService(){
            return new StateService();
        }

        @Bean
        public CountryService countryService(){
            return new CountryService();
        }

        @Bean
        public NIBSSAESEncryption nibssaesEncryption(){
            return new NIBSSAESEncryption();
        }

       // @Bean
        //public SubscriberService agentMgrService(){
       //     return new SubscriberService();
       // }

    }

//    @Autowired
//    private NibssUserService nibssUserService;



/*    @MockBean //this is used to bypass the actual repo object
    private NibssRepo nibssRepo;

    @Before
    public void runBefore(){

        Nibss nibss = new Nibss();
        Mockito.when(nibssRepo.getOne(Long.valueOf("1"))).thenReturn(nibss);
        System.out.println("The nibss response "+ nibss);

    }*/

}
