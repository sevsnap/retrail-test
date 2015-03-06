/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.server.pip.impl.PIPTimer;
import it.cnr.iit.retrail.server.pip.impl.PIPSessions;
import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.demo.UsageController;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UConFactory;
import it.cnr.iit.retrail.server.pip.impl.PIP;
import static it.cnr.iit.retrail.test.DALTest.dal;
import java.io.File;
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

    static PIPSessions pipSessions = null;
    static PIPTimer pipTimer = null;
    PepRequest pepRequest = null;
    static String lastObligation = null;

    public PIPTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        log.warn("Setting up environment...");
        try {
            URL pdpUrl = new URL(pdpUrlString);
            URL myUrl = new URL(pepUrlString);

            // start server
            ucon = UConFactory.getInstance(pdpUrl);
            ucon.loadConfiguration(UsageController.class.getResourceAsStream("/PIPTest.xml"));
            pipSessions = (PIPSessions) ucon.getPIPChain().get("sessions");
            pipTimer = (PIPTimer) ucon.getPIPChain().get("timer");
            TestPIPReputation pipReputation = (TestPIPReputation) ucon.getPIPChain().get("reputation");
            assertEquals("bronze", pipReputation.getReputation().get("fedoraRole"));
            ucon.init();
            ucon.startRecording(new File(("serverRecord.xml")));
            // start client

            pep = new PEP(pdpUrl, myUrl) {
                @Override
                public void onRecoverAccess(PepSession session) throws Exception {
                    // Remove previous run stale sessions
                    endAccess(session);
                }
                
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
            pep.startRecording(new File("clientRecord.xml"));
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
        assertEquals("TRY", pepSession.getStateName());
        assertEquals(StateType.PASSIVE, pepSession.getStateType());
        assertTrue(pep.hasSession(pepSession));
        assertEquals(1, pep.getSessions().size());
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(pdpUrlString, pepSession.getUconUrl().toString());
        assertEquals("sayWelcome", lastObligation);
        return pepSession;
    }

    private void beforeStartAccess(PepSession pepSession) throws Exception {
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(StateType.PASSIVE, pepSession.getStateType());
        assertEquals("TRY", pepSession.getStateName());
    }

    private void afterStartAccess(PepSession pepSession) throws Exception {
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(StateType.ONGOING, pepSession.getStateType());
        assertEquals("sayDetected", lastObligation);
    }

    private void beforeEndAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.getSessions().size());
        assertNotEquals(StateType.END, pepSession.getStateType()); // FIXME was DELETED
        assertNotEquals(StateType.UNKNOWN, pepSession.getStateType());
        assertNotEquals("REVOKED", pepSession.getStateName());
        assertNotEquals("REJECTED", pepSession.getStateName());// FIXME was REJECTED
        assertTrue(pep.hasSession(pepSession));
    }

    private void afterEndAccess(PepSession response) throws Exception {
        assertFalse(pep.hasSession(response));
        assertEquals(0, pep.getSessions().size());
        assertEquals(pdpUrlString, response.getUconUrl().toString());
        assertEquals(StateType.END, response.getStateType()); // FIXME was DELETED
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
        assertEquals(0, pipSessions.getSessions());
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        assertEquals(0, pipSessions.getSessions());
        beforeStartAccess(pepSession);
        PepSession startResponse = pep.startAccess(pepSession);
        afterStartAccess(startResponse);
        afterStartAccess(pepSession);
        assertEquals(1, pipSessions.getSessions());
        beforeEndAccess(startResponse);
        beforeEndAccess(pepSession);
        log.warn("XXXXX ending session: {}", startResponse);
        PepSession endResponse = pep.endAccess(startResponse);
        afterEndAccess(endResponse);
        afterEndAccess(startResponse);
        afterEndAccess(pepSession);
        assertEquals(0, pipSessions.getSessions());
        log.info("ok");
    }

    @Test
    public void test3_TryWithBadReputation() throws Exception {
        log.info("testing access with bad reputation");
        PepRequest req = PepRequest.newInstance(
                "userWithBadReputation",
                "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                " ",
                "issuer");
        PepSession pepSession = pep.tryAccess(req);
        assertEquals(PepResponse.DecisionEnum.Deny, pepSession.getDecision());
        assertEquals(StateType.END, pepSession.getStateType()); // FIXME was REJECTED
        assertEquals(0, pep.getSessions().size());
        assertEquals(0, pipSessions.getSessions());
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
        assertEquals(StateType.END, pepSession.getStateType()); // FIXME was REJECTED
        assertEquals(0, pep.getSessions().size());
        assertEquals(0, pipSessions.getSessions());
        log.info("ok");
    }

    @Test
    public void test4_ConcurrentTryAccessShouldAllowBoth() throws Exception {
        log.info("testing concurrent try access (should be allowed to both)");
        assertEquals(0, pipSessions.getSessions());
        beforeTryAccess();
        PepSession pepSession1 = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession1);
        assertEquals(0, pipSessions.getSessions());
        UconAttribute a = dal.getSharedAttribute(pipSessions.category, pipSessions.getAttributeId());
        //assertEquals(1, a.getSessions().size());
        PepSession pepSession2 = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession2.getDecision());
        //assertEquals(1, a.getSessions().size());
        assertEquals(0, pipSessions.getSessions());
        a = dal.getSharedAttribute(pipSessions.category, pipSessions.getAttributeId());
        //assertEquals(2, a.getSessions().size());
        pep.endAccess(pepSession2);
        a = dal.getSharedAttribute(pipSessions.category, pipSessions.getAttributeId());
        //assertEquals(1, a.getSessions().size());
        pep.endAccess(pepSession1);
        afterEndAccess(pepSession1);
        assertEquals(0, pipSessions.getSessions());
        a = dal.getSharedAttribute(pipSessions.category, pipSessions.getAttributeId());
        //assertEquals(null, a);
        log.info("ok, 2 concurrent tries admitted");
    }

    @Test
    public void test5_ConcurrentStartAccessShouldDenyTheSecondOne() throws Exception {
        log.info("testing concurrent start access (should be denied to the second one)");
        beforeTryAccess();
        assertEquals(0, pipSessions.getSessions());
        PepSession pepSession1 = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession1);
        PepSession pepSession2 = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession2.getDecision());
        assertEquals(2, pep.getSessions().size());
        beforeStartAccess(pepSession1);
        pepSession1 = pep.startAccess(pepSession1);
        afterStartAccess(pepSession1);
        assertEquals(1, pipSessions.getSessions());
        pepSession2 = pep.startAccess(pepSession2);
        assertEquals("TRY", pepSession2.getStateName());
        assertEquals(StateType.PASSIVE, pepSession2.getStateType());
        assertEquals(1, pipSessions.getSessions());
        assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession2.getDecision());
        assertEquals(2, pep.getSessions().size());
        pep.endAccess(pepSession2);
        assertEquals(1, pipSessions.getSessions());
        pep.endAccess(pepSession1);
        assertEquals(0, pipSessions.getSessions());
        afterEndAccess(pepSession1);
        afterEndAccess(pepSession2);
        log.info("ok, 2 concurrent starts not admitted");
    }

    @Test
    public void test6_AccessTooLong() throws Exception {
        log.info("testing prolonged access (should be denied after 2 secs)");
        beforeTryAccess();
        assertEquals(0, pipSessions.getSessions());
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        beforeStartAccess(pepSession);
        pepSession.getLocalInfo().put("tryThis", "itsOk");
        PepSession response = pep.startAccess(pepSession);
        afterStartAccess(response);
        assertNotNull(response.getLocalInfo());
        assertEquals("itsOk", response.getLocalInfo().get("tryThis"));
        int ms = (int) (1000 * pipTimer.getResolution()) + 1000 * 2 /* pipTimer.getMaxDuration() */ + 100;
        log.warn("ok, waiting {} ms for ucon to revoke session", ms);
        Thread.sleep(ms);
        response = pep.getSession(response.getUuid());
        assertEquals("REVOKED", response.getStateName());
        assertEquals("sayRevoked", lastObligation);
        assertNotNull(response.getLocalInfo());
        assertEquals("itsOk", response.getLocalInfo().get("tryThis"));
        pep.endAccess(pepSession);
        afterEndAccess(pepSession);

        log.info("ok, 2 concurrent tries admitted");
    }
    
    int refreshed = 0;
    @Test
    public void test7_CheckRefreshCalled() throws Exception {   
        PIP pip = new PIP() {
            @Override 
            public void init(UConInterface ucon) {
                super.init(ucon);
                refreshed = 0;
            } 
            @Override 
            public void refresh(PepRequestInterface accessRequest, PepSessionInterface session) {
                refreshed++;
            } 
        };
        ucon.getPIPChain().add(pip);
        beforeTryAccess();
        assertEquals(0, pipSessions.getSessions());
        assertEquals(0, refreshed);
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(1, refreshed);
        afterTryAccess(pepSession);
        pep.startAccess(pepSession);
        afterStartAccess(pepSession);
        assertEquals(2, refreshed);
        log.warn("XXX ending with: {}", pepSession);
        pep.endAccess(pepSession);
        afterEndAccess(pepSession);
        assertEquals(3, refreshed);
    }
    
}
