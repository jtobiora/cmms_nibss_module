package ng.upperlink.nibss.cmms.controller;

import com.fasterxml.jackson.annotation.JsonView;
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
import ng.upperlink.nibss.cmms.model.bank.Bank;
import ng.upperlink.nibss.cmms.model.bank.BankUser;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.repo.MandateRepo;
import ng.upperlink.nibss.cmms.service.UserService;
import ng.upperlink.nibss.cmms.service.bank.BankService;
import ng.upperlink.nibss.cmms.service.bank.BankUserService;
import ng.upperlink.nibss.cmms.service.biller.BillerService;
import ng.upperlink.nibss.cmms.service.biller.BillerUserService;
import ng.upperlink.nibss.cmms.service.biller.ProductService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateStatusService;
import ng.upperlink.nibss.cmms.service.mandateImpl.RejectionReasonsService;
import ng.upperlink.nibss.cmms.util.FileUploadUtils;
import ng.upperlink.nibss.cmms.view.Views;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/*
*
* Handles mandates initiated from the bank
*
* */
@RestController
@RequestMapping("/bank/mandate")
@Slf4j
public class BankMandateController {
    //private Logger log = LoggerFactory.getLogger(BankMandateController.class);
    private ProductService productService;
    private BankService bankService;
    private BankUserService bankUserService;
    private BillerService billerService;
    private MandateService mandateService;
    private MandateStatusService mandateStatusService;
    private UserService userService;
    private RejectionReasonsService rejectionReasonsService;
    private MandateValidator mandateValidator;
    private BillerUserService billerUserService;
    private MandateRepo mandateRepo;
    private FileUploadUtils fileUploadUtils;

    @Autowired
    public void setFileUploadUtils(FileUploadUtils fileUploadUtils) {
        this.fileUploadUtils = fileUploadUtils;
    }

    @Autowired
    public void setMandateRepo(MandateRepo mandateRepo) {
        this.mandateRepo = mandateRepo;
    }

    @Autowired
    public void setBillerUserService(BillerUserService billerUserService) {
        this.billerUserService = billerUserService;
    }

    @Autowired
    public void setMandateValidator(MandateValidator mandateValidator) {
        this.mandateValidator = mandateValidator;
    }

    @Autowired
    public void setRejectionReasonsService(RejectionReasonsService rejectionReasonsService) {
        this.rejectionReasonsService = rejectionReasonsService;
    }

    @Autowired
    public void setBillerService(BillerService billerService) {
        this.billerService = billerService;
    }

