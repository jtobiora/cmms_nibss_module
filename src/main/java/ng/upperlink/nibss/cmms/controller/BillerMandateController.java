package ng.upperlink.nibss.cmms.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.UserDetail;
import ng.upperlink.nibss.cmms.dto.mandates.MandateActionRequest;
import ng.upperlink.nibss.cmms.dto.mandates.MandateRequest;
import ng.upperlink.nibss.cmms.dto.mandates.MandateResponse;
import ng.upperlink.nibss.cmms.dto.mandates.RejectionRequests;
import ng.upperlink.nibss.cmms.enums.*;
import ng.upperlink.nibss.cmms.errorHandler.ErrorDetails;
import ng.upperlink.nibss.cmms.mandates.exceptions.CustomGenericException;
import ng.upperlink.nibss.cmms.mandates.exceptions.ExcelReaderException;
import ng.upperlink.nibss.cmms.mandates.utils.ExcelParser;
import ng.upperlink.nibss.cmms.mandates.utils.MandateValidator;
import ng.upperlink.nibss.cmms.model.User;
import ng.upperlink.nibss.cmms.model.biller.Biller;
import ng.upperlink.nibss.cmms.model.biller.BillerUser;
import ng.upperlink.nibss.cmms.model.mandate.BulkMandate;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.service.UserService;
import ng.upperlink.nibss.cmms.service.bank.BankService;
import ng.upperlink.nibss.cmms.service.bank.BankUserService;
import ng.upperlink.nibss.cmms.service.biller.BillerService;
import ng.upperlink.nibss.cmms.service.biller.BillerUserService;
import ng.upperlink.nibss.cmms.service.biller.ProductService;
import ng.upperlink.nibss.cmms.service.mandateImpl.*;
import ng.upperlink.nibss.cmms.util.MandateUtils;
import ng.upperlink.nibss.cmms.view.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/*
*
* Handles mandates initiated from the billers' end
*
* */
@RestController
@Slf4j
@RequestMapping("/biller/mandate")
public class BillerMandateController {

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
    private RestTemplate restTemplate;
    private RejectionService rejectionService;
    private ExcelParser excelParser;
    private BulkMandateService bulkMandateService;

    @Value("${excel.file.path}")
    private String excelFilePath;

    private static final int status = Constants.STATUS_MANDATE_DELETED;

    @Autowired
    public void setBulkMandateService(BulkMandateService bulkMandateService) {
        this.bulkMandateService = bulkMandateService;
    }

    @Autowired
    public void setExcelParser(ExcelParser excelParser){
        this.excelParser = excelParser;
    }

