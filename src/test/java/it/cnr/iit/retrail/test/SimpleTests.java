/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.PepSession;
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
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SimpleTests {
    static final String pdpUrlString = "http://localhost:8080";
    static final Logger log = LoggerFactory.getLogger(SimpleTests.class);
    static UConInterface ucon = null;
    static PEPInterface pep = null;
    PepAccessRequest pepRequest = null;

    public SimpleTests() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        log.warn("Setting up environment...");
        try {
            // start server
            ucon = UCon.getInstance();
            ucon.init();
            // start client
            URL pdpUrl = new URL(pdpUrlString);
            URL myUrl = new URL("http://localhost:8081");
            pep = new PEP(pdpUrl, myUrl);
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
            pep.endAccess(s);
        }
        pep.term();
        ucon.term();
    }

    @Before
    public void setUp() {
        try {
            pepRequest = PepAccessRequest.newInstance(
                    "fedoraRole",
                    "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                    " ",
                    "issuer");
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
        assertEquals(1, pep.getSessions().size());
        assertEquals(PepSession.Status.TRY, pepSession.getStatus());
    }

    private void afterStartAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.getSessions().size());
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
     * Test of hasSession method, of class PEP.
     * @throws java.io.IOException
     */
    @Test
    public void test1_init() throws IOException {
        log.info("Check if the server made us recover some local sessions");
        assertEquals(0, pep.getSessions().size());
        log.info("Ok, no recovered sessions");
    }

    /**
     * Test of echo method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test2_Echo() throws Exception {
        log.info("checking if the server is up and running");
        String echoTest = "<echoTest/>";
        Node node = DomUtils.read(echoTest);
        Node result = ((PEP)pep).echo(node);
        assertEquals(DomUtils.toString(node), DomUtils.toString(result));
        log.info("server echo ok");
    }

    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test3_TryEndCycle() throws Exception {
        log.info("performing a tryAccess-EndAccess short cycle");
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
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test3_TryEndCycleAccessWithCustomId() throws Exception {
        log.info("TryAccessWithCustomId");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest, "ziopino");
        afterTryAccess(pepSession);
        assertEquals("ziopino", pepSession.getCustomId());
        beforeEndAccess(pepSession);
        PepSession response = ((PEP)pep).endAccess(null, pepSession.getCustomId());
        afterEndAccess(response);
        afterEndAccess(pepSession);
    }

    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test4_AssignCustomIdByUuid() throws Exception {
        log.info("AssignCustomIdByUuid");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest, "ziopino");
        afterTryAccess(pepSession);
        PepSession assignResponse = ((PEP)pep).assignCustomId(pepSession.getUuid(), null, "ziopino2");
        assertEquals(pdpUrlString, assignResponse.getUconUrl().toString());
        assertEquals("ziopino2", assignResponse.getCustomId());
        assertEquals(pdpUrlString, pepSession.getUconUrl().toString());
        assertEquals("ziopino2", pepSession.getCustomId());
        afterTryAccess(assignResponse);
        afterTryAccess(pepSession);
        beforeEndAccess(pepSession);
        beforeEndAccess(assignResponse);
        PepSession response = ((PEP)pep).endAccess(assignResponse.getUuid(), null);
        afterEndAccess(response);
        afterEndAccess(assignResponse);
        afterEndAccess(pepSession);
    }

    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test4_AssignCustomIdByCustomId() throws Exception {
        log.info("AssignCustomIdByCustomId");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest, "ziopino2");
        afterTryAccess(pepSession);
        PepSession assignResponse = ((PEP)pep).assignCustomId(null, pepSession.getCustomId(), "ziopino");
        afterTryAccess(assignResponse);
        afterTryAccess(pepSession);
        assertEquals("ziopino", pepSession.getCustomId());
        assertTrue(pep.hasSession(pepSession));
        assertEquals("ziopino", assignResponse.getCustomId());
        assertTrue(pep.hasSession(assignResponse));
        beforeEndAccess(pepSession);
        beforeEndAccess(assignResponse);
        PepSession response = ((PEP)pep).endAccess(null, pepSession.getCustomId());
        afterEndAccess(response);
        afterEndAccess(assignResponse);
        afterEndAccess(pepSession);
    }

    /**
     * Test of startAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test5_TryStartEndCycle() throws Exception {
        log.info("testing try - start - end cycle");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        beforeStartAccess(pepSession);
        PepSession startResponse = pep.startAccess(pepSession);
        afterStartAccess(startResponse);
        afterStartAccess(pepSession);
        beforeEndAccess(startResponse);
        beforeEndAccess(pepSession);
        PepSession endResponse = pep.endAccess(startResponse);
        afterEndAccess(endResponse);
        afterEndAccess(startResponse);
        afterEndAccess(pepSession);
        log.info("ok");
    }
    

    /**
     * Test of startAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test5_TryStartEndCycleWithUuid() throws Exception {
        log.info("testing try - start - end cycle");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        beforeStartAccess(pepSession);
        PepSession startResponse = ((PEP)pep).startAccess(pepSession.getUuid(), null);
        afterStartAccess(pepSession);
        afterStartAccess(pepSession);
        beforeEndAccess(startResponse);
        beforeEndAccess(pepSession);
        PepSession endResponse = ((PEP)pep).endAccess(pepSession.getUuid(), null);
        afterEndAccess(endResponse);
        afterEndAccess(startResponse);
        afterEndAccess(pepSession);
        log.info("ok");
    }
    
    /**
     * Test of startAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test5_TryStartEndCycleWithCustomId() throws Exception {
        log.info("testing try - start - end cycle");
        beforeTryAccess();
        PepSession pepSession = pep.tryAccess(pepRequest);
        afterTryAccess(pepSession);
        beforeStartAccess(pepSession);
        PepSession startResponse = ((PEP)pep).startAccess(null, pepSession.getCustomId());
        afterStartAccess(startResponse);
        afterStartAccess(pepSession);
        beforeEndAccess(startResponse);
        beforeEndAccess(pepSession);
        PepSession endResponse = ((PEP)pep).endAccess(null, pepSession.getCustomId());
        afterEndAccess(endResponse);
        afterEndAccess(startResponse);
        afterEndAccess(pepSession);
        log.info("ok");
    }
    
    
    
    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test6_TryWithNull() throws Exception {
        log.info("start");
        assertEquals(0, pep.getSessions().size());
        try {
            PepSession pepSession = pep.tryAccess(null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(NullPointerException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }

    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test7_StartNoTry() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).startAccess("unexistentUuid", null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test7_StartNoTryWithNull() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).startAccess(null, null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
    
    @Test
    public void test7_StartNoTryWithUuidEmpty() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).startAccess("", null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
    
    @Test
    public void test7_StartNoTryWithCustomId() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).startAccess(null, "unexistentCustomId");
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }

    @Test
    public void test7_StartNoTryWithCustomIdEmpty() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).startAccess(null, "");
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
    
    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test8_EndNoTry() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).endAccess("unexistentUuid", null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
    
    @Test
    public void test8_EndNoTryEmpty() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).endAccess("", null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
   
    @Test
    public void test8_EndNoTryCustomId() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).endAccess(null, "badCustomId");
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
    @Test
    public void test8_EndNoTryCustomIdEmpty() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).endAccess(null, "");
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
    
    @Test
    public void test9_EndTryWithNull() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).endAccess(null, null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }

    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testA_AssignNoTry() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).assignCustomId("unexistentUuid", null, null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }

    
    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testB_TryAssignWithNullId() throws Exception {
        log.info("start");
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(1, pep.getSessions().size());
        try {
            PepSession assignResponse = ((PEP)pep).assignCustomId(pepSession.getUuid(), null, null);
            //assertNotEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  assignResponse);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        } finally {
            pep.endAccess(pepSession);
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }

    
    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testC_EndTwice() throws Exception {
        log.info("start");
        PepSession pepSession = pep.tryAccess(pepRequest, "ziopino2");
        ((PEP)pep).assignCustomId(null, pepSession.getCustomId(), "ziopino");
        assertEquals("ziopino", pepSession.getCustomId());
        assertTrue(pep.hasSession(pepSession));
        ((PEP)pep).endAccess(null, pepSession.getCustomId());
        assertFalse(pep.hasSession(pepSession));
        log.info("end");
    }
    /**
     * Test of assignCustomId method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testD_StartTwice() throws Exception {
        log.info("start");
        PepSession pepSession = pep.tryAccess(pepRequest);
        PepSession startResponse = pep.startAccess(pepSession);
        try {
            PepSession startResponse2 = pep.startAccess(pepSession);
            fail("Must throw XmlRcpException, got instead " +  startResponse2);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        pep.endAccess(startResponse);
        assertFalse(pep.hasSession(pepSession));
        log.info("end");
    }

    /**
     * Test of startAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testE_AssignWithNoIds() throws Exception {
        log.info("start");
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(PepSession.Status.TRY, pepSession.getStatus());
        PepSession response = pep.startAccess(pepSession);
        log.info("response {}", response);
        assertEquals(PepAccessResponse.DecisionEnum.Permit, response.getDecision());
        assertEquals(PepSession.Status.ONGOING, response.getStatus());
        assertEquals(PepAccessResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(PepSession.Status.ONGOING, pepSession.getStatus());
        response = pep.endAccess(response);
        assertEquals(PepSession.Status.DELETED, response.getStatus());
        assertEquals(PepSession.Status.DELETED, pepSession.getStatus());
        log.info("end");
    }

    
}
