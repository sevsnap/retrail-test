/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.server.dal.Attribute;
import it.cnr.iit.retrail.server.dal.DAL;
import it.cnr.iit.retrail.server.dal.UconSession;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.beanutils.BeanUtils;
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
import org.xml.sax.SAXException;

/**
 *
 * @author oneadmin
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DALTests {

    static final String pepUrlString = "http://localhost:8081";

    static final Logger log = LoggerFactory.getLogger(DALTests.class);
    static DAL dal = DAL.getInstance();
    static TestPIPReputation pipReputation;
    static TestPIPSessions pipSessions;
    static TestPIPTimer pipTimer;

    public DALTests() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        log.warn("Setting up environment...");
        pipSessions = new TestPIPSessions();
        pipReputation = new TestPIPReputation();
        pipReputation.reputationMap.put("user1", "bronze");
        pipReputation.reputationMap.put("user2", "gold");
        pipTimer = new TestPIPTimer(3);
        pipTimer.setResolution(0.25);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        log.info("done");
    }

    @Before
    public void setUp() throws MalformedURLException {
        pipSessions.init();
        pipReputation.init();
        //pipTimer.init();
        Collection<Attribute> l = dal.listAttributes(new URL(pepUrlString));
        if (l.size() > 0) {
            log.error("no attribute attributed to {} should be in the dal, found {}!", pepUrlString, l.size());
        }
        for (Attribute a : l) {
            log.error("**** listAttributes({}): {}", pepUrlString, a);
        }
        assertEquals(0, l.size());
        l = dal.listAttributes();
        if (l.size() > 0) {
            log.error("no attribute should be in the dal, found {}!", l.size());
        }
        for (Attribute a : l) {
            log.error("**** listAttributes(): {}", a);
        }
        assertEquals(0, l.size());
        Collection<UconSession> u = dal.listSessions();
        if (u.size() > 0) {
            log.error("no session should be in the dal, found {}!", u.size());
        }
        for (UconSession s : u) {
            log.error("**** listSessions(): {}", s);
        }
        assertEquals(0, u.size());
        log.info("after setup everything looks ok!");
    }

    @After
    public void tearDown() throws Exception {
        //pipTimer.term();
        pipSessions.term();
        pipReputation.term();
        Collection<Attribute> l = dal.listAttributes(new URL(pepUrlString));
        if (l.size() > 0) {
            log.error("all attributes for {} in the dal should be cleared, but found {}!", pepUrlString, l.size());
        }
        for (Attribute a : l) {
            log.error("**** listAttributes({}): {}", pepUrlString, a);
        }
        assertEquals(0, l.size());
        l = dal.listAttributes();
        if (l.size() > 0) {
            log.error(", all attributes in the dal should be cleared, but found {}!", l.size());
        }
        for (Attribute a : l) {
            log.error("**** listAttributes(): {}", a);
        }
        assertEquals(0, l.size());
        Collection<UconSession> u = dal.listSessions();
        if (u.size() > 0) {
            log.error(", all attributes in the dal should be cleared, but found {}!", u.size());
        }
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

    private PepSession toPepSession(UconSession uconSession) throws Exception {
        PepSession pepSession = new PepSession(PepResponse.DecisionEnum.Permit, null);
        BeanUtils.copyProperties(pepSession, uconSession);
        pepSession.setUconUrl(new URL(pepUrlString));
        return pepSession;        
    }
    
    private PepSession getPepSession(String uuid) throws Exception {
        return toPepSession(dal.getSession(uuid));
    }
    
    private PepRequest getPepRequest(String uuid) throws Exception {
        // XXX Supports max 1 level parent->children
        UconSession uconSession = dal.getSession(uuid);
        PepRequest pepRequest = new PepRequest();
        for(Attribute a: uconSession.getAttributes()) {
            PepAttribute pepA = new PepAttribute(a.getId(), a.getType(), a.getValue(), a.getIssuer(), a.getCategory(), a.getFactory());
            Attribute p = (Attribute) a.getParent();
            if(p != null) {
                pepA.setParent(new PepAttribute(p.getId(), p.getType(), p.getValue(), p.getIssuer(), p.getCategory(), p.getFactory()));
                log.info("HAS FOUND PARENT: {}", pepA.getParent());
            }
            pepRequest.add(pepA);
        }
        return pepRequest;
    }
       
    private PepAttributeInterface getPepAttributeWithParent(PepRequest r, PepAttribute parent) {
        for(PepAttributeInterface a: r)
            if(a.getParent() == parent)
                return a;
        return null;
    }
    
    @Test
    public void test1_SharedManagedAttribute() throws Exception {
        PepRequest pepRequest1 = newRequest("user1");
        pipSessions.onBeforeTryAccess(pepRequest1);
        log.info("starting session for {}", pepRequest1);
        UconSession uconSession1 = dal.startSession(pepRequest1, new URL(pepUrlString), "custom1");        
        Attribute sessions = dal.listManagedAttributes(pipSessions.getUUID()).iterator().next();
        assertEquals(pipSessions.id, sessions.getId());
        assertEquals(pipSessions.category, sessions.getCategory());
        assertEquals("0", sessions.getValue());
        assertEquals(null, sessions.getParent());
        PepSession pepSession1 = new PepSession(PepResponse.DecisionEnum.Permit, null);
        BeanUtils.copyProperties(pepSession1, uconSession1);
        pepSession1.setUconUrl(new URL(pepUrlString));
        pipSessions.onAfterTryAccess(pepRequest1, pepSession1);
        dal.endSession(uconSession1);
        assertEquals(0, dal.listManagedAttributes(pipSessions.getUUID()).size());
    }
    
    @Test
    public void test2_PrivateUnmanagedAttributeWithOneSession() throws Exception {
        PepRequest pepRequest = newRequest("user1");
        log.info("emulating tryAccess for {}", pepRequest);
        pipReputation.onBeforeTryAccess(pepRequest);
        UconSession uconSession = dal.startSession(pepRequest, new URL(pepUrlString), "custom1");
        String sessionUuid = uconSession.getUuid();
        Attribute reputation = dal.listUnmanagedAttributes(pipReputation.getUUID()).iterator().next();
        assertEquals(pipReputation.id, reputation.getId());
        assertEquals(pipReputation.category, reputation.getCategory());
        assertEquals("bronze", reputation.getValue());
        assertNotEquals(null, reputation.getParent());
        assertEquals(pipReputation.subjectId, reputation.getParent().getId());
        assertEquals(pipReputation.category, reputation.getParent().getCategory());
        assertEquals("user1", reputation.getParent().getValue());
        pipReputation.onAfterTryAccess(pepRequest, toPepSession(uconSession));
        log.info("tryAccess emulated correcly");
        dal.endSession(dal.getSession(sessionUuid));
        assertEquals(0, dal.listUnmanagedAttributes(pipReputation.getUUID()).size());
    }

    private void assertReputation(String sessionUUID, String reputation, String forUserValue) throws Exception {
        // check if added attribute is correct
        PepRequest r = getPepRequest(sessionUUID);
        PepAttributeInterface a = r.getAttribute(pipReputation.category, pipReputation.id);
        log.error("***** reputation: {}, parent: {}", a, a.getParent());
        assertNotEquals(null, a);
        assertEquals(reputation, a.getValue());
        assertEquals(forUserValue, a.getParent().getValue());
    }
    
    @Test
    public void test2_PrivateUnmanagedAttributeWithTwoSessionsShouldNotMessUp() throws Exception {
        PepRequest pepRequest1 = newRequest("user1");
        log.info("emulating tryAccess for {}", pepRequest1);
        pipReputation.onBeforeTryAccess(pepRequest1);
        UconSession uconSession1 = dal.startSession(pepRequest1, new URL(pepUrlString), "custom1");
        String sessionUuid1 = uconSession1.getUuid();
        assertReputation(sessionUuid1, "bronze", "user1");
        pipReputation.onAfterTryAccess(pepRequest1, toPepSession(uconSession1));
        log.info("tryAccess emulated correcly");
        PepRequest pepRequest2 = newRequest("user2");
        log.info("emulating tryAccess for {}", pepRequest2);
        pipReputation.onBeforeTryAccess(pepRequest2);
        UconSession uconSession2 = dal.startSession(pepRequest2, new URL(pepUrlString), "custom2");
        String sessionUuid2 = uconSession2.getUuid();
        assertNotEquals(sessionUuid1, sessionUuid2);
        // check if added attribute is correct
        assertReputation(sessionUuid2, "gold", "user2");
        assertReputation(sessionUuid1, "bronze", "user1");
        pipReputation.onAfterTryAccess(pepRequest2, toPepSession(uconSession2));
        assertReputation(sessionUuid2, "gold", "user2");
        assertReputation(sessionUuid1, "bronze", "user1");
        log.info("tryAccess emulated correcly");
        dal.endSession(dal.getSession(sessionUuid1));
        assertEquals(1, dal.listUnmanagedAttributes(pipReputation.getUUID()).size());
        dal.endSession(dal.getSession(sessionUuid2));
        assertEquals(0, dal.listUnmanagedAttributes(pipReputation.getUUID()).size());
    }

}
