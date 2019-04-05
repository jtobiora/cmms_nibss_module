/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ng.upperlink.nibss.cmms.config;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.enums.Channel;
import ng.upperlink.nibss.cmms.enums.FeeBearer;
import ng.upperlink.nibss.cmms.enums.SplitType;
import ng.upperlink.nibss.cmms.enums.TransactionStatus;
import ng.upperlink.nibss.cmms.model.Transaction;
import ng.upperlink.nibss.cmms.model.mandate.Fee;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.service.TransactionService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;
import java.util.stream.IntStream;

/**
 *
 * Provide dummy data for Transactions table (testing purposes)
 */
//@Profile("dev")
//@Configuration
@Slf4j
public class DevDataPopulate {
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private MandateService mandateService;
    
    @Bean
    public CommandLineRunner populateDate() {
        return (args) -> {
            Mandate mandate = mandateService.getMandateByMandateId(Long.valueOf(43));
            
            if (null == mandate)
                log.trace("The mandate with the ID {} does not exist");
  
            Set<Fee> billerFee = mandate.getBiller().getFee();
            
            
            //populate successful transactions
            IntStream.range(1, 5).forEach((i) -> {
                Transaction transaction = new Transaction();
                transaction.setMandate(mandate);
                transaction.setAmount(mandate.isFixedAmountMandate() ? mandate.getAmount() : mandate.getVariableAmount()); // determine the nature of mandate
                transaction.setBearer(FeeBearer.SUBSCRIBER);
                transaction.setSplitType(SplitType.FIXED);
                transaction.setBillableAtTransactionTime(true);
                transaction.setFee(new BigDecimal(30));
                transaction.setMarkedUp(false);
                transaction.setBank(mandate.getBank());
                transaction.setAccountName("");
                transaction.setAccountNumber("");
                transaction.setBillerDebitAccountName("");
                transaction.setBillerDebitAccountNumber("");
                transaction.setBillerDebitBank(null);
                transaction.setDateCreated(new Date());
                transaction.setPaymentDate(new Date());
                transaction.setTransactionType(Channel.PORTAL);
                transaction.setStatus(TransactionStatus.PAYMENT_SUCCESSFUL);
                transaction.setSuccessfulSessionId("09494049409490494040498484984");
                transaction.setDefaultFee(new BigDecimal(30));
                transactionService.saveTransaction(transaction);
            });
            
            // popoulate failed transactions
            IntStream.range(1, 5).forEach((i) -> {
                Transaction transaction = new Transaction();
                transaction.setMandate(mandate);
                transaction.setAmount(mandate.isFixedAmountMandate() ? mandate.getAmount() : mandate.getVariableAmount()); // determine the nature of mandate
                transaction.setBearer(FeeBearer.SUBSCRIBER);
                transaction.setSplitType(SplitType.FIXED);
                transaction.setBillableAtTransactionTime(true);
                transaction.setFee(new BigDecimal(30));
                transaction.setMarkedUp(false);
                transaction.setBank(mandate.getBank());
                transaction.setAccountName("");
                transaction.setAccountNumber("");
                transaction.setBillerDebitAccountName("");
                transaction.setBillerDebitAccountNumber("");
                transaction.setBillerDebitBank(null);
                transaction.setDateCreated(new Date());
                transaction.setPaymentDate(new Date());
                transaction.setTransactionType(Channel.PORTAL);
                transaction.setStatus(TransactionStatus.PAYMENT_FAILED);
                transaction.setDefaultFee(new BigDecimal(30));
                transactionService.saveTransaction(transaction);
            });
            
        };
    }
}
