/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.pip.impl.StandAlonePIP;
import java.util.Date;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public class TestPIPTimer extends StandAlonePIP {

    protected int maxDuration;
    protected double resolution = 1.0;
    protected StateType forStatus = StateType.ONGOING;

    public TestPIPTimer() {
        super();
        this.log = LoggerFactory.getLogger(TestPIPTimer.class);
        this.maxDuration = 3600;
    }

    public TestPIPTimer(int maxDuration) {
        super();
        this.log = LoggerFactory.getLogger(TestPIPTimer.class);
        this.maxDuration = maxDuration;
    }

    @Override
    public void onBeforeStartAccess(PepRequestInterface request, PepSessionInterface session) {
        PepAttributeInterface subject = request.getAttribute("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject", "urn:oasis:names:tc:xacml:1.0:subject:subject-id");
        PepAttributeInterface a = newPrivateAttribute("timer", "http://www.w3.org/2001/XMLSchema#double", Double.toString(maxDuration), "http://localhost:8080/federation-id-prov/saml", subject);
        request.replace(a);
    }

    @Override
    public void onAfterStartAccess(PepRequestInterface request, PepSessionInterface session) {
        if (session.getStateType() != StateType.ONGOING) {
            log.warn("clearing timer attribute because session status = {}", session.getStateType());
            PepAttributeInterface subject = request.getAttribute("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject", "urn:oasis:names:tc:xacml:1.0:subject:subject-id");
            PepAttributeInterface a = newPrivateAttribute("timer", "http://www.w3.org/2001/XMLSchema#double", "0", "http://localhost:8080/federation-id-prov/saml", subject);
            a.setExpires(new Date());
            request.replace(a);
        }
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }

    public double getResolution() {
        return resolution;
    }

    public void setResolution(double resolution) {
        this.resolution = resolution;
    }

    public StateType getForStatus() {
        return forStatus;
    }

    public void setForStatus(StateType forStatus) {
        this.forStatus = forStatus;
    }

    @Override
    public void run() {
        boolean interrupted = false;
        while (!interrupted) {
            try {
                Thread.sleep((int) (1000 * resolution));
                for (PepAttributeInterface a : listManagedAttributes()) {
                    UconAttribute u = (UconAttribute) a;
                    UconSession s = u.getSession();
                    if (s.getStateType() == forStatus) {
                        Double ttg = Double.parseDouble(a.getValue());
                        if (ttg > 0) {
                            ttg = Double.max(0, ttg - resolution);
                            a.setValue(ttg.toString());
                            log.debug("awaken {}", a);
                            notifyChanges(a);
                        }
                    }
                }
            } catch (InterruptedException ex) {
                log.warn("interrupted");
                interrupted = true;
            } catch (Exception ex) {
                log.error("while timer running: {}", ex);
            }
        }
        log.info("exiting");
    }

}
