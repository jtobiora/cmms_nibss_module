package ng.upperlink.nibss.cmms.event;

import lombok.Data;
import ng.upperlink.nibss.cmms.model.WebAppAuditEntry;


@Data
public class AuditEvent {

    private WebAppAuditEntry auditEntry;

    public AuditEvent(WebAppAuditEntry auditEntry) {

        this.auditEntry = auditEntry;
    }
}
