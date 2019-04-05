/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ng.upperlink.nibss.cmms.schedules;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.NIBSSPayPayment;
import ng.upperlink.nibss.cmms.model.Transaction;
import ng.upperlink.nibss.cmms.service.BillingHelper;
import ng.upperlink.nibss.cmms.service.BillingProvider;
import ng.upperlink.nibss.cmms.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Megafu Charles <noniboycharsy@gmail.com>
 */
@Component
@Slf4j
public class BillingSchedule {
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private BillingProvider billingService;
    
    @Autowired
    private BillingHelper billingHelperService;
    
    @Value("${billing.payment.folder}")
    private String cmmsBillingPath;
    
    /**
     * This schedule handles the generation of the billing file every saturday
     */
    @Scheduled(cron = "${cmms.billing.time}")
    public void doCmmsBilling() {
        try {
            LocalDateTime startDateTime = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN);
            LocalDateTime endDateTime = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MAX);
            Date startDate = Timestamp.valueOf(startDateTime);
            Date endDate = Timestamp.valueOf(endDateTime);

            List<Transaction> transactions = transactionService.getTransactionWeeklyBilling(startDate, endDate);
            List<NIBSSPayPayment> nibssPayment = transactionService.getNIBSSPaymentWeeklyBilling(startDate, endDate);
//            List<NIBSSPayPayment> nibssDebitPayment = transactionService.getNIBSSDebitPaymentWeeklyBilling(startDate, endDate);

            if (null == transactions || transactions.isEmpty())
                return;

            Path billingFilePath = billingService.getBillingZipFile(transactions, nibssPayment, cmmsBillingPath, BillingProvider.BillingPeriod.WEEKLY);

            if (null != billingFilePath) // update the billing status of the transactions
                transactionService.updateSelectedTransactions(transactions);

            billingService.cleanUp(transactions, nibssPayment);
        } catch (IOException | RuntimeException e) {
            log.error("Could not generate billing report for CMMS", e);
        }
    }

    /**
     * Clean up temporary files
     */
    @Scheduled(cron = "${cmms.clean.up.temp.folder}")
    public void cleanUpOldFiles() {
        billingHelperService.cleanUpOldFiles();
    }
}
