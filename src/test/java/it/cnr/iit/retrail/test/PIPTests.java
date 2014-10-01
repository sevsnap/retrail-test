/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.commons.PepSession;
import it.cnr.iit.retrail.demo.UsageController;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.dal.DAL;
import it.cnr.iit.retrail.server.impl.UCon;
import java.io.IOException;
import java.net.URL;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author oneadmin
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PIPTests {

    static final String pdpUrlString = "http://localhost:8080";
    static final String pepUrlString = "http://localhost:8081";

    static final Logger log = LoggerFactory.getLogger(PIPTests.class);
    static UConInterface ucon = null;
    static PEPInterface pep = null;
    
    static TestPIPSessions pipSessions = null;
    static TestPIPTimer pipTimer = null;
    PepAccessRequest pepRequest = null;

    public PIPTests() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        log.warn("Setting up environment...");
        try {
            // start server
            ucon = UCon.getInstance();
            ucon.setPreauthPolicy(UsageController.class.getResource("/META-INF/policies1/pre1.xml"));
            ucon.setOngoingPolicy(UsageController.class.getResource("/META-INF/policies1/on1.xml"));
            pipSessions = new TestPIPSessions();
            ucon.addPIP(pipSessions);
            TestPIPReputation reputation = new TestPIPReputation();
            reputation.reputationMap.put("fedoraRole", "bronze");
            reputation.reputationMap.put("fedoraBadReputation", "bad");
            ucon.addPIP(reputation);
            pipTimer = new TestPIPTimer(2);
            ucon.addPIP(pipTimer);
            ucon.init();

            // start client
            URL pdpUrl = new URL(pdpUrlString);
            URL myUrl = new URL(pepUrlString);
            
            pep = new PEP(pdpUrl, myUrl) {
                @Override
                public synchronized void onRevokeAccess(PepSession session) throws Exception {
                    log.warn("automatic end access disabled for test purposes - {}", session);
                }
            };

            // clean up previous sessions, if any, by clearing the recoverable
            // access flag. This ensures the next heartbeat we'll have a clean
            // ucon status (the first heartbeat is waited by init()).
            pep.setAccessRecoverableByDefault(false);
            pep.init();        // We should have no sessions now
        } catch (XmlRpcException | IOException e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        while (pep.getSessions().size() > 0) {
            PepSession s = pep.getSessions().iterator().next();
            log.warn("Terminating {}", s);
            pep.endAccess(s);
        }
        log.info("terminating pep");
        pep.term();
        log.info("terminating ucon");
        ucon.term();
        log.info("done");
    }

    @Before
    public void setUp() {
        try {
            assertEquals(0, DAL.getInstance().listSessions().size());
            pepRequest = PepAccessRequest.newInstance(
                    "fedoraRole",
                    "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                    " ",
                    "issuer");
            PepRequestAttribute attribute = new PepRequestAttribute(
                    "urn:fedora:names:fedora:2.1:resource:datastream:id",
                    PepRequestAttribute.DATATYPES.STRING,
                    "FOPDISSEM",
                    "issuer",
                    PepRequestAttribute.CATEGORIES.RESOURCE);
            pepRequest.add(attribute);
        } catch (Exception e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    private void beforeTryAccess() {
        assertEquals(0, pep.getSessions().size());
    }

    private PepSession afterTryAccess(PepSession pepSession) throws Exception {
        assertTrue(pep.hasSession(pepSession));
        assertEquals(1, pep.getSessions().size());
        assertEquals(PepSession.Status.TRY, pepSession.getStatus());
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(pdpUrlString, pepSession.getUconUrl().toString());
        return pepSession;
    }

    private void beforeStartAccess(PepSession pepSession) throws Exception {
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(PepSession.Status.TRY, pepSession.getStatus());
    }

    private void afterStartAccess(PepSession pepSession) throws Exception {
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(PepSession.Status.ONGOING, pepSession.getStatus());
    }

    private void beforeEndAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.getSessions().size());
        assertNotEquals(PepSession.Status.DELETED, pepSession.getStatus());
        assertNotEquals(PepSession.Status.UNKNOWN, pepSession.getStatus());
        assertNotEquals(PepSession.Status.REVOKED, pepSession.getStatus());
        assertNotEquals(PepSession.Status.REJECTED, pepSession.getStatus());
        assertTrue(pep.hasSession(pepSession));
    }

    private void afterEndAccess(PepSession response) throws Exception {
        assertFalse(pep.hasSession(response));
        assertEquals(0, pep.getSessions().size());
        assertEquals(pdpUrlString, response.getUconUrl().toString());
        assertEquals(PepSession.Status.DELETED, response.getStatus());
    }

    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test1_TryEndCycle() throws Exception {
        log.info("testing pre-access policy only");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        beforeEndAccess(pepSession);
        PepSession pepResponse = pep.endAccess(pepSession);
        afterEndAccess(pepResponse);
        afterEndAccess(pepSession);
        log.info("short cycle ok");
    }

    /**
     * Test of startAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test2_TryStartEndCycle() throws Exception {
        log.info("testing on-access policy");
        assertEquals(0, pipSessions.sessions);        
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        assertEquals(0, pipSessions.sessions);        
        beforeStartAccess(pepSession);
        PepSession startResponse = pep.startAccess(pepSession);
        afterStartAccess(startResponse);
        afterStartAccess(pepSession);
        assertEquals(1, pipSessions.sessions);        
        beforeEndAccess(startResponse);
        beforeEndAccess(pepSession);
        PepSession endResponse = pep.endAccess(startResponse);
        afterEndAccess(endResponse);
        afterEndAccess(startResponse);
        afterEndAccess(pepSession);
        assertEquals(0, pipSessions.sessions);        
        log.info("ok");
    }
    
    @Test
    public void test3_TryWithBadReputation() throws Exception {
        log.info("testing access with bad reputation");
        PepAccessRequest req = PepAccessRequest.newInstance(
                    "fedoraBadReputation",
                    "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                    " ",
                    "issuer");
        PepSession pepSession = pep.tryAccess(req);
        assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(PepSession.Status.REJECTED, pepSession.getStatus());
        assertEquals(0, pep.getSessions().size());
        assertEquals(0, pipSessions.sessions);        
        log.info("ok");
    }
    
    @Test
    public void test3_TryWithUnknownSubject() throws Exception {
        log.info("testing access with unknown subject");
        PepAccessRequest req = PepAccessRequest.newInstance(
                    "unknownSubject",
                    "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                    " ",
                    "issuer");
        PepSession pepSession = pep.tryAccess(req);
        assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(PepSession.Status.REJECTED, pepSession.getStatus());
        assertEquals(0, pep.getSessions().size());
        assertEquals(0, pipSessions.sessions);        
        log.info("ok");
    }

    @Test
    public void test4_ConcurrentTryAccess() throws Exception {
        log.info("testing concurrent try access (should be allowed to both)");
        assertEquals(0, pipSessions.sessions);
        beforeTryAccess();
        PepSession pepSession1 = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession1);
        assertEquals(0, pipSessions.sessions);
        PepSession pepSession2 = pep.tryAccess(pepRequest);
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession2.getDecision());
        assertEquals(2, pep.getSessions().size());
        assertEquals(0, pipSessions.sessions);
        pep.endAccess(pepSession2);
        pep.endAccess(pepSession1);
        afterEndAccess(pepSession1);
        assertEquals(0, pipSessions.sessions);
        log.info("ok, 2 concurrent tries admitted");
    }
    
    @Test
    public void test5_ConcurrentStartAccess() throws Exception {
        log.info("testing concurrent start access (should be denied to the second one)");
        beforeTryAccess();
        assertEquals(0, pipSessions.sessions);
        PepSession pepSession1 = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession1);
        PepSession pepSession2 = pep.tryAccess(pepRequest);
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession2.getDecision());
        assertEquals(2, pep.getSessions().size());
        beforeStartAccess(pepSession1);
        pepSession1 = pep.startAccess(pepSession1);
        afterStartAccess(pepSession1);
        assertEquals(1, pipSessions.sessions);
        pepSession2 = pep.startAccess(pepSession2);
        assertEquals(1, pipSessions.sessions);
        assertEquals(PepSession.Status.TRY, pepSession2.getStatus());
        assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession2.getDecision());
        assertEquals(2, pep.getSessions().size());
        pep.endAccess(pepSession2);
        assertEquals(1, pipSessions.sessions);
        pep.endAccess(pepSession1);
        assertEquals(0, pipSessions.sessions);
        afterEndAccess(pepSession1);
        afterEndAccess(pepSession2);
        log.info("ok, 2 concurrent tries admitted");
    }
    
    @Test
    public void test6_AccessTooLong() throws Exception {
        log.info("testing prolonged access (should be denied after 2 secs)");
        beforeTryAccess();
        assertEquals(0, pipSessions.sessions);
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        beforeStartAccess(pepSession);
        PepSession response = pep.startAccess(pepSession);
        afterStartAccess(response);
        log.warn("ok, waiting some time for ucon to revoke session");
        Thread.sleep(1000*pipTimer.maxDuration + 100);
        response = pep.getSession(response.getUuid());
        assertEquals(PepSession.Status.REVOKED, response.getStatus());
        pep.endAccess(pepSession);
        afterEndAccess(pepSession);
        log.info("ok, 2 concurrent tries admitted");
    }

}
