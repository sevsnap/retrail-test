/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.demo;

import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.StateType;
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
        User user = null;
        synchronized (users) {
            user = users.get(id);
            if (user == null) {
                log.warn("user \"{}\" (size={}) not cached, creating new", id, id.length());
                user = new User(id);
                users.put(id, user);
            }
        }
        return user;
    }

    public String getId() {
        return id;
    }

    public synchronized StateType getStateType() {
        return pepSession == null ? StateType.UNKNOWN : pepSession.getStateType();
    }

    public synchronized String getStateName() {
        return pepSession == null ? null : pepSession.getStateName();
    }

    public String getCustomId() {
        return pepSession == null ||  pepSession.getCustomId().length() == 0? "?" : pepSession.getCustomId();
    }

    public String getUuid() {
        return pepSession == null? "?" : pepSession.getUuid();
    }

    
    public PepSession getSession() {
        return pepSession;
    }

    public boolean goToDoor() {
        boolean ok = false;
        try {
            PepRequest req = PepRequest.newInstance(
                    id,
                    "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                    " ",
                    "issuer");
            log.info("sending request: {}", req);
            pepSession = UsageController.getInstance().tryAccess(req, id);
            log.info("got decision: {}", pepSession);
            ok = pepSession.getDecision() == PepResponse.DecisionEnum.Permit;
        } catch (Exception e) {
            log.error("Unexpected exception: {}", e.getMessage());
        }
        return ok;
    }

    public boolean enterRoom() {
        boolean ok = false;
        try {
            pepSession = UsageController.getInstance().startAccess(pepSession);
            ok = pepSession.getDecision() == PepResponse.DecisionEnum.Permit;
        } catch (Exception e) {
            log.error("Unexpected exception: {}", e.getMessage());
        }
        return ok;
    }

    public boolean leave() {
        boolean ok = false;
        try {
            pepSession = UsageController.getInstance().endAccess(pepSession);
            ok = pepSession.getStateType() == StateType.END;
            pepSession.setCustomId("");
        } catch (Exception e) {
            log.error("Unexpected exception: {}", e.getMessage());
        }
        return ok;
    }

    @Override
    public String toString() {
        return "User " + id + " [status=" + getStateType() + "]";
    }

}
