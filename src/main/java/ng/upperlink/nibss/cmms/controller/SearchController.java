package ng.upperlink.nibss.cmms.controller;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.UserDetail;
import ng.upperlink.nibss.cmms.dto.mandates.MandateSearchRequest;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.enums.Errors;
import ng.upperlink.nibss.cmms.errorHandler.ErrorDetails;
import ng.upperlink.nibss.cmms.model.User;
import ng.upperlink.nibss.cmms.model.bank.BankUser;
import ng.upperlink.nibss.cmms.model.biller.BillerUser;
import ng.upperlink.nibss.cmms.model.pssp.PsspUser;
import ng.upperlink.nibss.cmms.repo.SearchRepo;
import ng.upperlink.nibss.cmms.service.UserService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.text.ParseException;

/***
 * Provides search features for mandates
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class SearchController {
    private MandateSearchService mandateSearchService;
    private SearchRepo searchRepo;
    private UserService userService;

    @Value("${nibss-identity-key}")
    private String nibssKey;

    @Autowired
    public void setSearchRepo(SearchRepo searchRepo){
        this.searchRepo = searchRepo;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setMandateSearchService(MandateSearchService mandateSearchService){
        this.mandateSearchService = mandateSearchService;
    }

    /*  *search mandates
     *
     */
    @PostMapping("/mandate")
    public ResponseEntity searchMandates(@Valid @RequestBody MandateSearchRequest r,
                                         @ApiIgnore @RequestAttribute(Constants.USER_DETAIL) UserDetail userDetail,
                                         Pageable pageable) throws ParseException {

      try{
          User user = userService.get(userDetail.getUserId());
          if(user == null){
              return ResponseEntity.badRequest().body(new ErrorDetails("User NOT authorized to view search result."));
          }
          String apiKey = null;
          switch(user.getUserType()){
              case BANK:
                  apiKey = ((BankUser)user).getUserBank().getApiKey();
                  break;
              case BILLER:
                  apiKey = ((BillerUser)user).getBiller().getApiKey();
                  break;
              case PSSP:
                  apiKey = ((PsspUser)user).getPssp().getApiKey();
                  break;
              case NIBSS:
                  apiKey = nibssKey;
                  break;
          }

          return searchRepo.searchMandates(r.getMandateCode(),r.getMandateStartDate(),r.getMandateEndDate(),r.getMandateStatus(),r.getSubscriberCode(),
                  r.getAccName(),r.getAccNumber(),r.getBvn(),r.getEmail(),r.getBankCode(),
                  r.getProductName(),r.getMandateType(),r.getMandateCategory(),r.getChannel(),r.getAddress(),r.getPayerName(),r.getAmount(),r.getFrequency(),apiKey,pageable);

      }catch (Exception ex){
          log.error("Exception thrown while trying to search for mandates ",ex);
          return new ResponseEntity<>(new ErrorDetails(Errors.REQUEST_TERMINATED.getValue()), HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

}
