package ng.upperlink.nibss.cmms.controller;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.UserDetail;
import ng.upperlink.nibss.cmms.dto.mandates.MandateActionRequest;
import ng.upperlink.nibss.cmms.dto.mandates.MandateRequest;
import ng.upperlink.nibss.cmms.dto.mandates.MandateResponse;
import ng.upperlink.nibss.cmms.dto.mandates.RejectionRequests;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.enums.Errors;
import ng.upperlink.nibss.cmms.enums.RoleName;
import ng.upperlink.nibss.cmms.errorHandler.ErrorDetails;
import ng.upperlink.nibss.cmms.mandates.utils.MandateValidator;
import ng.upperlink.nibss.cmms.model.User;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.model.pssp.Pssp;
import ng.upperlink.nibss.cmms.model.pssp.PsspUser;
import ng.upperlink.nibss.cmms.service.UserService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/pssp/mandate")
public class PsspMandateController {
    private MandateService mandateService;
    private MandateValidator mandateValidator;
    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setMandateValidator(MandateValidator mandateValidator) {
        this.mandateValidator = mandateValidator;
    }

    //@Lazy
    @Autowired
    public void setMandateService(MandateService mandateService) {
        this.mandateService = mandateService;
    }

    @PostMapping
    public ResponseEntity createMandate(@Valid @RequestBody MandateRequest requestObject,
                                        HttpServletRequest servletRequest,
                                        @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) throws Exception{

        if(requestObject.isFixedAmountMandate()) {
            ResponseEntity validator = mandateValidator.validate(requestObject);
            if(validator != null){
                return validator;
            }
        }

        return mandateService.processSaveUpdate(requestObject,servletRequest,userDetail,false);
    }

    @PutMapping
    public ResponseEntity<Object> editMandate(@Valid @RequestBody MandateRequest requestObject,
                                              HttpServletRequest servletRequest,
                                              @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail){

        if(requestObject.isFixedAmountMandate()) {
            ResponseEntity validator = mandateValidator.validate(requestObject);
            if(validator != null){
                return validator;
            }
        }

        return mandateService.processSaveUpdate(requestObject,servletRequest,userDetail,true);
    }

