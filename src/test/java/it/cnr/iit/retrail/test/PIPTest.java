/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.demo.UsageController;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import static it.cnr.iit.retrail.test.DALTest.dal;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
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
public class PIPTest {

    static final String pdpUrlString = "http://localhost:8080";
    static final String pepUrlString = "http://localhost:8081";

    static final Logger log = LoggerFactory.getLogger(PIPTest.class);
    static UConInterface ucon = null;
    static PEPInterface pep = null;

    static TestPIPSessions pipSessions = null;
    static TestPIPTimer pipTimer = null;
    PepRequest pepRequest = null;
    static String lastObligation = null;

    public PIPTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        log.warn("Setting up environment...");
        try {
            // start server
            ucon = UCon.getInstance();
            ucon.setPolicy(UConInterface.PolicyEnum.PRE, UsageController.class.getResource("/META-INF/policies1/pre1.xml"));
            ucon.setPolicy(UConInterface.PolicyEnum.ON, UsageController.class.getResource("/META-INF/policies1/on1.xml"));
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

                @Override
                public synchronized void onObligation(PepSession session, String obligation) throws Exception {
                    log.warn("obligation {} received for {}", obligation, session);
                    lastObligation = obligation;
                }

                @Override
                public synchronized void runObligations(PepSession session) throws Exception {
                    lastObligation = null;
                    super.runObligations(session);
                    for (String obligation : session.getObligations()) {
                        onObligation(session, obligation);
                    }
                    session.getObligations().clear();
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
            Collection<UconSession> u = dal.listSessions();
            if (u.size() > 0) {
                log.error("no session should be in the dal, found {}!", u.size());
            }
            for (UconSession s : u) {
                log.error("**** listSessions(): {}", s);
            }
            assertEquals(0, u.size());

            pepRequest = PepRequest.newInstance(
                    "fedoraRole",
                    "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                    " ",
                    "issuer");
            PepAttribute attribute = new PepAttribute(
                    "urn:fedora:names:fedora:2.1:resource:datastream:id",
                    PepAttribute.DATATYPES.STRING,
                    "FOPDISSEM",
                    "issuer",
                    PepAttribute.CATEGORIES.RESOURCE);
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
        assertEquals(Status.TRY, pepSession.getStatus());
        assertTrue(pep.hasSession(pepSession));
        assertEquals(1, pep.getSessions().size());
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(pdpUrlString, pepSession.getUconUrl().toString());
        assertEquals("sayWelcome", lastObligation);
        return pepSession;
    }

    private void beforeStartAccess(PepSession pepSession) throws Exception {
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(Status.TRY, pepSession.getStatus());
    }

    private void afterStartAccess(PepSession pepSession) throws Exception {
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(Status.ONGOING, pepSession.getStatus());
        assertEquals("sayDetected", lastObligation);
    }

    private void beforeEndAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.getSessions().size());
        assertNotEquals(Status.DELETED, pepSession.getStatus());
        assertNotEquals(Status.UNKNOWN, pepSession.getStatus());
        assertNotEquals(Status.REVOKED, pepSession.getStatus());
        assertNotEquals(Status.REJECTED, pepSession.getStatus());
        assertTrue(pep.hasSession(pepSession));
    }

    private void afterEndAccess(PepSession response) throws Exception {
        assertFalse(pep.hasSession(response));
        assertEquals(0, pep.getSessions().size());
        assertEquals(pdpUrlString, response.getUconUrl().toString());
        assertEquals(Status.DELETED, response.getStatus());
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
        PepRequest req = PepRequest.newInstance(
                "fedoraBadReputation",
                "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                " ",
                "issuer");
        PepSession pepSession = pep.tryAccess(req);
        assertEquals(PepResponse.DecisionEnum.Deny, pepSession.getDecision());
        assertEquals(Status.REJECTED, pepSession.getStatus());
        assertEquals(0, pep.getSessions().size());
        assertEquals(0, pipSessions.sessions);
        assertEquals("sayStandOff", lastObligation);
        log.info("ok");
    }

    @Test
    public void test3_TryWithUnknownSubject() throws Exception {
        log.info("testing access with unknown subject");
        PepRequest req = PepRequest.newInstance(
                "unknownSubject",
                "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                " ",
                "issuer");
        PepSession pepSession = pep.tryAccess(req);
        assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(Status.REJECTED, pepSession.getStatus());
        assertEquals(0, pep.getSessions().size());
        assertEquals(0, pipSessions.sessions);
        log.info("ok");
    }

    @Test
    public void test4_ConcurrentTryAccessShouldAllowBoth() throws Exception {
        log.info("testing concurrent try access (should be allowed to both)");
        assertEquals(0, pipSessions.sessions);
        beforeTryAccess();
        PepSession pepSession1 = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession1);
        assertEquals(0, pipSessions.sessions);
        UconAttribute a = dal.getSharedAttribute(pipSessions.category, pipSessions.id);
        assertEquals(1, a.getSessions().size());
        PepSession pepSession2 = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession2.getDecision());
        assertEquals(1, a.getSessions().size());
        assertEquals(0, pipSessions.sessions);
        a = dal.getSharedAttribute(pipSessions.category, pipSessions.id);
        assertEquals(2, a.getSessions().size());
        pep.endAccess(pepSession2);
        a = dal.getSharedAttribute(pipSessions.category, pipSessions.id);
        assertEquals(1, a.getSessions().size());
        pep.endAccess(pepSession1);
        afterEndAccess(pepSession1);
        assertEquals(0, pipSessions.sessions);
        a = dal.getSharedAttribute(pipSessions.category, pipSessions.id);
        assertEquals(null, a);
        log.info("ok, 2 concurrent tries admitted");
    }

    @Test
    public void test5_ConcurrentStartAccessShouldDenyTheSecondOne() throws Exception {
        log.info("testing concurrent start access (should be denied to the second one)");
        beforeTryAccess();
        assertEquals(0, pipSessions.sessions);
        PepSession pepSession1 = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession1);
        PepSession pepSession2 = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession2.getDecision());
        assertEquals(2, pep.getSessions().size());
        beforeStartAccess(pepSession1);
        pepSession1 = pep.startAccess(pepSession1);
        afterStartAccess(pepSession1);
        assertEquals(1, pipSessions.sessions);
        pepSession2 = pep.startAccess(pepSession2);
        assertEquals(Status.TRY, pepSession2.getStatus());
        assertEquals(1, pipSessions.sessions);
        assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession2.getDecision());
        assertEquals(2, pep.getSessions().size());
        pep.endAccess(pepSession2);
        assertEquals(1, pipSessions.sessions);
        pep.endAccess(pepSession1);
        assertEquals(0, pipSessions.sessions);
        afterEndAccess(pepSession1);
        afterEndAccess(pepSession2);
        log.info("ok, 2 concurrent starts not admitted");
    }

    @Test
    public void test6_AccessTooLong() throws Exception {
        log.info("testing prolonged access (should be denied after 2 secs)");
        beforeTryAccess();
        assertEquals(0, pipSessions.sessions);
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        beforeStartAccess(pepSession);
        pepSession.getLocalInfo().put("tryThis", "itsOk");
        PepSession response = pep.startAccess(pepSession);
        afterStartAccess(response);
        assertNotNull(response.getLocalInfo());
        assertEquals("itsOk", response.getLocalInfo().get("tryThis"));
        int ms = (int) (1000 * pipTimer.getResolution()) + 1000 * pipTimer.getMaxDuration() + 100;
        log.warn("ok, waiting {} ms for ucon to revoke session", ms);
        Thread.sleep(ms);
        response = pep.getSession(response.getUuid());
        assertEquals(Status.REVOKED, response.getStatus());
        assertEquals("sayDenied", lastObligation);
        assertNotNull(response.getLocalInfo());
        assertEquals("itsOk", response.getLocalInfo().get("tryThis"));
        pep.endAccess(pepSession);
        afterEndAccess(pepSession);

        log.info("ok, 2 concurrent tries admitted");
    }

}
