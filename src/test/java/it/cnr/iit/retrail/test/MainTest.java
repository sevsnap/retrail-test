/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.demo.UsageController;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import it.cnr.iit.retrail.server.impl.UConFactory;
import static it.cnr.iit.retrail.test.DALTest.dal;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import static junit.framework.TestCase.assertEquals;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author oneadmin
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MainTest {

    static final String pdpUrlString = "http://localhost:8093";
    static final String pepUrlString = "http://localhost:8094";

    static final Logger log = LoggerFactory.getLogger(PIPTest.class);
    static UCon ucon = null;
    static PEP pep = null;

    PepRequest pepRequest = null;
    static String lastObligation = null;
    static private final Object revokeMonitor = new Object();
    static private int revoked = 0;

    public MainTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        log.warn("Setting up environment...");
        try {
            URL pdpUrl = new URL(pdpUrlString);
            URL myUrl = new URL(pepUrlString);
            // start server
            ucon = UConFactory.getInstance(pdpUrl);
            ucon.loadConfiguration(UsageController.class.getResourceAsStream("/MainTest.xml"));
            ucon.init();
            //ucon.startRecording(new File(("serverRecord.xml")));
            // start client

            pep = new PEP(pdpUrl, myUrl) {

                @Override
                public void onRecoverAccess(PepSession session) throws Exception {
                    // Remove previous run stale sessions
                    endAccess(session);
                }
                
                @Override
                public void onRevokeAccess(PepSession session) throws Exception {
                    synchronized(revokeMonitor) {
                        revoked++;
                        revokeMonitor.notifyAll();
                    }
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
            revoked = 0;
            Collection<UconSession> u = dal.listSessions();
            if (u.size() > 0) {
                log.error("no session should be in the dal, found {}!", u.size());
            }
            for (UconSession s : u) {
                log.error("**** listSessions(): {}", s);
            }
            assertEquals(0, u.size());

            pepRequest = PepRequest.newInstance("Carniani", "MANAGE", "VM", "ziopino");
            PepAttribute a = new PepAttribute(PepAttribute.IDS.ACTION,
                    PepAttribute.DATATYPES.STRING, "USE", "ziopino",
                    PepAttribute.CATEGORIES.ACTION);
            pepRequest.add(a);
            PepAttribute b = new PepAttribute("urn:oasis:names:tc:xacml:1.0:environment:environment-id",
                    PepAttribute.DATATYPES.STRING, "0", "ziopino",
                    "urn:oasis:names:tc:xacml:3.0:attribute-category:environment");
            pepRequest.add(b);
        } catch (Exception e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    private void openConcurrentSessions(int n) throws Exception {
        log.info("sequentially opening {} concurrent sessions", n);
        long startMs = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            log.info("opening session:  {} ", i);
            PepSession pepSession = pep.tryAccess(pepRequest);
            assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("ok, {} concurrent sessions opened; total tryAccess time [T{}] = {} ms, normalized {} ms", n, n, elapsedMs, elapsedMs/n);
    }
    
    private void startConcurrentSessions() throws Exception {
        int n = pep.getSessions().size();
        log.info("sequentially starting concurrent sessions");
        long startMs = System.currentTimeMillis();
        for (PepSession pepSession: pep.getSessions()) {
            log.info("starting session:  {} ", pepSession);
            pepSession = pep.startAccess(pepSession);
            assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("ok, concurrent sessions opened; total startAccess time [St{}] = {} ms, normalized = {} ms",  n, elapsedMs, elapsedMs/n);
    }

    private void closeConcurrentSessions() throws Exception {
        int n = pep.getSessions().size();
        log.info("sequentially closing {} all sessions", n);
        long startMs = System.currentTimeMillis();
        while (!pep.getSessions().isEmpty()) {
            PepSession pepSession = pep.getSessions().iterator().next();
            pep.endAccess(pepSession);
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("all {} sessions closed; total endAccess time [E{}] = {} ms, normalized = {} ms", n, n, elapsedMs, elapsedMs/n);
    }
    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    
    private void testRawRequest(String resourceName) throws Exception {
        log.info("testing raw xmlrpc request (not using the PEP interface): {}", resourceName);
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(pdpUrlString));
        config.setEnabledForExtensions(true);
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        InputStream is = getClass().getResourceAsStream(resourceName);
        Document doc = DomUtils.read(is);
        //log.info("request = {}", DomUtils.toString(doc));
        Object[] params = {doc.getDocumentElement(), pepUrlString, "rawreq"};
        Document reply = (Document) client.execute("UCon.tryAccess", params);
        Element decision = (Element) reply.getElementsByTagNameNS("*", "Decision").item(0);
        assertEquals("Permit", decision.getTextContent());
        Object[] params2 = {null, "rawreq"};
        reply = (Document) client.execute("UCon.endAccess", params2);
        decision = (Element) reply.getElementsByTagNameNS("*", "Decision").item(0);
        assertEquals("Permit", decision.getTextContent());
        log.info("ok");
    }
    
    @Test
    public void test1_TryListOfSubjectIdsRawReq1() throws Exception {
        log.info("testing separated Attribute+AttributeValue");
        testRawRequest("/rawRequest.xml");
    }

    @Test
    public void test1_TryListOfSubjectIdsRawReq2() throws Exception {
        log.info("testing joint Attribute+AttributeValue(s)");
        testRawRequest("/rawRequest2.xml");
    }
    
    @Test
    public void test2_TryListOfSubjectIds() throws Exception {
        log.info("testing pre-access policy");
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        PepSession pepResponse = pep.endAccess(pepSession);
        log.info("ok");
    }

    @Test
    public void test3_CheckAsyncNotifier() throws Exception {
        log.info("testing multiple revocations");
        revoked = 0;
        ucon.startRecording(new File("serverRecord.xml"));
        int n = 10;
        openConcurrentSessions(n);
        startConcurrentSessions();
        log.info("forcing reevaluation of the ONGOING sessions");
        ucon.wakeup();
        long startMs = System.currentTimeMillis();
        log.info("waiting for {} revocations", n);
        synchronized (revokeMonitor) {
            while (revoked < n) {
                revokeMonitor.wait();
                log.info("currently revoked {} of {} sessions", revoked, n);
            }
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("{} of {} sessions revoked; total revokeAccess time for PIP [R{}] = {} ms, normalized = {} ms", revoked, n, n, elapsedMs, elapsedMs/n);
        closeConcurrentSessions();
        ucon.stopRecording();
        log.info("checking if recorded log is working");
        Document doc = DomUtils.read(new File("serverRecord.xml"));
        //int revokeAccessCalls = doc.getElementsByTagName("methodCall").getLength();
        int revocations = doc.getElementsByTagNameNS("*", "Session").getLength();
        assertEquals(n, revocations);
        assertEquals(n, revoked);
        log.info("ok");
    }

}
