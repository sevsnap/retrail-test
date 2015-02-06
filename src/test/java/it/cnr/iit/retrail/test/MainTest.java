/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.server.pip.impl.PIPSessions;
import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.demo.UsageController;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
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
import org.w3c.dom.Document;

/**
 *
 * @author oneadmin
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MainTest {

    static final String pdpUrlString = "http://localhost:8080";
    static final String pepUrlString = "http://localhost:8081";

    static final Logger log = LoggerFactory.getLogger(PIPTest.class);
    static UConInterface ucon = null;
    static PEP pep = null;

    static PIPSessions pipSessions = null;
    static TestPIPTimer pipTimer = null;
    PepRequest pepRequest = null;
    static String lastObligation = null;
    static int revokes = 0;

    public MainTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        log.warn("Setting up environment...");
        try {
            // start server
            ucon = UCon.getInstance();
            ucon.setPolicy(UConInterface.PolicyEnum.PRE, UsageController.class.getResource("/META-INF/policies3/pre3.xml"));
            ucon.init();
            ucon.startRecording(new File(("serverRecord.xml")));
            // start client
            URL pdpUrl = new URL(pdpUrlString);
            URL myUrl = new URL(pepUrlString);

            pep = new PEP(pdpUrl, myUrl) {

                @Override
                public synchronized void onRevokeAccess(PepSession session) throws Exception {
                    log.warn("automatic end access disabled for test purposes - {}", session);
                    revokes++;
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

    /**
     * Test of tryAccess method, of class PEP.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test1_TryListOfSubjectIds() throws Exception {
        log.info("testing pre-access policy");
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        PepSession pepResponse = pep.endAccess(pepSession);
        log.info("ok");
    }

    @Test
    public void test2_CheckDoubleRevocationAtOnce() throws Exception {
        log.info("testing double revocation in bulk mode");
        ucon.stopRecording();
        revokes = 0;
        ucon.startRecording(new File(("serverRecord.xml")));
        PepSession pepSession1 = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession1.getDecision());
        PepSession pepSession2 = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession2.getDecision());
        assertEquals(2, pep.getSessions().size());
        pep.startAccess(pepSession1);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession1.getDecision());
        pep.startAccess(pepSession2);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession1.getDecision());
        log.info("forcing reevaluation of the ONGOING sessions by resetting the on policy");
        ucon.setPolicy(UConInterface.PolicyEnum.ON, UsageController.class.getResource("/META-INF/policies3/on3.xml"));
        Thread.sleep(500);
        assertEquals(2, revokes);
        pep.endAccess(pepSession1);
        pep.endAccess(pepSession2);
        log.info("checking if recorded log is working");
        Document doc = DomUtils.read(new File("serverRecord.xml"));
        assertEquals(1, doc.getElementsByTagName("methodCall").getLength());
        assertEquals(2, doc.getElementsByTagName("Session").getLength());
        log.info("ok");
    }

    @Test
    public void test3_CheckDoubleRevocationAsDistinctCalls() throws Exception {
        log.info("testing double revocation as distinct calls");
        revokes = 0;
        ucon.startRecording(new File(("serverRecord.xml")));
        PepSession pepSession1 = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession1.getDecision());
        pep.assignCustomId(pepSession1.getUuid(), null, "SESSION-1");
        PepSession pepSession2 = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession2.getDecision());
        pep.assignCustomId(pepSession2.getUuid(), null, "SESSION-2");
        assertEquals(2, pep.getSessions().size());
        pep.startAccess(pepSession1);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession1.getDecision());
        log.info("forcing reevaluation of the ONGOING sessions by setting the on policy again");
        ucon.setPolicy(UConInterface.PolicyEnum.ON, UsageController.class.getResource("/META-INF/policies3/on3.xml"));
        Thread.sleep(250);
        assertEquals(1, revokes);
        log.info("resetting the on policy");
        ucon.setPolicy(UConInterface.PolicyEnum.ON, (URL)null);
        pep.startAccess(pepSession2);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession2.getDecision());
        log.info("forcing reevaluation of the ONGOING sessions by setting the on policy again");
        ucon.setPolicy(UConInterface.PolicyEnum.ON, UsageController.class.getResource("/META-INF/policies3/on3.xml"));
        Thread.sleep(250);
        assertEquals(PepResponse.DecisionEnum.Deny, pepSession2.getDecision());
        assertEquals(2, revokes);
        pep.endAccess(pepSession1);
        pep.endAccess(pepSession2);
        log.info("checking if recorded log in append mode is working");
        Document doc = DomUtils.read(new File("serverRecord.xml"));
        assertEquals(2, doc.getElementsByTagName("methodCall").getLength());
        assertEquals(2, doc.getElementsByTagName("Session").getLength());
        log.info("ok");
    }

}
