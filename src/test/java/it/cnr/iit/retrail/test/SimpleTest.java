/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.server.impl.UCon;
import it.cnr.iit.retrail.server.impl.UConFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
public class SimpleTest {
    static final String pdpUrlString = "https://localhost:8080";
    static final String pepUrlString = "https://localhost:8081";
    static final String defaultKeystoreName = "/META-INF/keystore.jks";
    static final String defaultKeystorePassword = "uconas4wc";
    static final Logger log = LoggerFactory.getLogger(SimpleTest.class);
    static UCon ucon = null;
    static PEP pep = null;
    PepRequest pepRequest = null;

    public SimpleTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        log.warn("Setting up environment...");
        try {      
            // prepare server 
            URL pdpUrl = new URL(pdpUrlString);
            ucon = UConFactory.getInstance(pdpUrl);
            // Telling server to use a self-signed certificate and
            // trust any client.
            InputStream ks = SimpleTest.class.getResourceAsStream(defaultKeystoreName);
            ucon.trustAllPeers(ks, defaultKeystorePassword);
            // start server
            ucon.init();
            // prepare client
            URL myUrl = new URL(pepUrlString);
            pep = new PEP(pdpUrl, myUrl);
            // clean up previous sessions, if any, by clearing the recoverable
            // access flag. This ensures the next heartbeat we'll have a clean
            // ucon status (the first heartbeat is waited by init()).
            pep.setAccessRecoverableByDefault(false);
            pep.startRecording(new File("clientRecord.xml"));
            ucon.startRecording(new File("serverRecord.xml"));
            // Allowing client to accept a self-signed certificate;
            // allow callbacks to the pep for untrusted ucons.
            ks = SimpleTest.class.getResourceAsStream(defaultKeystoreName);
            pep.trustAllPeers(ks, defaultKeystorePassword);
            // start client
            pep.init();        // We should have no sessions now
        } catch (XmlRpcException | IOException e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        pep.stopRecording();
        pep.term();
        ucon.term();
    }

    @Before
    public void setUp() {
        try {
            pepRequest = PepRequest.newInstance(
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
        while (pep.getSessions().size() > 0) {
            PepSession s = pep.getSessions().iterator().next();
            log.warn("Terminating {}", s);
            s = pep.endAccess(s);
            afterEndAccess(s);
        }
    }

    private void beforeTryAccess() {
        assertEquals(0, pep.getSessions().size());        
    }
    
    private PepSession afterTryAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.getSessions().size());
        assertTrue(pep.hasSession(pepSession));
        assertEquals(Status.STANDARD, pepSession.getStatus());//FIXME was TRY FIXME
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertNotNull(pepSession.getUconUrl());
        assertEquals(pdpUrlString, pepSession.getUconUrl().toString());
        return pepSession;
    }
    
    private void beforeStartAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.getSessions().size());
        assertEquals(Status.STANDARD, pepSession.getStatus());//FIXME was TRY FIXME
    }

    private void afterStartAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.getSessions().size());
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(Status.ONGOING, pepSession.getStatus());
    }

    private void beforeEndAccess(PepSession pepSession) throws Exception {
        assertEquals(1, pep.getSessions().size());
        assertNotEquals(Status.UNKNOWN, pepSession.getStatus());
        assertNotEquals(Status.REVOKED, pepSession.getStatus());
        assertNotEquals(Status.END, pepSession.getStatus()); // FIXME was REJECTED
        assertTrue(pep.hasSession(pepSession));
    }
    
    private void afterEndAccess(PepSession response) throws Exception {
        assertFalse(pep.hasSession(response));
        assertEquals(pdpUrlString, response.getUconUrl().toString());
        assertEquals(Status.END, response.getStatus()); // FIXME was DELETED
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
    public void test5_TryStartEndCycleWithBulkList() throws Exception {
        log.info("testing try - start - try - start - double end cycle");
        beforeTryAccess();
        PepSession pepSession1 = pep.tryAccess(pepRequest);
        PepSession startResponse1 = pep.startAccess(pepSession1);
        PepSession pepSession2 = pep.tryAccess(pepRequest);
        PepSession startResponse2 = pep.startAccess(pepSession2);
        List<PepSession> endList = new ArrayList<>(2);
        endList.add(startResponse1);
        endList.add(startResponse2);
        List<PepSession> endResponses = pep.endAccess(endList);
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRcpException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
    
    @Test
    public void test9_EndTryWithNull1String() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).endAccess((String)null, null);
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRpcException, got instead " +  pepSession);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
    
    @Test
    public void test9_EndTryWithNull2Session() throws Exception {
        log.info("start");
        try {
            PepSession pepSession = ((PEP)pep).endAccess((PepSession)null);
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw NullPointerException, got instead " +  pepSession);
        } catch(NullPointerException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
    
    @Test
    public void test9_EndTryWithNull3StringList() throws Exception {
        log.info("start");
        try {
            List<PepSession> pepSessions = ((PEP)pep).endAccess((List<String>)null, null);
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRpcException, got instead " +  pepSessions);
        } catch(XmlRpcException e) {
            log.info("correctly threw exception: {}", e.getMessage());
        }
        assertEquals(0, pep.getSessions().size());
        log.info("end");
    }
    
    @Test
    public void test9_EndTryWithNull4SessionList() throws Exception {
        log.info("start");
        try {
            List<PepSession> pepSessions = ((PEP)pep).endAccess((List<PepSession>)null);
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw NullPointerException, got instead " +  pepSessions);
        } catch(NullPointerException e) {
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
            fail("Must throw XmlRpcException, got instead " +  pepSession);
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
            //assertNotEquals(PepResponse.DecisionEnum.Permit, pepSession.decision);
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
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(Status.STANDARD, pepSession.getStatus());  // FIXME was TRY
        PepSession response = pep.startAccess(pepSession);
        log.info("response {}", response);
        assertEquals(PepResponse.DecisionEnum.Permit, response.getDecision());
        assertEquals(Status.ONGOING, response.getStatus());
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(Status.ONGOING, pepSession.getStatus());
        response = pep.endAccess(response);
        assertEquals(Status.END, response.getStatus()); // was DELETED
        assertEquals(Status.END, pepSession.getStatus()); // was DELETED
        log.info("end");
    }

    
}
