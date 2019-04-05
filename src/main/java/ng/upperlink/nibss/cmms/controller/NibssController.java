package ng.upperlink.nibss.cmms.controller;


import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.AuthorizationRequest;
import ng.upperlink.nibss.cmms.dto.Id;
import ng.upperlink.nibss.cmms.dto.UserDetail;
import ng.upperlink.nibss.cmms.dto.nibss.NibssRequest;
import ng.upperlink.nibss.cmms.dto.search.UsersSearchRequest;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.enums.Errors;
import ng.upperlink.nibss.cmms.enums.UserType;
import ng.upperlink.nibss.cmms.enums.makerchecker.AuthorizationAction;
import ng.upperlink.nibss.cmms.enums.makerchecker.InitiatorActions;
import ng.upperlink.nibss.cmms.enums.makerchecker.ViewAction;
import ng.upperlink.nibss.cmms.errorHandler.ErrorDetails;
import ng.upperlink.nibss.cmms.exceptions.CMMSException;
import ng.upperlink.nibss.cmms.model.NibssUser;
import ng.upperlink.nibss.cmms.model.User;
import ng.upperlink.nibss.cmms.model.auth.Role;
import ng.upperlink.nibss.cmms.repo.SearchRepo;
import ng.upperlink.nibss.cmms.service.UserService;
import ng.upperlink.nibss.cmms.service.auth.RoleService;
import ng.upperlink.nibss.cmms.service.bank.BankService;
import ng.upperlink.nibss.cmms.service.makerchecker.UserAuthorizationService;
import ng.upperlink.nibss.cmms.service.nibss.NibssUserService;
import ng.upperlink.nibss.cmms.util.email.SmtpMailSender;
import ng.upperlink.nibss.cmms.util.encryption.EncyptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

/*
*  Handles NIBSS users' creation, update, toggle and makerchecker actions
* */
@Slf4j
@RestController
@RequestMapping("/user/nibss")
public class NibssController {

    private NibssUserService nibssUserService;

    private UserService userService;
    private RoleService roleService;
    private SmtpMailSender smtpMailSender;
    private UserAuthorizationService userAuthorizationService;
    private BankService bankService;
    private SearchRepo searchRepo;


    @Value("${email_from}")
    private String fromEmail;

    @Value("${encryption.salt}")
    private String salt;

    private UserType userType = UserType.NIBSS;

    @Autowired
    public void setSearchRepo(SearchRepo searchRepo) {
        this.searchRepo = searchRepo;
    }

    @Autowired
    public void setBankService(BankService bankService) {
        this.bankService = bankService;
    }

    @Autowired
    public void setAuthorizationService(UserAuthorizationService userAuthorizationService) {
        this.userAuthorizationService = userAuthorizationService;
    }

