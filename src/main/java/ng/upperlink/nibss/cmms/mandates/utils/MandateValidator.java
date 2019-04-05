package ng.upperlink.nibss.cmms.mandates.utils;

import ng.upperlink.nibss.cmms.dto.mandates.MandateRequest;
import ng.upperlink.nibss.cmms.enums.Errors;
import ng.upperlink.nibss.cmms.errorHandler.ErrorDetails;
import ng.upperlink.nibss.cmms.mandates.exceptions.CustomGenericException;
import ng.upperlink.nibss.cmms.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/*
* This class handles the validation of mandates - debit frequency, start date and end date
* */
@Component
public class MandateValidator{

    private static final Logger logger = LoggerFactory.getLogger(MandateValidator.class);

    public static void verify(MandateRequest req) throws CustomGenericException{
        if(req.getFrequency() == null)
            throw new CustomGenericException("Please provide the debit frequency");

        if(StringUtils.isEmpty(req.getMandateStartDate()))
            throw new CustomGenericException("Please provide the start date.");

        if(StringUtils.isEmpty(req.getMandateEndDate()))
            throw new CustomGenericException("Please provide the end date.");

    }

    //validate start date, end date and debit frequency for correctness
    public ResponseEntity validate(MandateRequest mandateReq) {
        logger.info("Doing validation...");

        logger.trace("mandate.getFrequency() "+ mandateReq.getFrequency());
        logger.trace("mandate.getMandateStartDate() "+ mandateReq.getMandateStartDate());

        verify(mandateReq);

        if(mandateReq.getFrequency() > 0){
            logger.info("Trying to validate period and frequency");
            String sDate = mandateReq.getMandateStartDate();
            String eDate = mandateReq.getMandateEndDate();

            SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd");

            try {
                Date startDate = sdf.parse(sDate);
                Date endDate= sdf.parse(eDate);

                if(startDate.compareTo(DateUtils.nullifyTime(new Date())) <= 0){
                    return new ResponseEntity<Object>(new ErrorDetails("Start date cannot be today or less!"), HttpStatus.BAD_REQUEST);
                } else {
                    long difference = endDate.getTime() - startDate.getTime();

                    difference = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS);

                    logger.info("Date difference between start and end date is " + difference);

                    if(difference < (mandateReq.getFrequency() * 7)){
                        return new ResponseEntity<Object>(new ErrorDetails("Mandate date range must be able to accommodate debit frequency!"), HttpStatus.BAD_REQUEST);
                    }

                }
            } catch (ParseException e) {
                logger.error(null,e);
                return new ResponseEntity<Object>(new ErrorDetails("Unable to compute debit frequency and period!"), HttpStatus.BAD_REQUEST);
            }catch(Exception e){
                logger.error("Unknown error thrown",e);
                return new ResponseEntity<Object>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return null;
    }


}
