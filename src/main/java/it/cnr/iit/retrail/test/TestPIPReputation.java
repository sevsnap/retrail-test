/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.server.pip.ActionEvent;
import it.cnr.iit.retrail.server.pip.impl.PIP;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public class TestPIPReputation extends PIP {

    public final String category = PepAttribute.CATEGORIES.SUBJECT;
    public final String subjectId = PepAttribute.IDS.SUBJECT;
    private Map<String, String> reputation = new HashMap<>();
    private String attributeId = "reputation";

    public TestPIPReputation() {
        super();
        this.log = LoggerFactory.getLogger(TestPIPReputation.class);
    }

    public Map<String, String> getReputation(){
        return reputation;
    }
    public void setReputation(final Map<String, String> reputation){
        this.reputation = reputation;
    }

    @Override
    public void fireBeforeActionEvent(ActionEvent e) {
        if (e.originState.getType() == StateType.BEGIN) {
            PepAttributeInterface subject = e.request.getAttributes(category, subjectId).iterator().next();
            String rep = getReputation().get(subject.getValue());
            if (reputation != null) {
                log.info("{} - subject {} has reputation {}", e.request, subject.getValue(), rep);
                PepAttributeInterface test = newPrivateAttribute(getAttributeId(), PepAttribute.DATATYPES.STRING, rep, subject);
                // Make attribute unmanaged (automatically managed by the UCon)
                // because we set an expiry date
                test.setExpires(new Date());
                e.request.replace(test);
            } else {
                log.warn("subject {} has no reputation attribute -- ignoring", subject.getValue());
            }
        }
    }

    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }
    
    @Override
    protected void refresh(PepAttributeInterface pepAttribute, PepSessionInterface session) {
        pepAttribute.setExpires(new Date());
    }
}
