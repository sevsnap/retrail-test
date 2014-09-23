/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and onBeforeTryAccess the template in the editor.
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.commons.PepSession;
import it.cnr.iit.retrail.server.pip.PIP;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
// Expiring attribute handler
public class TestPIPSessions extends PIP {

    private int sessions = 0;
    final private int maxSessions;
    
    public TestPIPSessions(int maxSessions) {
        super();
        this.log = LoggerFactory.getLogger(TestPIPSessions.class);
        this.maxSessions = maxSessions;
    }

    @Override
    public void onBeforeTryAccess(PepAccessRequest request) {
        log.info("Number of open sessions: " + sessions);
        PepRequestAttribute test = newAttribute("openSessions", "http://www.w3.org/2001/XMLSchema#integer", Integer.toString(sessions), "http://localhost:8080/federation-id-prov/saml", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
        request.add(test);
    }

    @Override
    public void onBeforeStartAccess(PepAccessRequest request, PepSession session) {
        sessions++;
        log.info("Number of open sessions: " + sessions);
        PepRequestAttribute test = newAttribute("openSessions", "http://www.w3.org/2001/XMLSchema#integer", Integer.toString(sessions), "http://localhost:8080/federation-id-prov/saml", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
        request.add(test);
    }
    
    @Override
    public void onBeforeEndAccess(PepAccessRequest request, PepSession session) {
        if(session.getStatus() == PepSession.Status.ONGOING)
           sessions--;
        log.info("Number of open sessions: " + sessions);
    }
}
