package ng.upperlink.nibss.cmms.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.event.AuditEvent;
import ng.upperlink.nibss.cmms.model.Auditable;
import ng.upperlink.nibss.cmms.model.WebAppAuditEntry;
import ng.upperlink.nibss.cmms.model.WebAuditAction;
import ng.upperlink.nibss.cmms.util.CommonUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/*
* Processes web audit
* */
@Service
@Slf4j
public class WebAuditProcessor {

    private final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final List<WebAppAuditEntry> entries;

    ThreadLocal<DateFormat> dateFormatThreadLocal = new ThreadLocal() {
        @Override
        public DateFormat get() {
            return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
        }
    };

    private ObjectMapper objectMapper;
    private ApplicationEventPublisher publisher;


    public WebAuditProcessor(ObjectMapper objectMapper, ApplicationEventPublisher publisher) {

        this.objectMapper = objectMapper;
        this.publisher = publisher;
        entries = new ArrayList<>();

    }

    //audit delete action
    public void captureDeleteAction(Object entity, Object entityId, Object[] state, String[] propertyNames) {
        persistAction(WebAuditAction.DELETED, entity, entityId, state, propertyNames);
    }


    //audit insert action
    public void captureInsertAction(Object entity, Object entityId, Object[] state, String[] propertyNames) {
        persistAction(WebAuditAction.CREATED, entity, entityId, state, propertyNames);
    }

    //audit update action
    public void captureUpdateAction(Object entity, Object entityId, Object[] currentState, Object[] previousState,
                                    String[] propertyNames) {


        if(!isAuditable(entity)) {
            return;
        }

        Map<String, String> oldObjectMap = extractObjectMap(previousState, propertyNames);
        Map<String, String> newObjectMap = extractObjectMap(currentState, propertyNames);

        if (null == oldObjectMap || null == newObjectMap)
            return;

        WebAppAuditEntry entry = createEntry(entity, entityId, WebAuditAction.UPDATED);

        try {
            entry.setOldObject(objectMapper.writeValueAsString(oldObjectMap));
            entry.setNewObject(objectMapper.writeValueAsString(newObjectMap));
        } catch (JsonProcessingException e) {
            log.error("could not convert object maps to JSON", e);
        }

       entries.add(entry);
    }

    //audit persist action
    private void persistAction(WebAuditAction action, Object entity, Object entityId, Object[] state, String[] propertyNames) {

        if(!isAuditable(entity)) {
            return;
        }

        Map<String, String> objectMap = extractObjectMap(state, propertyNames);
        if (null == objectMap)
            return;

        WebAppAuditEntry entry = createEntry(entity, entityId, action);

        try {
            String entityValue = objectMapper.writeValueAsString(objectMap);
            if(action == WebAuditAction.CREATED) {
                entry.setNewObject(entityValue);
            } else {
                entry.setOldObject(entityValue);
            }
        } catch (JsonProcessingException e) {
            log.error("could not generate object map", e);
        }

        entries.add(entry);

    }


    /** Verify if the entity is Auditable i.e if it has the annotation @Auditable on it
     * @param entity
     * @return
     */
    private boolean isAuditable(Object entity) {
        return entity.getClass().isAnnotationPresent(Auditable.class);
    }

    private Map<String, String> extractObjectMap(Object[] state, String[] propetyNames) {
        if (null == state || null == propetyNames || state.length == 0 || propetyNames.length == 0)
            return null;

        if (propetyNames.length != state.length)
            return null;

        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < propetyNames.length; i++) {
            Object temp = state[i];
            if (temp instanceof Date) {
                map.put(propetyNames[i], dateFormatThreadLocal.get().format((Date) temp));
            } else {
                map.put(propetyNames[i], Objects.toString(temp));
            }
        }

        return map;
    }


    /** Creates an entry to be saved in the database
     * @param entity
     * @param entityId
     * @param action
     * @return
     */
    private WebAppAuditEntry createEntry(Object entity, Object entityId, WebAuditAction action) {
        WebAppAuditEntry entry = new WebAppAuditEntry();
        entry.setAction(action);
        entry.setClassName(entity.getClass().getName());
        entry.setEntityId(CommonUtils.convertObjectToJson(entityId));
        entry.setUser(getCurrentUser());

        return entry;
    }

    //Save all user details
    public void saveAll() {
        CompletableFuture<Void> process = CompletableFuture.runAsync(() -> {
            for (WebAppAuditEntry entry : entries) {
                publisher.publishEvent(new AuditEvent(entry));
            }
            entries.clear();
        });
    }


    /** Gets the current authenticated user
     * @return
     */
    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if( null == authentication)
            return null;
        if (authentication.getName().trim().equals(Constants.DEFAULT_SPRING_SECURITY_USERNAME))
            throw new IllegalStateException("There is no valid user logged in, ensure you are successfully logged into the portal before carrying out this operation");
        return authentication.getName();
    }

}