    @GetMapping
    public ResponseEntity<Object> listMandatesByPSSP(@ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail,
                                                       @RequestParam int pageNumber, @RequestParam int pageSize){

        try {
            User user = userService.get(userDetail.getUserId());
            if (user == null) {
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
            }

            Pssp pssp = ((PsspUser)user).getPssp();

            String role = user.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);
            List<Long> statusIdList = new ArrayList<>();

            if(!mandateService.verifyUserRole(new String[]{RoleName.PSSP_INITIATOR.getValue(),RoleName.PSSP_AUTHORIZER.getValue()},role)){
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
            }

            statusIdList.add(Constants.PSSP_INITIATE_MANDATE);

            //get mandates by the PSSP
            Page<Mandate> listOfMandates = mandateService.getMandatesByPSSP(pssp.getApiKey(),new PageRequest(pageNumber,pageSize),statusIdList);

            return new ResponseEntity<Object>(listOfMandates, HttpStatus.OK);

        }catch(Exception ex){
            log.error("An exception occurred while trying to retrieve all mandates for view", ex);
            return new ResponseEntity<>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{mandateId}")
    public ResponseEntity<Object> showMandateById(@PathVariable("mandateId") Long mandateId,
                                                  HttpServletRequest request,
                                                  @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        User user = userService.get(userDetail.getUserId());
        if (user == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        Pssp pssp = ((PsspUser)user).getPssp();
        if (pssp == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails("PSSP not found."));
        }

        try {
            Mandate mandate = mandateService.getByPSSPAndMandateId(mandateId, pssp.getApiKey());
            if(mandate == null){
                return new ResponseEntity<>(new ErrorDetails("The mandate was not found!"),HttpStatus.NOT_FOUND);
            }

            String mandateImage =  mandateService.getUploadedMandateImage(mandate,request);

            return new ResponseEntity<>(new MandateResponse(mandate,mandateImage), HttpStatus.OK);

        } catch (Exception e) {
            log.error("Exception caught while retrieving mandate with id {} ",mandateId, e.getMessage());
            return new ResponseEntity<>(new ErrorDetails("Mandate details could not be retrieved."), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PutMapping("/action")
    public ResponseEntity<Object> actOnMandates(@RequestBody MandateActionRequest req,
                                                @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail){

        User userOperator = userService.get(userDetail.getUserId());
        if (userOperator == null){
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        Mandate mandate = mandateService.getMandateByMandateId(req.getMandateId());

        if(mandate == null){
            return new ResponseEntity<>(new ErrorDetails("Mandate not Found!"),HttpStatus.NOT_FOUND);
        }

        String role = userOperator.getRoles().stream().map( r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

        if(!mandateService.verifyUserRole(new String[]{RoleName.PSSP_INITIATOR.getValue(),RoleName.PSSP_AUTHORIZER.getValue()},role)){
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
        }

        int action = Integer.parseInt(req.getAction());
        switch(action){
            case 3:
                //mandate can only be deleted by a pssp initiator if it's status is PSSP_INITIATE_MANDATE or PSSP_REJECT status
                if((mandate.getStatus().getId() == Constants.PSSP_INITIATE_MANDATE ||
                        mandate.getStatus().getId() == Constants.PSSP_REJECT_MANDATE)){
                    return mandateService.performMandateOperations(userOperator, mandate, "delete");
                }else{
                    return new ResponseEntity<>(new ErrorDetails("Only rejected and PSSP initiated mandates can be deleted!"),HttpStatus.BAD_REQUEST);
                }

            case 2:
                //Suspend mandate only if active
                if(mandate.getRequestStatus() == Constants.STATUS_ACTIVE) {
                    return mandateService.performMandateOperations(userOperator, mandate, "suspend");
                }else{
                    return new ResponseEntity<>(new ErrorDetails("Mandate is currently suspended!"),HttpStatus.BAD_REQUEST);
                }

            case 1:
                //activate mandate only if suspended
                if(mandate.getRequestStatus() == Constants.STATUS_MANDATE_SUSPENDED){
                    return mandateService.performMandateOperations(userOperator, mandate, "activate");
                }else {
                    return new ResponseEntity<>(new ErrorDetails("Mandate is currently activated!"),HttpStatus.BAD_REQUEST);
                }

            default:
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()));
        }
    }

    @PutMapping("/approve")
    public ResponseEntity<Object> approveMandates(HttpServletRequest request,
                                                  @RequestBody MandateActionRequest reqBody,
                                                  @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail){

        User userOperator = userService.get(userDetail.getUserId());
        if (userOperator == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        String role = userOperator.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

        if(!mandateService.verifyUserRole(new String[]{RoleName.PSSP_AUTHORIZER.getValue()},role)){
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
        }

        Mandate mandate = mandateService.getMandateByMandateId(reqBody.getMandateId());

        if (mandate == null){
            return new ResponseEntity<>(new ErrorDetails("Mandate not Found!"),HttpStatus.NOT_FOUND);
        }

        switch(reqBody.getAction()){
            case "approve":
                //if the mandate has already been initiated by a pssp initiator, then proceed to approve
                if(mandate.getStatus().getId() == Constants.PSSP_INITIATE_MANDATE){
                    return mandateService.processMandateByPSSP(mandate, reqBody.getAction(),  request, userOperator,null);
                }
            default:
                return ResponseEntity.badRequest().body(new ErrorDetails("Request processing failed. Please try again."));
        }
    }

    @PutMapping(value = "/reject/{mandateId}")
    public ResponseEntity<Object> rejectMandate(
            @RequestBody RejectionRequests rejectionRequest,
            @PathVariable("mandateId") Long mandateId,
            HttpServletRequest request,
            @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        User userOperator = userService.get(userDetail.getUserId());

        if (userOperator == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        String role = userOperator.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

        if(!mandateService.verifyUserRole(new String[]{RoleName.PSSP_AUTHORIZER.getValue()},role)){
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
        }

        Mandate mandate = mandateService.getMandateByMandateId(mandateId);

        return mandate == null ? new ResponseEntity<>(new ErrorDetails("Mandate not Found!"),HttpStatus.NOT_FOUND) :
                mandateService.processMandateByPSSP(mandate, "reject", request, userOperator,rejectionRequest);
    }

    @GetMapping("/show")
    public ResponseEntity<Object> showMandatesByStatus(
            @RequestParam("operation") String operation,
            @RequestParam int pageNumber, @RequestParam int pageSize,
            @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail){

        try {
            User user = userService.get(userDetail.getUserId());
            if (user == null) {
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
            }

            String requestStatus = "";
            Long statusType = null;
            Page<Mandate> mandates = null;

            //user logged in
            if(user instanceof PsspUser){
                String role = user.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

                if(!mandateService.verifyUserRole(new String[]{RoleName.PSSP_INITIATOR.getValue(),RoleName.PSSP_AUTHORIZER.getValue()},role)){
                    return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
                }

                if(operation.equalsIgnoreCase("rejected")){
                    //rejected requests
                    statusType = Constants.PSSP_REJECT_MANDATE.longValue();
                }else if(operation.equalsIgnoreCase("authorized")){
                    //authorized requests
                    statusType = Constants.PSSP_AUTHORIZE_MANDATE.longValue();
                }else if(operation.equals("approved")){
                    //pending requests
                    statusType = Constants.PSSP_INITIATE_MANDATE.longValue();
                    requestStatus = "pssp_initiate_mandate";
                }else if(operation.equalsIgnoreCase("suspended")){
                    //suspended requests
                    requestStatus = "pssp_suspended_mandate";
                }

                Pssp pssp = ((PsspUser) user).getPssp();

                if(pssp == null){
                    return ResponseEntity.badRequest().body(new ErrorDetails("PSSP not found!"));
                }

                mandates = mandateService.getMandatesByPSSPAndStatus(statusType,new PageRequest(pageNumber,pageSize),pssp.getApiKey(),requestStatus);

                return new ResponseEntity<>(mandates,HttpStatus.OK);
            }
            return new ResponseEntity<>(mandates,HttpStatus.BAD_REQUEST);
        }catch(Exception ex){
            log.error("Exception thrown while trying to fetch mandates for view ",ex);
            return new ResponseEntity<>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