    @Autowired
    public void setRejectionService(RejectionService rejectionService){
        this.rejectionService = rejectionService;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    @Autowired
    public void setBillerUserService(BillerUserService billerUserService){
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

   // @Lazy
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


    /** create mandates from billers' end
     * @param requestObject
     * @param servletRequest
     * @param userDetail
     * @return
     * @throws Exception
     */
    @PostMapping("/add")
    @JsonView(Views.MandateView.class)
    public ResponseEntity createMandate(@Valid @RequestBody MandateRequest requestObject,
                                        HttpServletRequest servletRequest,
                                        @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) throws Exception{

        if(requestObject.isFixedAmountMandate()){
            ResponseEntity validator = mandateValidator.validate(requestObject);
            if(validator != null){
                return validator;
            }
        }

        return mandateService.processSaveUpdate(requestObject,servletRequest,userDetail,false);
    }

    /** Update mandates
     * @param requestObject
     * @param servletRequest
     * @param userDetail
     * @return
     */
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

    /** List all mandates by created by a biller
     * @param userDetail
     * @param pageNumber
     * @param pageSize
     * @return
     */
    @GetMapping("/listAll")
    @JsonView(Views.MandateView.class)
    public ResponseEntity<Object> listMandatesByBiller(@ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail,
                                                       @RequestParam int pageNumber, @RequestParam int pageSize){

        try {
            User user = userService.get(userDetail.getUserId());
            if (user == null) {
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
            }

            Biller biller = ((BillerUser)user).getBiller();
            List<Long> statusIdList = new ArrayList<>();

            statusIdList.add(Constants.BILLER_INITIATE_MANDATE);

            //get mandates by the biller
            Page<Mandate> listOfMandates = mandateService.getMandatesByBillers(biller.getApiKey(),new PageRequest(pageNumber,pageSize),statusIdList);

            return new ResponseEntity<Object>(listOfMandates,HttpStatus.OK);

        }catch(Exception ex){
            log.error("An exception occurred while trying to retrieve all mandates for view", ex);
            return new ResponseEntity<>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** view mandates by id
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
        BillerUser billerUser = billerUserService.getUserById(userDetail.getUserId());

        if (billerUser == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        try {
            Mandate mandate = mandateService.getMandateByBillerAndMandateId(mandateId, billerUser.getBiller());
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

    /** suspend, activate or delete mandates
     * @param req
     * @param userDetail
     * @return
     */
    @PutMapping("/action")
    @JsonView(Views.MandateView.class)
    public ResponseEntity<Object> actOnMandates(@RequestBody MandateActionRequest req,
                                                @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail){

        User userOperator = userService.get(userDetail.getUserId());
        if (userOperator == null){
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

            Mandate mandate = mandateService.getMandateByMandateId(req.getMandateId());

            if(mandate == null){
                return new ResponseEntity<Object>(new ErrorDetails("Mandate not Found!"),HttpStatus.NOT_FOUND);
            }

            String role = userOperator.getRoles().stream().map( r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

            if(!mandateService.verifyUserRole(new String[]{RoleName.BILLER_INITIATOR.getValue(),RoleName.BILLER_AUTHORIZER.getValue()},role)){
                return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
            }

            int action = Integer.parseInt(req.getAction());
            switch(action){
                case 3:
                    //mandate can only be deleted by a biller initiator if it's status is BILLER_INITIATE_MANDATE or BILLER_REJECT status
                    if((mandate.getStatus().getId() == Constants.BILLER_INITIATE_MANDATE ||
                            mandate.getStatus().getId() == Constants.BILLER_REJECT_MANDATE)){
                        return mandateService.performMandateOperations(userOperator, mandate, "delete");
                    }else{
                        return new ResponseEntity<Object>(new ErrorDetails("Only rejected and biller-initiated mandates can be deleted!"),HttpStatus.BAD_REQUEST);
                    }

                case 2:
                    //Suspend mandate only if active
                    if(mandate.getRequestStatus() == Constants.STATUS_ACTIVE) {
                        return mandateService.performMandateOperations(userOperator, mandate, "suspend");
                    }else{
                        return new ResponseEntity<Object>(new ErrorDetails("Mandate is currently suspended!"),HttpStatus.BAD_REQUEST);
                    }

                case 1:
                    //activate mandate only if suspended
                    if(mandate.getRequestStatus() == Constants.STATUS_MANDATE_SUSPENDED){
                        return mandateService.performMandateOperations(userOperator, mandate, "activate");
                    }else {
                        return new ResponseEntity<Object>(new ErrorDetails("Mandate is currently activated!"),HttpStatus.BAD_REQUEST);
                    }

                default:
                    return ResponseEntity.badRequest().body(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()));
            }
    }


    /** Approve mandates
     * @param request
     * @param reqBody
     * @param userDetail
     * @return
     */
    @PutMapping("/approve")
    @JsonView(Views.MandateView.class)
    public ResponseEntity<Object> approveMandates(HttpServletRequest request,
                                                  @RequestBody MandateActionRequest reqBody,
                                                  @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail){

        User userOperator = userService.get(userDetail.getUserId());
        if (userOperator == null) {
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.UNKNOWN_USER.getValue()));
        }

        String role = userOperator.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

        if(!mandateService.verifyUserRole(new String[]{RoleName.BILLER_AUTHORIZER.getValue()},role)){
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
        }

        Mandate mandate = mandateService.getMandateByMandateId(reqBody.getMandateId());

        if (mandate == null){
            return new ResponseEntity<>(new ErrorDetails("Mandate not Found!"),HttpStatus.NOT_FOUND);
        }

        switch(reqBody.getAction()){
            case "approve":
                return mandateService.processMandateByBillers(mandate, reqBody.getAction(),  request, userOperator,null);
            default:
                return ResponseEntity.badRequest().body(new ErrorDetails("Request cannot be processed!"));
        }
    }

    /** Reject mandates initiated by billers
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

        if(!mandateService.verifyUserRole(new String[]{RoleName.BILLER_AUTHORIZER.getValue()},role)){
            return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
        }

        Mandate mandate = mandateService.getMandateByMandateId(mandateId);

        return mandate == null ? new ResponseEntity<>(new ErrorDetails("Mandate not Found!"),HttpStatus.NOT_FOUND) :
                mandateService.processMandateByBillers(mandate, "reject", request, userOperator,rejectionRequest);
    }

    //get debit frequency
    @GetMapping(value = "/frequencies")
    public ResponseEntity<Object> getMandateFrequencies(){
        return new ResponseEntity<Object>(MandateFrequency.getMandateFrequencies(),HttpStatus.OK);
    }

    //show all mandates that are suspended, approved and rejected
    @GetMapping("/show")
    @JsonView(Views.MandateView.class)
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
           if(user instanceof BillerUser){
               String role = user.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

               if(!mandateService.verifyUserRole(new String[]{RoleName.BILLER_INITIATOR.getValue(),RoleName.BILLER_AUTHORIZER.getValue()},role)){
                   return ResponseEntity.badRequest().body(new ErrorDetails(Errors.NOT_PERMITTED.getValue()));
               }

               if(operation.equalsIgnoreCase("rejected")){
                   //rejected requests
                   statusType = Constants.BILLER_REJECT_MANDATE.longValue();
               }else if(operation.equalsIgnoreCase("authorized")){
                   //authorized requests
                   statusType = Constants.BILLER_AUTHORIZE_MANDATE.longValue();
               }else if(operation.equals("approved")){
                   //pending requests
                   statusType = Constants.BILLER_INITIATE_MANDATE.longValue();
                   requestStatus = "biller_initiate_mandate";
               }else if(operation.equalsIgnoreCase("suspended")){
                   //suspended requests
                   requestStatus = "biller_suspended_mandate";
               }

               BillerUser billerUser = billerUserService.getUserById(userDetail.getUserId());

               if(billerUser == null){
                   return ResponseEntity.badRequest().body(new ErrorDetails("User not found!"));
               }

               mandates = mandateService.getMandatesByBillerAndStatus(statusType,new PageRequest(pageNumber,pageSize),billerUser.getBiller().getApiKey(),requestStatus);

               return new ResponseEntity<>(mandates,HttpStatus.OK);
           }
           return new ResponseEntity<>(mandates,HttpStatus.BAD_REQUEST);
       }catch(Exception ex){
           log.error("Exception thrown while trying to fetch mandates for view ",ex);
           return new ResponseEntity<>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
       }
    }

     //view all the reasons for rejection
    @RequestMapping(value = "/rejectionReasons", method = RequestMethod.GET)
    public ResponseEntity<Object> getMandateRejectedReasons(){
        return new ResponseEntity<>(rejectionReasonsService.getAll(), HttpStatus.OK);
    }

    //get Excel Template for Bulk Mandate Uploads
    //@ApiIgnore
    @GetMapping(value = "/template")
    public ResponseEntity<InputStreamResource> getReportTemplateForBulkCreation(
            @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) throws Exception {

        User user = userService.get(userDetail.getUserId());

        if (user == null) {
            throw new CustomGenericException("User not found!");
        }

        Biller biller = null;

        if (UserType.find(userDetail.getUserType()) == UserType.BILLER) {
            biller = ((BillerUser)user).getBiller();
            return excelParser.getGeneratedExcelFile(userDetail,biller);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    //@ApiIgnore
    //upload Mandate in bulk.
    @PostMapping(value = "/bulk")
    @JsonView(Views.MandateView.class)
    public ResponseEntity createMandateInBulk(@ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail,
                                             @RequestParam("file") MultipartFile file) throws Exception{

        //Allow Billers to upload mandates
        if (UserType.find(userDetail.getUserType()) == UserType.BILLER) {
            User user = userService.get(userDetail.getUserId());
            if (user == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            //get role
            String role = user.getRoles().stream().map(r -> r.getName().getValue()).collect(Collectors.toList()).get(0);

            //Only Biller_Initiators can upload mandates for billers
            String[] permittedRoles = {RoleName.BILLER_INITIATOR.getValue()};

            boolean found = Arrays.asList(permittedRoles).contains(role);

            if (!found) {
                throw new ExcelReaderException(Errors.NOT_PERMITTED.getValue());
            }


            Biller biller = null;
            if(user instanceof BillerUser){
                biller = ((BillerUser)user).getBiller();
            }

            return excelParser.bulkCreation(file,  user,biller,role);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PutMapping("/imageUpdate")
    @JsonView(Views.MandateView.class)
    public ResponseEntity uploadMandateImage(@Valid @RequestBody MandateActionRequest req,
                                             HttpServletRequest servletRequest,@ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail){
        try {
            BulkMandate bulkMandate = bulkMandateService.getByMandateId(req.getMandateId());

            if(bulkMandate == null){
                return new ResponseEntity<Object>(new ErrorDetails("Mandate not Found!"),HttpStatus.NOT_FOUND);
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  //It will ignore all the properties that are not declared.

            Mandate mandate = mapper.readValue(bulkMandate.getMandateInJson(),Mandate.class);

            String mandateCode = MandateUtils.getMandateCode(String.valueOf(System.currentTimeMillis()), mandate.getBiller().getRcNumber(),String.valueOf(mandate.getProduct().getId()));

            mandateService.uploadMandateImage(null,servletRequest,mandateCode,userDetail,mandate,req.getAction());

            //update the mandate id to reflect the one after the last mandate saved
            mandate.setId(mandateService.getMaxMandate() + 1);

            return ResponseEntity.ok(mandateService.saveMandate(mandate));
        } catch(Exception ex){
            log.error("Error in uploading mandate image! ",ex);
            return ResponseEntity.badRequest().body("Error occured while uploading mandate image.");
        }
    }

}
