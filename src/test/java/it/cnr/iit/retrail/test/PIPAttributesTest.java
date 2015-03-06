/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.server.pip.impl.PIPTimer;
import it.cnr.iit.retrail.server.pip.impl.PIPSessions;
import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.demo.UsageController;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.DAL;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UConFactory;
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
public class PIPAttributesTest {

    static final String pdpUrlString = "http://localhost:8080";
    static final String pepUrlString = "http://localhost:8081";

    static final Logger log = LoggerFactory.getLogger(PIPAttributesTest.class);
    static UConInterface ucon = null;
    static PEPInterface pep = null;

    static PIPSessions pipSessions = null;
    static PIPTimer pipTimer = null;
    static TestPIPReputation pipReputation = null;

    static DAL dal = DAL.getInstance();

    public PIPAttributesTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        log.warn("Setting up environment...");
        try {
            URL pdpUrl = new URL(pdpUrlString);
            URL myUrl = new URL(pepUrlString);
            // start server
            ucon = UConFactory.getInstance(pdpUrl);
            ucon.loadConfiguration(UsageController.class.getResourceAsStream("/PIPAttributesTest.xml"));
            pipSessions = (PIPSessions) ucon.getPIPChain().get("sessions");
            pipReputation = (TestPIPReputation) ucon.getPIPChain().get("reputation");
            pipTimer = (PIPTimer) ucon.getPIPChain().get("timer");
            ucon.init();
            ucon.startRecording(new File("serverRecord.xml"));
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
            };
            pep.init();        // We should have no sessions now
            pep.startRecording(new File("clientRecord.xml"));
        } catch (XmlRpcException | IOException e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        log.info("terminating pep");
        pep.term();
        log.info("terminating ucon");
        ucon.term();
        log.info("done");
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() throws Exception {
        while (pep.getSessions().size() > 0) {
            PepSession s = pep.getSessions().iterator().next();
            log.warn("Terminating {}", s);
            pep.endAccess(s);
            afterEndAccess(s);
        }
        log.info("after teardown, all attributes in the dal should be cleared!");
        Collection<UconAttribute> l = dal.listAttributes(new URL(pepUrlString));
        /*
         for(UconAttribute a: l)
         log.error("**** listAttributes({}): {}", pepUrlString, a);
         assertEquals(0, l.size());
         */
        /*
         l = dal.listAttributes();
         for(UconAttribute a: l)
         log.error("**** listAttributes(): {}", a);
         assertEquals(0, l.size());
         */
        log.info("after teardown, all sessions in the dal should be cleared!");
        Collection<UconSession> u = dal.listSessions();
        for (UconSession s : u) {
            log.error("**** listSessions(): {}", s);
        }
        assertEquals(0, u.size());
        log.info("after teardown everything looks ok!");
    }

    private PepRequest newRequest(String subjectValue) {
        PepRequest pepRequest = PepRequest.newInstance(
                subjectValue,
                "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                " ",
                "issuer");
        return pepRequest;
    }

    private void beforeTryAccess() {
        assertEquals(0, pep.getSessions().size());
    }

    private PepSession afterTryAccess(PepSession pepSession) throws Exception {
        assertTrue(pep.hasSession(pepSession));
        assertEquals(1, pep.getSessions().size());
        assertEquals(StateType.PASSIVE, pepSession.getStateType());
        assertEquals("TRY", pepSession.getStateName());
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(pdpUrlString, pepSession.getUconUrl().toString());
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
    }

    private void beforeEndAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.getSessions().size());
        assertNotEquals(StateType.END, pepSession.getStateType());// was DELETED
        assertNotEquals(StateType.UNKNOWN, pepSession.getStateType());
        assertNotEquals("REVOKED", pepSession.getStateName());
        assertTrue(pep.hasSession(pepSession));
    }

    private void afterEndAccess(PepSession response) throws Exception {
        assertFalse(pep.hasSession(response));
        assertEquals(pdpUrlString, response.getUconUrl().toString());
        assertEquals(StateType.END, response.getStateType()); // was DELETED
    }

    @Test
    public void test1_SingleSession() throws Exception {
        log.info("testing try start end session");
        PepRequest pepRequest1 = newRequest("user1");
        PepSession pepSession1 = pep.tryAccess(pepRequest1);
        afterTryAccess(pepSession1);
        PepSession response1 = pep.startAccess(pepSession1);
        afterStartAccess(response1);
        for (UconAttribute a : dal.listAttributes()) {
            log.info("************** {}", a);
        }
        pep.endAccess(pepSession1);
        afterEndAccess(response1);
        log.info("ok");
    }

    @Test
    public void test2_FlatAttributes() throws Exception {
        log.info("testing if concurrent sessions correctly share attributes");
        PepRequest pepRequest1 = newRequest("user1");
        PepSession pepSession1 = pep.tryAccess(pepRequest1);
        PepSession response1 = pep.startAccess(pepSession1);
        afterStartAccess(response1);
        PepRequest pepRequest2 = newRequest("user2");
        PepSession pepSession2 = pep.tryAccess(pepRequest2);
        PepSession response2 = pep.startAccess(pepSession2);
        afterStartAccess(response2);
        PepRequest pepRequest3 = newRequest("user3");
        PepSession pepSession3 = pep.tryAccess(pepRequest3);
        PepSession response3 = pep.startAccess(pepSession3);
        log.info("session 3 must not be Permitted since the shared number of session would be greater than 2");
        assertNotEquals(PepResponse.DecisionEnum.Permit, response3.getDecision());
        pep.endAccess(response1);
        pep.endAccess(response2);
        pep.endAccess(response3);
        log.info("ok");
    }

    @Test
    public void test3_UnmanagedPrivateAttributes() throws Exception {
        log.info("testing if concurrent sessions have their own unmanaged reputation attribute");
        PepRequest pepRequest1 = newRequest("user1");
        PepSession pepSession1 = pep.tryAccess(pepRequest1);
        PepSession response1 = pep.startAccess(pepSession1);
        afterStartAccess(response1);
        log.info("ensure there has been at least 1 timer tick");
        Thread.sleep(1000);
        assertEquals(1, pipReputation.listUnmanagedAttributes().size());
        log.info("try - start - end the second request (should fail since reputation too low)");
        PepRequest pepRequest4 = newRequest("user4");
        PepSession pepSession4 = pep.tryAccess(pepRequest4);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession4.getDecision());
        Thread.sleep(500);
        log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        assertEquals(2, pipReputation.listUnmanagedAttributes().size());
        PepSession response4 = pep.startAccess(pepSession4);
        assertNotEquals(PepResponse.DecisionEnum.Permit, response4.getDecision());
        assertEquals(2, pipReputation.listUnmanagedAttributes().size());
        pep.endAccess(response4);
        assertEquals(1, pipReputation.listUnmanagedAttributes().size());
        log.info("ensure there has been at least 1 timer tick");
        Thread.sleep(1000);
        //assertEquals(1, pipReputation.listManagedAttributes().size());
        log.info("Session 1 must not be REVOKED because of reputation interleaving");
        response1 = pep.getSession(response1.getUuid());
        assertEquals(StateType.ONGOING, response1.getStateType());
        //assertEquals(1, pipReputation.listManagedAttributes().size());
        Thread.sleep(1300);
        log.info("Session 1 must now be REVOKED because of time");
        assertEquals("REVOKED", response1.getStateName());
        //assertEquals(1, pipReputation.listManagedAttributes().size());
        pep.endAccess(response1);
        log.info("ok");
    }

    @Test
    public void test4_HierarchicalTimeVariantAttributes() throws Exception {
        log.info("testing if concurrent sessions have their own attribute timers");
        PepRequest pepRequest1 = newRequest("user1");
        PepRequest pepRequest2 = newRequest("user2");
        PepSession pepSession1 = pep.tryAccess(pepRequest1);
        PepSession response1 = pep.startAccess(pepSession1);
        afterStartAccess(response1);
        assertEquals(1, pipTimer.listManagedAttributes().size());
        Thread.sleep(1000);
        assertEquals(1, pipTimer.listManagedAttributes().size());
        PepSession pepSession2 = pep.tryAccess(pepRequest2);
        PepSession response2 = pep.startAccess(pepSession2);
        afterStartAccess(response2);
        assertEquals(2, pipTimer.listManagedAttributes().size());
        log.warn("ok, waiting for ucon to revoke session {}", pepSession1.getUuid());
        Thread.sleep(2100 + (int) (1000 * pipTimer.getResolution()));
        log.info("by now session {} must be REVOKED, whilst session {} should be ONGOING", pepSession1.getUuid(), pepSession2.getUuid());
        response1 = pep.getSession(response1.getUuid());
        assertEquals("REVOKED", response1.getStateName());
        response2 = pep.getSession(response2.getUuid());
        assertEquals(StateType.ONGOING, response2.getStateType());
        Thread.sleep(1000);
        log.warn("ok. session {} should have been now REVOKED as well", pepSession2.getUuid());
        response2 = pep.getSession(response2.getUuid());
        assertEquals("REVOKED", response2.getStateName());
        log.debug("ok. restoring global configuration");
        pep.endAccess(response1);
        pep.endAccess(response2);
        log.info("ok");
    }
}
