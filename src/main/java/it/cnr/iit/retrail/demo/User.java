/*
 */
package it.cnr.iit.retrail.demo;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.PepSession;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class User {

    static final Logger log = LoggerFactory.getLogger(User.class);
    static final Map<String, User> users = new HashMap<>();
    private final String id;
    private PepSession pepSession = null;

    private User(String id) {
        this.id = id;
    }

    static public User getInstance(String id) throws Exception {
        User user = users.get(id);
        if(user == null) {
            user = new User(id);
            users.put(id, user);
        }
        return user;
    }

    public String getId() {
        return id;
    }
    
    public PepSession.Status getStatus() {
        return pepSession == null? PepSession.Status.UNKNOWN : pepSession.getStatus();
    }

    
    public String getCustomId() {
        return pepSession == null? "?" : pepSession.getCustomId();
    }

    
    public boolean goToDoor() {
        boolean ok = false;
        try {
            PepAccessRequest req = PepAccessRequest.newInstance(
                    id,
                    "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                    " ",
                    "issuer");
            log.info("sending request: {}", req);
            pepSession = UsageController.getInstance().tryAccess(req, id);
            log.info("got decision: {}", pepSession);
            ok = pepSession.getDecision() == PepAccessResponse.DecisionEnum.Permit;
        } catch (Exception e) {
            log.error("Unexpected exception: {}", e.getMessage());
        }
        return ok;
    }

    public boolean enterRoom() {
        boolean ok = false;
        try {
            pepSession = UsageController.getInstance().startAccess(pepSession);
            ok = pepSession.getDecision() == PepAccessResponse.DecisionEnum.Permit;
        } catch (Exception e) {
            log.error("Unexpected exception: {}", e.getMessage());
        }
        return ok;
    }

    public boolean leave() {
        boolean ok = false;
        try {
            pepSession = UsageController.getInstance().endAccess(pepSession);
            ok = pepSession.getStatus() == PepSession.Status.DELETED;
            if(ok)
                users.remove(id);
        } catch (Exception e) {
            log.error("Unexpected exception: {}", e.getMessage());
        }
        return ok;
    }

}
