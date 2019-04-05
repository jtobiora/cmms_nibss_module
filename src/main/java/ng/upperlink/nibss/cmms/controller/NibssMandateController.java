package ng.upperlink.nibss.cmms.controller;


import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.UserDetail;
import ng.upperlink.nibss.cmms.dto.mandates.MandateActionRequest;
import ng.upperlink.nibss.cmms.dto.mandates.MandateRequest;
import ng.upperlink.nibss.cmms.dto.mandates.MandateResponse;
import ng.upperlink.nibss.cmms.dto.mandates.RejectionRequests;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.enums.Errors;
import ng.upperlink.nibss.cmms.enums.MandateStatusType;
import ng.upperlink.nibss.cmms.enums.RoleName;
import ng.upperlink.nibss.cmms.errorHandler.ErrorDetails;
import ng.upperlink.nibss.cmms.mandates.utils.MandateValidator;
import ng.upperlink.nibss.cmms.model.NibssUser;
import ng.upperlink.nibss.cmms.model.User;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.service.UserService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateStatusService;
import ng.upperlink.nibss.cmms.service.nibss.NibssUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.stream.Collectors;

/*
* Handles creation of mandates by NIBSS
*
* */
@RestController
@RequestMapping("/nibss/mandate")
@Slf4j
public class NibssMandateController {
    private MandateValidator mandateValidator;
    private MandateService mandateService;
    private UserService userService;
    private MandateStatusService mandateStatusService;
    private NibssUserService nibssUserService;

   //private Logger log = LoggerFactory.getLogger(NibssMandateController.class);

    @Autowired
    public void setNibssUserService(NibssUserService nibssUserService) {
        this.nibssUserService = nibssUserService;
    }

    @Autowired
    public void setMandateStatusService(MandateStatusService mandateStatusService) {
        this.mandateStatusService = mandateStatusService;
    }

