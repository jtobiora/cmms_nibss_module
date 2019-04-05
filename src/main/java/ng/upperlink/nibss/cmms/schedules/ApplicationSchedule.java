/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ng.upperlink.nibss.cmms.schedules;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.enums.Channel;
import ng.upperlink.nibss.cmms.model.Transaction;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.service.QueueService;
import ng.upperlink.nibss.cmms.service.TransactionService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateService;
import ng.upperlink.nibss.cmms.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Megafu Charles <noniboycharsy@gmail.com>
 */
@Component
@Slf4j
public class ApplicationSchedule {

    @Autowired
    private MandateService mandateService;

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private QueueService queueService;
    
    @Value("${initiate.mandate.transaction.topic}")
    private String paymentTopic;

    @Value("${initiate.mandate.advice.topic}")
    private String mandateAdviseTopic;

    @Value("${mandate.advice.retrials.count}")
    private Integer retrialCount;


    /**
     * Migrate due mandates to the transactions table
     */
    @Scheduled(cron = "${move.daily.due.mandates}")
    public void moveDueDailyMandates() {
        log.trace("Starting the migration of due mandates to the transaction table (::)");
        Instant start = Instant.now();
        List<? extends Mandate> dueMandates = mandateService.getAllDueMandates();

        log.info(".: The total number of mandates fetched for the date {} is {}", CommonUtils.convertDateToString(new Date(), "yyyy-MM-dd HH:mm:ss"), dueMandates.size());
        dueMandates.stream().forEach((m) -> {
            // calculate fee and process transaction

            transactionService.processTransaction(m, Channel.PORTAL);
            Date nextDebitDate = CommonUtils.nextDebitDate(m.getNextDebitDate(), m.getFrequency()); // calculate the next debit date
            log.info("The next debit date for the mandate code {} is {}; startDate :: {}; endDate :: {}; currentDebitDate :: {};", m.getMandateCode(), m.getStartDate(), m.getEndDate(),
                    CommonUtils.convertDateToString(nextDebitDate, "yyyy-MM-dd HH:mm:ss"));
            // update the next debit date
            m.setNextDebitDate(nextDebitDate);
            mandateService.updateMandate(m);
        });
        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMinutes();
        log.info("The migration of due mandates to the transaction table took {} minutes", timeElapsed);
    }

    /**
     * Process Freshly Migrated Mandates
     */
    @Scheduled(cron = "${post.fresh.transactions}")
    public void postFreshTransactions() {
        log.trace("Starting the processing of fresh transactions (::)");
        Instant start = Instant.now();

        List<Transaction> transactions = transactionService.getFreshTransactions();
        log.trace("The total number of fresh transactions spooled from the database: {}", transactions.size());
        transactions.parallelStream().forEach((t) -> {
//            log.trace("Sending the transaction with ID {} and mandate code {} to the queue", t.getId(), t.getMandate().getMandateCode());
            queueService.sendTransactionToQueue(String.valueOf(t.getId()), paymentTopic); // sends this transaction to the mandate transaction topic
        });

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMinutes();
        log.trace("The processing of fresh transactions took {} minutes", timeElapsed);
    }

    /**
     * Process Previous Failed Transactions with a retrial count of 1
     */
    @Scheduled(cron = "${first.transaction.posting.retrial}")
    public void firstTransactionPostingRetrial() {
        log.trace("Starting the processing of first transactions retrials");
        Instant start = Instant.now();

        List<Transaction> transactions = transactionService.getPreviousDaysTransactions(new Date(), 1, 1, 0);
        log.trace("The total number of failed transactions for the first retrial is {}", transactions.size());
        transactions.parallelStream().forEach((t) -> {
//            log.trace("Sending previous day first retrial with ID {} and mandate code {} to the queue", t.getId(), t.getMandate().getMandateCode());
            queueService.sendTransactionToQueue(String.valueOf(t.getId()), paymentTopic); // sends this transaction to the mandate transaction topic
        });

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMinutes();
        log.trace("The processing of first retrial transactions took {} minutes", timeElapsed);
    }
    /**
     * Process Previous day transactions with a retrial count of 2
     */
    @Scheduled(cron = "${second.transaction.posting.retrials}")
    public void secondTransactionPostRetrial() {
        log.trace("Starting the processing of second transactions retrials (::)");
        Instant start = Instant.now();

        List<Transaction> transactions = transactionService.getPreviousDaysTransactions(new Date(), 1, 2, 0);
        log.trace("The total number of failed transactions for the second retrial is {}", transactions.size());
        transactions.parallelStream().forEach((t) -> {
//            log.trace("Sending previous day second retrial with ID {} and mandate code {} to the queue", t.getId(), t.getMandate().getMandateCode());
            queueService.sendTransactionToQueue(String.valueOf(t.getId()), paymentTopic); // sends this transaction to the mandate transaction topic
        });

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMinutes();
        log.trace("The processing of second retrial transactions took {} minutes", timeElapsed);
    }


    /** TODO: Include the two more failed transactions retrials for the past two days **/


    /**
     *  Retry mandates whose mandate advice responses were not successful
     */
    @Scheduled(cron = "${mandate.advice.retrials}")
    public void retryMandateAdvice(){
        log.info("Retrying mandates with unsuccessful mandate advice ---");
        Instant start = Instant.now();

        List<Mandate> failedMandates = mandateService.getMandatesWithFailedMandateAdvise(new Long(retrialCount));

        log.info("The total number of mandates with unapproved mandate advice is {}", failedMandates.size());

        failedMandates.parallelStream().forEach((m) -> {
            String mandateInString = CommonUtils.convertObjectToJson(m);
            queueService.send(mandateAdviseTopic,mandateInString);
        });

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMinutes();
        log.info("Retrial took {} minutes", timeElapsed);
    }


}
