package ng.upperlink.nibss.cmms.event;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.service.WebAppAuditEntryService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/*
*  Takes care of Listening for audit events
* */
@Component
@Slf4j
public class AuditEventListener {

    private WebAppAuditEntryService auditEntryService;

    public AuditEventListener(final WebAppAuditEntryService auditEntryService) {

        this.auditEntryService = auditEntryService;
    }

    /*
    *  Log Audit events
    * */
    @Async
    @EventListener
    public void logEvent(AuditEvent event) {
        try {
            auditEntryService.save(event.getAuditEntry());
        } catch (RuntimeException e) {
            log.error("could not save audit",e);

        }

    }
}
