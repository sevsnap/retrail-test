/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.pip.ActionEvent;
import it.cnr.iit.retrail.server.pip.impl.PIP;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Francesco
 */
public class TestPIPWorkingTime extends PIP{
    
    final public String category = PepAttribute.CATEGORIES.SUBJECT;
    private String attributeId = "workingTime";

    public TestPIPWorkingTime() {
        super();
        this.log = LoggerFactory.getLogger(TestPIPWorkingTime.class);
    }

    @Override
    public void init(UConInterface ucon) {
        super.init(ucon);
        PepAttributeInterface workingTime = newSharedAttribute(getAttributeId(), PepAttribute.DATATYPES.BOOLEAN, true, category);
        log.debug("the new attribute is: {}", workingTime);
        workingTime = getSharedAttribute(category, attributeId);
        assert (workingTime != null);
        
/*        Collection<UconSession> session = dal.listSessions(StateType.ONGOING);
        count = 0;
        for(UconSession s:session){
            Collection<UconAttribute> attributes = s.getAttributes();
            for(UconAttribute a:attributes){
                if(a.getId().equals(attributeId2)) count++;
            }
        }*/
    }

    public boolean isWorkingTime() {
        PepAttributeInterface workingTime = getSharedAttribute(category, attributeId);
        assert (workingTime!= null);
        Boolean v = Boolean.parseBoolean(workingTime.getValue());
        return v;
    }
    
    public void setAttribute(boolean flag){
        PepAttributeInterface workingTime = getSharedAttribute(category, attributeId);
        assert (workingTime!= null);
        workingTime.setValue(Boolean.toString(flag));
        try {
            ucon.notifyChanges(workingTime);
        } catch (Exception ex) {
            Logger.getLogger(TestPIPWorkingTime.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }
    
    @Override
    public void fireAfterActionEvent(ActionEvent e) {
        log.info("originState = {}, e.session = {}", e.originState, e.session);
        if (e.originState.getType() != StateType.ONGOING
                && e.session.getStateType() == StateType.ONGOING) {
            PepAttributeInterface sessions = getSharedAttribute(category, attributeId);
            assert (sessions != null);
            Integer v = Integer.parseInt(sessions.getValue()) + 1;
            log.info("incrementing sessions to {}", v);
            sessions.setValue(v.toString());
            e.request.replace(sessions);
                   
        } else if ((e.originState.getName().equals("REVOKED") ||
                e.originState.getType()== StateType.ONGOING)
                && e.session.getStateType() == StateType.END) {
            PepAttributeInterface sessions = getSharedAttribute(category, attributeId);
            assert (sessions != null);
            Integer v = Integer.parseInt(sessions.getValue()) - 1;
            log.info("decrementing sessions to {}", v);
            assert (v >= 0);
            sessions.setValue(v.toString());
            e.request.replace(sessions);
        }
    }

}