    @Autowired
    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }

    @Autowired
    public void setNibssUserService(NibssUserService nibssUserService) {
        this.nibssUserService = nibssUserService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setSmtpMailSender(SmtpMailSender smtpMailSender) {
        this.smtpMailSender = smtpMailSender;
    }


    /** get all NIBSS users
     * @param pageNumber
     * @param pageSize
     * @return
     */
    @GetMapping
    public ResponseEntity getNibssUsers(@RequestParam int pageNumber, @RequestParam int pageSize) {
        try{
            return ResponseEntity.ok(nibssUserService.get(new PageRequest(pageNumber, pageSize)));
        }catch(Exception ex){
            log.error("NIBSS users could not be retrieved",ex);
            ex.printStackTrace();
            return new ResponseEntity<>(new ErrorDetails("Unable to load NIBSS users"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/pageable-object")
    public ResponseEntity getPageableObject(Pageable pageable){
        return ResponseEntity.ok(nibssUserService.getNibssUsersPageable(pageable));
    }

    @GetMapping("/all")
    public ResponseEntity getNibssUsers() {

        try {
            return ResponseEntity.ok(nibssUserService.getAllNIBSSUsers());
        }catch (Exception ex){
            log.error("NIBSS users could not be retrieved ",ex);
            return new ResponseEntity<>(new ErrorDetails("Unable to load NIBSS users"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Get all active NIBSS users
     * @param activeStatus
     * @return
     */
    @GetMapping("/activated")
    public ResponseEntity getAllActiveUsers(@RequestParam boolean activeStatus){
        try {
            return ResponseEntity.ok(nibssUserService.getUsersByActiveStatus(activeStatus));
        } catch (Exception ex) {
            log.error("NIBSS users could not be retrieved",ex);
            return new ResponseEntity<>(new ErrorDetails("Unable to load NIBSS users."), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /** Get all Autorized, Pending & Rejected NIBSS users
     * @param viewAction
     * @param pageNumber
     * @param pageSize
     * @return
     */
    @GetMapping("/viewAction")
    public ResponseEntity getAllPendingUsers(@RequestParam ViewAction viewAction, @RequestParam int pageNumber, @RequestParam int pageSize) {
        try{
            return ResponseEntity.ok(nibssUserService.selectView(viewAction, new PageRequest(pageNumber, pageSize)));
        }catch (Exception ex){
            log.error("Retrieving of NIBSS users failed ",ex);
            return new ResponseEntity<>(new ErrorDetails("Unable to load users."), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** View all NIBSS users whose updates have been done but are not yet authorized
     * @param id
     * @param userDetail
     * @return
     */
    @GetMapping("/previewUpdate")
    public ResponseEntity previewUpdate(@RequestParam Long id,
                                        @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {
        try {
            return ResponseEntity.status(201).body(nibssUserService.previewUpdate(id));
        } catch (CMMSException e) {
            log.error("Exception thrown while previewing updates ",e);
            return ErrorDetails.setUpErrors("Unable to preview", Arrays.asList(e.getMessage()),e.getCode());
        }catch(Exception ex){
            log.error("Failed to preview data for NIBSS users ",ex);
            return new ResponseEntity<>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Get NIBSS users by id
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity getNibssUser(@PathVariable Long id) {
        try{
            NibssUser nibssUser = nibssUserService.get(id);
            if (nibssUser == null) {
                return ErrorDetails.setUpErrors("Unable to retrieve", Arrays.asList("No user was found"),"404");
            } else {
                return ResponseEntity.ok(nibssUserService.get(id));
            }
        }catch(Exception e){
            log.error("Retrieving of NIBSS user with id {} failed ",id,e);
            return new ResponseEntity<>(new ErrorDetails("Unable to load user."), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Create NIBSS user
     * @param request
     * @param userDetail
     * @return
     */
    @PostMapping
    public ResponseEntity createNibssUser(@Valid @RequestBody NibssRequest request,
                                          @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        try {
            nibssUserService.validate(request, false, null);
            Role newUserRole = roleService.get(request.getRoleId());
            if (newUserRole == null) {
                return ErrorDetails.setUpErrors("Unable to create", Arrays.asList("Role not found, please try again"), "404");
            }
            User operatorUser = userService.get(userDetail.getUserId());
            if (operatorUser == null) {
                return ErrorDetails.setUpErrors("Unable to create", Arrays.asList(Errors.UNKNOWN_USER.getValue()),"404");
            }

            NibssUser nibss = null;
            nibss = nibssUserService.generate(new NibssUser(), null, request, operatorUser, false, userType);
            if (nibss == null)
                return ErrorDetails.setUpErrors("Unable to create", Arrays.asList(Errors.UNKNOWN_USER.getValue()),"404");


            String password = this.userService.generatePassword();
            if (!password.isEmpty())
                nibss.setPassword(EncyptionUtil.doSHA512Encryption(password, this.salt));

            nibss = nibssUserService.save(nibss);

            //send email
//            nibssUserService.sendAwarenessEmail(nibss, password, request.getLoginURL(), false, fromEmail);
            return ResponseEntity.ok(nibss);
        } catch (CMMSException e) {
            log.error("Exception thrown ",e);
            return ErrorDetails.setUpErrors("Unable to create", Arrays.asList(e.getMessage()),e.getCode());
        }  catch (IOException e) {
            log.error("Exception thrown ",e);
            return ErrorDetails.setUpErrors("Unable to create", Arrays.asList("The create action failed. Error: " + e.getMessage()),"500");
        }catch(Exception ex){
            log.error("could not create a user",ex);
            return ErrorDetails.setUpErrors("Unable to create user", Arrays.asList("Processing failed. Please try again."),"500");
        }

    }


    /** update NIBSS user details
     * @param request
     * @param userDetail
     * @return
     */
    @PutMapping
    public ResponseEntity updateNibssUser(@Valid @RequestBody NibssRequest request,
                                          @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {


        try {
            //validate the request
            nibssUserService.validate(request, true, request.getUserId());
            //get the user logged in
            User operatorUser = userService.get(userDetail.getUserId());
            if (operatorUser == null) {
                return ErrorDetails.setUpErrors("Unable to update", Arrays.asList(Errors.UNKNOWN_USER.getValue()),"404");
            }

            //get the user to be updated
            NibssUser existingUser = nibssUserService.get(request.getUserId());
            if (existingUser == null) {
                return ErrorDetails.setUpErrors("Unable to update", Arrays.asList("Invalid user Id."),"404");
            }
            existingUser = nibssUserService.generate(new NibssUser(), existingUser, request, operatorUser, true, userType);
            existingUser = nibssUserService.save(existingUser);

            //send email
            nibssUserService.sendAwarenessEmail(existingUser, existingUser.getPassword(), request.getLoginURL(), true, fromEmail);
            return ResponseEntity.ok(existingUser);
        } catch (IOException e) {
            log.error("Exception thrown while updating a user ", e);
            return ErrorDetails.setUpErrors("Unable to update", Arrays.asList("Processing failed. Please try again."),"500");
        } catch (CMMSException e) {
            log.error("Exception thrown while updating a user ", e);
            return ErrorDetails.setUpErrors("Unable to update", Arrays.asList(e.getMessage()),e.getCode());
        }catch(Exception ex){
            log.error("could not update a user",ex);
            return ErrorDetails.setUpErrors("Unable to update user", Arrays.asList("Unable to update user."),"500");
        }
    }

    /** Authorize NIBSS user
     * @param request
     * @param action
     * @param userDetail
     * @return
     */
    @PutMapping("/authorization")
    public ResponseEntity authorizationAction(@Valid @RequestBody AuthorizationRequest request, @RequestParam AuthorizationAction action,
                                              @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {

        try {
            return ResponseEntity.ok(nibssUserService.performAuthorization(request,action, userDetail));
        } catch (CMMSException e) {
            log.error("Error thrown while authorizing a user ",e);
            return ErrorDetails.setUpErrors("Authorization failed", Arrays.asList(e.getMessage()),e.getCode());
        }
    }

    /** Toggle NIBSS user
     * @param request
     * @param userDetail
     * @return
     */
    @PutMapping("/toggle")
    public ResponseEntity toggleNibssUser(@Valid @RequestBody Id request,
                                          @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail) {
        //get the user logged in
        User operatorUser = userService.get(userDetail.getUserId());
        if (operatorUser == null) {
            return ErrorDetails.setUpErrors("Unable to toggle", Arrays.asList(Errors.UNKNOWN_USER.getValue()),"404");
        }

        //get the user to be updated
        NibssUser nibss = nibssUserService.get(request.getId());
        if (nibss == null) {
            return ErrorDetails.setUpErrors("Unable to toggle", Arrays.asList("Invalid user Id."),"404");
        }
        try {
            return ResponseEntity.ok(nibssUserService.toggle(nibss, operatorUser, null, InitiatorActions.TOGGLE));
        } catch (IOException e) {
            log.error("Error thrown while toggling a user ",e);
            return ErrorDetails.setUpErrors("Unable to toggle", Arrays.asList("Toggle action failed. Error:" + e.getMessage()),"500");
        } catch (CMMSException e) {
            log.error("Toggle failed ",e);
            return ErrorDetails.setUpErrors("Unable to toggle", Arrays.asList(e.getMessage()),e.getCode());
        }catch(Exception ex){
            log.error("Unable to toggle user ", ex);
            return ErrorDetails.setUpErrors("Unable to toggle user", Arrays.asList(ex.getMessage()),"500");
        }
    }

    /** Search NIBSS user details
     * @param request
     * @param pageable
     * @return
     * @throws ParseException
     */
    @PostMapping("/search")
    public ResponseEntity searchNibssUsers(@RequestBody UsersSearchRequest request,
                                           Pageable pageable) throws ParseException{

        return ResponseEntity.ok(nibssUserService.doSearch(request,pageable));

    }

}