    @Autowired
    public void setMandateValidator(MandateValidator mandateValidator) {
        this.mandateValidator = mandateValidator;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    //@Lazy
    @Autowired
    public void setMandateService(MandateService mandateService) {
        this.mandateService = mandateService;
    }


    /** create mandates by NIBSS
     * @param requestObject
     * @param servletRequest
     * @param result
     * @param userDetail
     * @return
     * @throws Exception
     */
    @PostMapping
    public ResponseEntity createMandate(@Valid @RequestBody MandateRequest requestObject,
                                        HttpServletRequest servletRequest,
                                        BindingResult result,
                                        @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) throws Exception {

        if(requestObject.isFixedAmountMandate()) {
            ResponseEntity validator = mandateValidator.validate(requestObject);
            if(validator != null){
                return validator;
            }
        }

        return mandateService.processSaveUpdate(requestObject, servletRequest, userDetail, false);
    }

    /** update mandates by NIBSS
     * @param requestObject
     * @param servletRequest
     * @param result
     * @param userDetail
     * @return
     */
    @PutMapping
    public ResponseEntity<Object> editMandate(@Valid @RequestBody MandateRequest requestObject,
                                              HttpServletRequest servletRequest,
                                              BindingResult result,
                                              @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        if(requestObject.isFixedAmountMandate()) {
            ResponseEntity validator = mandateValidator.validate(requestObject);
            if(validator != null){
                return validator;
            }
        }

        return mandateService.processSaveUpdate(requestObject, servletRequest, userDetail, true);
    }

    //show all mandates
    @GetMapping
    public ResponseEntity<Object> listAllMandates(@ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail,
                                                  @RequestParam int pageNumber, @RequestParam int pageSize) {

        //nibss can view all mandates on the system
        try {
            User user = userService.get(userDetail.getUserId());
            if (user == null) {
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
            }

            return new ResponseEntity<>(mandateService.listAllMandates(new PageRequest(pageNumber, pageSize)), HttpStatus.OK);
        } catch (Exception ex) {
            log.error("An exception occurred while trying to retrieve all mandates ", ex);
            return new ResponseEntity<>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //view a mandate by id
    @GetMapping("/{mandateId}")
    public ResponseEntity<Object> showMandateById(@PathVariable("mandateId") Long mandateId,
                                                  HttpServletRequest request,
                                                  @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        User user = userService.get(userDetail.getUserId());
        if (user == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        try {
            Mandate mandate = mandateService.getMandateByMandateId(mandateId);

            if (mandate == null) {
                return new ResponseEntity<>(new ErrorDetails("The mandate was not found!"), HttpStatus.NOT_FOUND);
            }

            String mandateImage = mandateService.getUploadedMandateImage(mandate, request);

            return new ResponseEntity<>(new MandateResponse(mandate, mandateImage), HttpStatus.OK);

        } catch (Exception e) {
            log.error("An exception occurred while trying to retrieve mandate with id: {}", mandateId, e);
            return new ResponseEntity<>(new ErrorDetails("Mandate details could not be retrieved."), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //suspend, activate and delete mandates
    @PutMapping("/action")
    public ResponseEntity<Object> actOnMandates(@RequestBody MandateActionRequest req,
                                                @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        User userOperator = userService.get(userDetail.getUserId());
        if (userOperator == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        Mandate mandate = mandateService.getMandateByMandateId(req.getMandateId());

        if (mandate == null) {
            return new ResponseEntity<Object>(new ErrorDetails("Mandate not Found!"), HttpStatus.NOT_FOUND);
        }

        String role = userOperator.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

        if (!mandateService.verifyUserRole(new String[]{RoleName.NIBSS_INITIATOR.getValue(), RoleName.NIBSS_AUTHORIZER.getValue()}, role)) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
        }

        int action = Integer.parseInt(req.getAction());
        switch (action) {
            //TODO -- can NIBSS delete mandates created by other users such as banks and billers
            case 3:
                //mandate can only be deleted if its status is NIBSS_BILLER_INITIATE_MANDATE
                if ((mandate.getStatus().getId() == Constants.NIBSS_BILLER_INITIATE_MANDATE ||
                        mandate.getStatus().getId() == Constants.NIBSS_REJECT_MANDATE)) {
                    return mandateService.performMandateOperations(userOperator, mandate, "delete");
                } else {
                    return new ResponseEntity<Object>(new ErrorDetails("Only rejected and NIBSS initiated mandates can be deleted by this user!"), HttpStatus.BAD_REQUEST);
                }

            case 2:
                //Suspend mandate only if active
                if (mandate.getRequestStatus() == Constants.STATUS_ACTIVE) {
                    return mandateService.performMandateOperations(userOperator, mandate, "suspend");
                } else {
                    return new ResponseEntity<Object>(new ErrorDetails("Mandate is currently suspended!"), HttpStatus.BAD_REQUEST);
                }

            case 1:
                //activate mandate only if suspended
                if (mandate.getRequestStatus() == Constants.STATUS_MANDATE_SUSPENDED) {
                    return mandateService.performMandateOperations(userOperator, mandate, "activate");
                } else {
                    return new ResponseEntity<>(new ErrorDetails("Mandate is currently activated!"), HttpStatus.BAD_REQUEST);
                }

            default:
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()));
        }
    }

    //approve mandates
    @PutMapping("/approve")
    public ResponseEntity<Object> approveMandates(HttpServletRequest request,
                                                  @RequestBody MandateActionRequest reqBody,
                                                  @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        User userOperator = userService.get(userDetail.getUserId());
        if (userOperator == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        String role = userOperator.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

        if (!mandateService.verifyUserRole(new String[]{RoleName.NIBSS_AUTHORIZER.getValue()}, role)) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
        }

        Mandate mandate = mandateService.getMandateByMandateId(reqBody.getMandateId());

        if (mandate == null) {
            return new ResponseEntity<>(new ErrorDetails("Mandate not Found!"), HttpStatus.NOT_FOUND);
        }

        switch (reqBody.getAction()) {
            case "approve":
                //if the mandate has already been initiated by a NIBSS initiator, then proceed to approve
                if (mandate.getStatus().getStatusName() == MandateStatusType.NIBSS_BILLER_INITIATE_MANDATE) {
                    return mandateService.processMandateByNIBBS(mandate, reqBody.getAction(), request, userOperator, null);
                }
            default:
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.DATA_NOT_PROVIDED.getValue()));
        }
    }

    //reject a mandate by id
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

        if (!mandateService.verifyUserRole(new String[]{RoleName.NIBSS_AUTHORIZER.getValue()}, role)) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
        }

        Mandate mandate = mandateService.getMandateByMandateId(mandateId);

        return mandate == null ? new ResponseEntity<>(new ErrorDetails("Mandate not Found!"), HttpStatus.NOT_FOUND) :
                mandateService.processMandateByNIBBS(mandate, "reject", request, userOperator, rejectionRequest);
    }

    //show a mandate by status
    @GetMapping("/show")
    public ResponseEntity<Object> showMandatesByStatus(
            @RequestParam("operation") String operation,
            @RequestParam int pageNumber, @RequestParam int pageSize,
            @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        try{
            User user = userService.get(userDetail.getUserId());
            if (user == null) {
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
            }

            String requestStatus = "";
            Long statusType = null;
            Page<Mandate> mandates = null;

            //user logged in
            if (user instanceof NibssUser) {
                if (operation.equalsIgnoreCase("rejected")) {
                    //rejected requests
                    statusType = Constants.NIBSS_REJECT_MANDATE.longValue();
                } else if (operation.equalsIgnoreCase("authorized")) {
                    //authorized requests
                    statusType = Constants.NIBSS_BILLER_AUTHORIZE_MANDATE.longValue();
                } else if (operation.equals("approved")) {
                    //pending requests
                    statusType = Constants.NIBSS_BILLER_INITIATE_MANDATE.longValue();
                    requestStatus = "nibss_initiate_mandate";
                } else if (operation.equalsIgnoreCase("suspended")) {
                    //suspended requests
                    requestStatus = "nibss_suspended_mandate";
                }

                NibssUser nibssUser = nibssUserService.get(userDetail.getUserId());
                mandates = mandateService.getMandatesByNIBBSAndStatus(statusType, new PageRequest(pageNumber, pageSize), nibssUser, requestStatus);

                return new ResponseEntity<>(mandates, HttpStatus.OK);
            }
            return new ResponseEntity<>(mandates, HttpStatus.BAD_REQUEST);
        }catch(Exception e){
            log.error("Exception thrown while trying to fetch mandates for view ",e);
            return new ResponseEntity<>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}