package ng.upperlink.nibss.cmms.controller;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.enums.Channel;
import ng.upperlink.nibss.cmms.enums.MandateCategory;
import ng.upperlink.nibss.cmms.enums.MandateFrequency;
import ng.upperlink.nibss.cmms.enums.MandateRequestType;
import ng.upperlink.nibss.cmms.errorHandler.ErrorDetails;
import ng.upperlink.nibss.cmms.service.QueueService;
import ng.upperlink.nibss.cmms.service.TransactionService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
* handles common functions of the project for mandates
* */
@RestController
@Slf4j
public class CommonsController {
    @Autowired
    private MandateService mandateService;
    @Autowired
    private QueueService queueService;

    @Autowired
    private TransactionService transactionService;

    @Value("${initiate.mandate.transaction.topic}")
    private String paymentTopic;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${activemq.delivery.retrial}")
    private int messageDeliveryRetrial;

    //view mandate categories
    @GetMapping("/mandateCategories")
    public ResponseEntity<?> getAllMandateCategories() {
        try{
            return ResponseEntity.ok(MandateCategory.getMandateCategories());
        }catch(Exception e){
            return new ResponseEntity<>(new ErrorDetails("Could not fetch mandate category!"), HttpStatus.NO_CONTENT);
        }
    }

    //view mandate types
    @GetMapping("/mandateTypes")
    public ResponseEntity<?> getAllMandateTypes() {
        try{
            return ResponseEntity.ok(MandateRequestType.getMandateTypes());
        }catch (Exception e){
            return new ResponseEntity<>(new ErrorDetails("Could not fetch mandate Type!"), HttpStatus.NO_CONTENT);
        }

    }

    //view mandate channels
    @GetMapping("/mandateChannels")
    public ResponseEntity<?> getAllMandateChannels() {
        try {
            return ResponseEntity.ok(Channel.getMandateChannels());
        }catch(Exception e){
            return new ResponseEntity<>(new ErrorDetails("Could not fetch mandate channels!"), HttpStatus.NO_CONTENT);
        }
    }

    //view mandate frequencies
    @GetMapping("/mandateFrequencies")
    public ResponseEntity<?> getAllFrequencies() {
       try {
           return ResponseEntity.ok(MandateFrequency.getMandateFrequencies());
       }catch (Exception e){
           return new ResponseEntity<>(new ErrorDetails("Could not fetch mandate frequency!"), HttpStatus.NO_CONTENT);
       }
    }

}


