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
public class PIPAttributesTests {

    static final String pdpUrlString = "http://localhost:8080";
    static final String pepUrlString = "http://localhost:8081";

    static final Logger log = LoggerFactory.getLogger(PIPAttributesTests.class);
    static UConInterface ucon = null;
    static PEPInterface pep = null;
    
    static TestPIPSessions pipSessions = null;
    static TestPIPTimer pipTimer = null;
    PepAccessRequest pepRequest = null;

    public PIPAttributesTests() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        log.warn("Setting up environment...");
        try {
            // start server
            ucon = UCon.getInstance();
            ucon.setPreauthPolicy(UsageController.class.getResource("/META-INF/policies2/pre2.xml"));
            ucon.setOngoingPolicy(UsageController.class.getResource("/META-INF/policies2/on2.xml"));
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

    @Test
    public void test1_SharedAttributes() throws Exception {
        log.info("testing if concurrent sessions correctly share attributes");
        int oldDuration = pipTimer.getMaxDuration();
        PepSession pepSession1 = pep.tryAccess(pepRequest);
        PepSession response1 = pep.startAccess(pepSession1);
        afterStartAccess(response1);
        PepSession pepSession2 = pep.tryAccess(pepRequest);
        PepSession response2 = pep.startAccess(pepSession2);
        afterStartAccess(response2);
        PepSession pepSession3 = pep.tryAccess(pepRequest);
        PepSession response3 = pep.startAccess(pepSession3);
        log.info("session 3 must not be Permitted since the shared number of session would be greater than 2");
        assertNotEquals(PepAccessResponse.DecisionEnum.Permit, response3.getDecision());
        log.info("ok");
    }
    
    @Test
    public void test2_PrivateAttributes() throws Exception {
        log.info("testing if concurrent sessions have their own attribute timers");
        int oldDuration = pipTimer.getMaxDuration();
        pipTimer.setMaxDuration(1);
        PepSession pepSession1 = pep.tryAccess(pepRequest);
        PepSession response1 = pep.startAccess(pepSession1);
        afterStartAccess(response1);
        pipTimer.setMaxDuration(3);
        PepSession pepSession2 = pep.tryAccess(pepRequest);
        PepSession response2 = pep.startAccess(pepSession2);
        afterStartAccess(response2);
        log.warn("ok, waiting for ucon to revoke session 1");
        Thread.sleep(1100);
        log.info("by now session 1 must be REVOKED, whilst session 2 should be ONGOING");
        response1 = pep.getSession(response1.getUuid());
        assertEquals(PepSession.Status.REVOKED, response1.getStatus());
        response2 = pep.getSession(response2.getUuid());
        assertEquals(PepSession.Status.ONGOING, response2.getStatus());
        Thread.sleep(2000);
        log.warn("ok. session 2 should have been now REVOKED as well");
        response2 = pep.getSession(response2.getUuid());
        assertEquals(PepSession.Status.REVOKED, response2.getStatus());
        log.debug("ok. restoring global configuration");
        pipTimer.setMaxDuration(oldDuration);
        log.info("ok");
    }
}
