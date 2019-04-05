/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ng.upperlink.nibss.cmms.controller;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.*;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.enums.Errors;
import ng.upperlink.nibss.cmms.enums.TransactionStatus;
import ng.upperlink.nibss.cmms.errorHandler.ErrorDetails;
import ng.upperlink.nibss.cmms.model.Transaction;
import ng.upperlink.nibss.cmms.model.User;
import ng.upperlink.nibss.cmms.service.BillingHelper;
import ng.upperlink.nibss.cmms.service.BillingProvider;
import ng.upperlink.nibss.cmms.service.TransactionService;
import ng.upperlink.nibss.cmms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author Megafu Charles <noniboycharsy@gmail.com>
 */
@RestController
@RequestMapping("/transactions")
@Slf4j
public class TransactionController {
    
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private UserService userService;
    @Autowired
    private BillingProvider billingService;
    @Autowired
    private BillingHelper billerHelperService;
    
    @Value("${billing.payment.temporary.folder}")
    private String cmmsTempBillingFolder;
    
    @GetMapping("/details")
    public ResponseEntity getTransactionDetails(@RequestParam int pageNumber, @RequestParam int pageSize,
                                                                              @RequestParam(value = "status", required = true) String status,
                                                                              @DateTimeFormat(pattern = Constants.TRANSACTION_DETAILS_DATE_FORMAT) @RequestParam(value = "startDate", required = false) Date from,
                                                                              @DateTimeFormat(pattern = Constants.TRANSACTION_DETAILS_DATE_FORMAT) @RequestParam(value = "endDate", required = false) Date to,
                                                                              @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {
        User user = userService.get(userDetail.getUserId());
        
        if (user == null) 
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        
        try {
            Page<TransactionDetail> result = transactionService.processTransactionDetails(user, () -> TransactionStatus.valueOf(status), to, from, new PageRequest(pageNumber, pageSize));
            log.trace("The result is: {}", null == result ? "No Result" : result.getSize());
            return ResponseEntity.ok(new TransactionDetailResponse<>(to, from, result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Get Transaction Summary
     * @param type
     * @param summaryType
     * @param range
     * @param userDetail
     * @return 
     */
    @GetMapping("/summary/{type}/{summaryType}/{range}")
    public ResponseEntity getTransactionSummary(@PathVariable String type, @PathVariable String summaryType, 
                                                @PathVariable int range, 
                                                @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {
        
        log.trace("The type provided is: {}", type);
        log.trace("The provided date summary type is: {}", summaryType);
        log.trace("The provided date range is: {}", range);
        
        User user = userService.get(userDetail.getUserId());
        
        if (user == null) 
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        
        if (!type.trim().equals(Constants.SUCCESSFUL_SUMMARY) && !type.trim().equals(Constants.UNSUCCESSFUL_SUMMARY))
            return ResponseEntity.badRequest().body(Constants.INVALID_SUMMARY_TYPE);
        
        if (!summaryType.trim().equals(Constants.MONTH_TYPE_SUMMARY) && !summaryType.trim().equals(Constants.YEAR_TYPE_SUMMARY))
            return ResponseEntity.badRequest().body(Constants.INVALID_DATE_SUMMARY_TYPE);
        
        if (range <= 0)
            return ResponseEntity.badRequest().body(Constants.INVALID_SUMMARY_DATE_RANGE);
        
        Future<List<TransactionSummaryDto>> processTransactions = null;
        try {
            processTransactions = transactionService.getTransactionSummaryByRole(user, 
                                                                                 () -> type.equals(Constants.SUCCESSFUL_SUMMARY) ? TransactionStatus.PAYMENT_SUCCESSFUL : TransactionStatus.PAYMENT_FAILED,
                                                                                 () -> getDateSummary(summaryType, range));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        
        try {
            return ResponseEntity.ok(processTransactions.get());
        } catch (InterruptedException e) {
            log.error("The process for transaction summary was interrupted for type {}", type, e);
        } catch (ExecutionException e) {
            log.error("An execution exception occurred while fetching transaction summary for type {}", type, e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("System malfunction, please try again");
    }
    
    @GetMapping("/billing")
    public ResponseEntity<Object> downloadBillingFile(@DateTimeFormat(pattern = Constants.TRANSACTION_DETAILS_DATE_FORMAT) @RequestParam(value = "startDate", required = true) Date from,
                                                      @DateTimeFormat(pattern = Constants.TRANSACTION_DETAILS_DATE_FORMAT) @RequestParam(value = "endDate", required = true) Date to) {
        try {
            List<Transaction> transactions = transactionService.getTransactionBilling(from, to);
            List<NIBSSPayPayment> nibssPayment = transactionService.getNIBSSPaymentBilling(to, from);
            
            if (null == transactions || transactions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK).body("No Transactions Found");
            }
            
            Path billingFilePath = billingService.getBillingZipFile(transactions, nibssPayment, cmmsTempBillingFolder, BillingProvider.BillingPeriod.WEEKLY);
            billingService.cleanUp(transactions, nibssPayment);
            
            if (null != billingFilePath) {
                Resource file = billerHelperService.downloadBillingReport(billingFilePath);
                return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"" + file.getFilename() + "\"").body(file);
            }
            
        } catch (IOException | RuntimeException e) {
            log.error("Could not generate billing file", e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to generate billing file");
    }
    
    @GetMapping("/billing/list")
    public ResponseEntity billingList(@DateTimeFormat(pattern = Constants.TRANSACTION_DETAILS_DATE_FORMAT) @RequestParam(value = "startDate", required = true) Date from,
                                                      @DateTimeFormat(pattern = Constants.TRANSACTION_DETAILS_DATE_FORMAT) @RequestParam(value = "endDate", required = true) Date to) {

        List<Transaction> transactions = transactionService.getTransactionBilling(from, to);

        if (null == transactions || transactions.isEmpty()) {
            return ResponseEntity.ok("No Transactions Found");
        }

        return ResponseEntity.ok().body(transactionService.getBanksBilling(transactions));
    }
    
    
    
    private Date getDateSummary(String summaryType, int range) {
        if (summaryType.trim().equals(Constants.MONTH_TYPE_SUMMARY)) {
            LocalDateTime monthDateTime = LocalDateTime.of(LocalDate.now().minusMonths(range), LocalTime.MIN);
            return Timestamp.valueOf(monthDateTime);
        } else if (summaryType.trim().equals(Constants.YEAR_TYPE_SUMMARY)) {
            LocalDateTime monthDateTime = LocalDateTime.of(LocalDate.now().minusYears(range), LocalTime.MIN);
            return Timestamp.valueOf(monthDateTime);
        }
        return new Date();
    }
}