    @Autowired
    public void setBankUserService(BankUserService bankUserService) {
        this.bankUserService = bankUserService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setBankService(BankService bankService) {
        this.bankService = bankService;
    }

    //@Lazy
    @Autowired
    public void setMandateService(MandateService mandateService) {
        this.mandateService = mandateService;
    }

    @Autowired
    public void setMandateStatusService(MandateStatusService mandateStatusService) {
        this.mandateStatusService = mandateStatusService;
    }

    @Autowired
    public void setProductService(ProductService productService) {
        this.productService = productService;
    }


    //Create mandates by bank_biller_initiator => for bank billers and other billers recruited by the bank
    @JsonView(Views.MandateView.class)
    @PostMapping("/add")
    public ResponseEntity createMandate(@Valid @RequestBody MandateRequest requestObject,
                                        HttpServletRequest servletRequest,
                                        @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) throws Exception {

        if(requestObject.isFixedAmountMandate()) {
            ResponseEntity validator = mandateValidator.validate(requestObject);
            if(validator != null){
                return validator;
            }
        }

        return mandateService.processSaveUpdate(requestObject, servletRequest, userDetail, false);

    }

    //Update mandate details
    @PutMapping("/edit")
    @JsonView(Views.MandateView.class)
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


    /**Approve mandates from billers and banks
     * @param req
     * @param request
     * @param userDetail
     * @return
     */
    @PutMapping("/approve")
    @JsonView(Views.MandateView.class)
    public ResponseEntity<Object> approveMandates(@RequestBody MandateActionRequest req,
                                                  HttpServletRequest request,
                                                  @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        User userOperator = userService.get(userDetail.getUserId());
        if (userOperator == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        String role = userOperator.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

        if (!mandateService.verifyUserRole(new String[]{RoleName.BANK_INITIATOR.getValue(), RoleName.BANK_AUTHORIZER.getValue(), RoleName.BANK_BILLER_AUTHORIZER.getValue()}, role)) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
        }

        return mandateService.processMandateByBanks(req.getMandateId(), req.getAction(), request, userOperator, null);
    }


    /** list all mandates by banks
     * @param userDetail
     * @param pageNumber
     * @param pageSize
     * @return
     */
    @GetMapping("/listAll")
    @JsonView(Views.MandateView.class)
    public ResponseEntity<Object> listMandatesByBanks(@ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail,
                                                      @RequestParam int pageNumber, @RequestParam int pageSize) {

       try{
           User user = userService.get(userDetail.getUserId());

           if (user == null) {
               return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
           }

           Bank bank = ((BankUser) user).getUserBank();
           if(bank == null){
               return ResponseEntity.badRequest().body(new ErrorDetails("Bank was not found!"));
           }

           String role = user.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

           List<Long> statusIdList = new ArrayList<>();

           //each user sees the mandates that apply to them
           if (role.equalsIgnoreCase(RoleName.BANK_BILLER_AUTHORIZER.getValue()) || role.equalsIgnoreCase(RoleName.BANK_BILLER_INITIATOR.getValue())) {
               statusIdList.add(Constants.BANK_BILLER_INITIATE_MANDATE);
           } else if (role.equalsIgnoreCase(RoleName.BANK_INITIATOR.getValue())) {
               statusIdList.add(Constants.BILLER_AUTHORIZE_MANDATE);
               statusIdList.add(Constants.BANK_BILLER_AUTHORIZE_MANDATE);
           } else if (role.equalsIgnoreCase(RoleName.BANK_AUTHORIZER.getValue())) {
               statusIdList.add(Constants.BANK_AUTHORIZE_MANDATE);
           }

           Page<Mandate> mandates = mandateService.getAllMandates(bank.getApiKey(),bank.getId(), new PageRequest(pageNumber, pageSize), statusIdList);

           return new ResponseEntity<Object>(mandates, HttpStatus.OK);
       }catch(Exception ex){
           log.error("Exception thrown while trying to fetch mandates for view ",ex);
           return new ResponseEntity<>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
       }

    }

    /** view mandate by Id
     * @param mandateId
     * @param request
     * @param userDetail
     * @return
     */
    @GetMapping("/view/{mandateId}")
    @JsonView(Views.MandateView.class)
    public ResponseEntity<Object> showMandateById(@PathVariable("mandateId") Long mandateId,
                                                  HttpServletRequest request,
                                                  @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        //get the current user
        BankUser bankUser = bankUserService.getById(userDetail.getUserId());

        if (bankUser == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        //get the user bank
        Bank userBank = bankUser.getUserBank();

        try {
            Mandate mandate = mandateService.getMandateByBankAndMandateId(mandateId, userBank);

            if(mandate == null){
                return new ResponseEntity<Object>(new ErrorDetails("The mandate was not found!"),HttpStatus.NOT_FOUND);
            }

            String mandateImage = mandateService.getUploadedMandateImage(mandate,request);

            return new ResponseEntity<Object>(new MandateResponse(mandate,mandateImage), HttpStatus.OK);
           // return new ResponseEntity<Object>(mandateService.getMandateByBankAndMandateId(mandateId, userBank), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Unable to fetch mandate with id {}", mandateId,e);
            return new ResponseEntity<>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**  Reject mandates initiated by banks
     * @param rejectionRequest
     * @param mandateId
     * @param request
     * @param userDetail
     * @return
     */
    @PutMapping(value = "/reject/{mandateId}")
    @JsonView(Views.MandateView.class)
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

        if(!mandateService.verifyUserRole(new String[]{RoleName.BANK_INITIATOR.getValue(),RoleName.BANK_BILLER_AUTHORIZER.getValue(), RoleName.BANK_AUTHORIZER.getValue()},role)){
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
        }

        return mandateService.rejectMandatesByBanks(mandateId, "reject", request, userOperator, rejectionRequest);
    }

    /** Suspend, activate or delete mandates
     * @param req
     * @param userDetail
     * @return
     */
    @PutMapping("/action")
    public ResponseEntity<Object> workOnMandates(@RequestBody MandateActionRequest req,
                                                 @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        User userOperator = userService.get(userDetail.getUserId());
        if (userOperator == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        Mandate mandate = mandateService.getMandateByMandateId(req.getMandateId());

        if(mandate == null) {
            return new ResponseEntity<Object>(new ErrorDetails("Mandate not Found!"),HttpStatus.NOT_FOUND);
        }

        String role = userOperator.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

        if(!mandateService.verifyUserRole(new String[]{RoleName.BANK_INITIATOR.getValue(), RoleName.BANK_BILLER_INITIATOR.getValue(),
                RoleName.BANK_BILLER_AUTHORIZER.getValue(),RoleName.BANK_AUTHORIZER.getValue()},role)){
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
        }


        int action = Integer.parseInt(req.getAction());
        switch (action) {
            case 3:
                //delete operation can be performed only if status is BANK_REJECT_MANDATE or BANK_BILLER_INITIATE_MANDATE
                if ((mandate.getStatus().getId() == Constants.BANK_BILLER_INITIATE_MANDATE || mandate.getStatus().getId() == Constants.BANK_REJECT_MANDATE)) {
                    return mandateService.performMandateOperations(userOperator, mandate, "delete");
                } else {
                    return new ResponseEntity<Object>(new ErrorDetails("Only mandates with status 'BANK REJECTED' or 'BANK INITIATED' can be deleted!"), HttpStatus.BAD_REQUEST);
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
                    return new ResponseEntity<Object>(new ErrorDetails("Mandate is currently activated!"), HttpStatus.BAD_REQUEST);
                }
            default:
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()));
        }
    }


    /** view mandates by the status of the mandate
     * @param operation
     * @param pageNumber
     * @param pageSize
     * @param userDetail
     * @return
     */
    @GetMapping("/show")
    @JsonView(Views.MandateView.class)
    public ResponseEntity<Object> showMandatesByStatus(
            @RequestParam("operation") String operation,
            @RequestParam int pageNumber, @RequestParam int pageSize,
            @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {


        try {
            User user = userService.get(userDetail.getUserId());
            if (user == null) {
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
            }

            String role = user.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

            String[] permittedRoles = {RoleName.BANK_INITIATOR.getValue(), RoleName.BANK_AUTHORIZER.getValue(), RoleName.BANK_BILLER_AUTHORIZER.getValue()};

            boolean found = Arrays.asList(permittedRoles).contains(role);

            if (!found) {
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
            }

            String requestStatus = "";
            Long statusType = null;
            Page<Mandate> mandates = null;

            if (user instanceof BankUser) {
                if (role.equalsIgnoreCase(RoleName.BANK_INITIATOR.getValue())) {
                    if (operation.equals("rejected")) {
                        //rejected requests
                        statusType = Constants.BANK_REJECT_MANDATE.longValue();
                    } else if (operation.equals("authorized")) {
                        //authorized requests
                        statusType = Constants.BANK_AUTHORIZE_MANDATE.longValue();
                    } else if (operation.equals("approved")) {
                        //pending requests
                        statusType = Constants.BILLER_AUTHORIZE_MANDATE.longValue(); //or BANK_BILLER_AUTHORIZE_MANDATE
                        requestStatus = "pending_mandate";
                    } else if (operation.equals("suspended")) {
                        //suspended requests
                        requestStatus = "suspended_mandate";
                    }
                } else if (role.equalsIgnoreCase(RoleName.BANK_BILLER_AUTHORIZER.getValue())) {
                    if (operation.equals("rejected")) {
                        //list of rejected requests
                        statusType = Constants.BANK_REJECT_MANDATE.longValue();
                    } else if (operation.equals("authorized")) {
                        //list of authorized requests
                        statusType = Constants.BANK_BILLER_AUTHORIZE_MANDATE.longValue();
                    } else if (operation.equals("approved")) {
                        //list of pending requests
                        statusType = Constants.BANK_BILLER_INITIATE_MANDATE.longValue();
                        requestStatus = "pending_mandate";
                    } else if (operation.equals("suspended")) {
                        //list of suspended requests
                        requestStatus = "suspended_mandate";
                    }
                } else if (role.equalsIgnoreCase(RoleName.BANK_AUTHORIZER.getValue())) {
                    if (operation.equals("rejected")) {
                        //rejected requests
                        statusType = Constants.BANK_REJECT_MANDATE.longValue();
                    } else if (operation.equals("authorized")) {
                        //approved requests
                        statusType = Constants.BANK_APPROVE_MANDATE.longValue();
                    } else if (operation.equals("approved")) {
                        //pending requests
                        statusType = Constants.BANK_AUTHORIZE_MANDATE.longValue();
                        requestStatus = "pending_mandate";
                    } else if (operation.equals("suspended")) {
                        //suspended requests
                        requestStatus = "suspended_mandate";
                    }
                }


                BankUser bankUser = bankUserService.getById(userDetail.getUserId());
                mandates = mandateService.getMandatesByBanksAndStatus(role, statusType, new PageRequest(pageNumber, pageSize), bankUser.getUserBank(), requestStatus);
                return new ResponseEntity<Object>(mandates, HttpStatus.OK);

            }
            return new ResponseEntity<Object>(mandates, HttpStatus.BAD_REQUEST);
        }catch(Exception ex){
            log.error("Exception thrown while trying to fetch mandates for view ",ex);
            return new ResponseEntity<>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
