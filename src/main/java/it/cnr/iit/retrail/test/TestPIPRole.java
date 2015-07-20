/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author Francesco
 */
public class TestPIPRole extends PIP{

    public final String category = PepAttribute.CATEGORIES.SUBJECT;
    public final String subjectId = PepAttribute.IDS.SUBJECT;
    private Map<String, String> role = new HashMap<>();
    private String attributeId = "role";

    public TestPIPRole() {
        super();
        this.log = LoggerFactory.getLogger(TestPIPReputation.class);
    }

    public Map<String, String> getRole(){
        return role;
    }
    public void setRole(final Map<String, String> role){
        this.role = role;
    }

    @Override
    public void fireBeforeActionEvent(ActionEvent e) {
        if (e.originState.getName().equals("TRY")) {
            PepAttributeInterface subject = e.request.getAttributes(category, subjectId).iterator().next();
            String rep = getRole().get(subject.getValue());
            if (role != null) {
                log.info("{} - subject {} has role {}", e.request, subject.getValue(), rep);
                PepAttributeInterface test = newPrivateAttribute(getAttributeId(), PepAttribute.DATATYPES.STRING, rep, subject);
                // Make attribute unmanaged (automatically managed by the UCon)
                // because we set an expiry date
                test.setExpires(new Date());
                e.request.replace(test);
            } else {
                log.warn("subject {} has no role attribute -- ignoring", subject.getValue());
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