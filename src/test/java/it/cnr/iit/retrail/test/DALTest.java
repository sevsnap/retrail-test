/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.server.pip.impl.PIPSessions;
import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.DAL;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
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
public class DALTest {

    static final String pepUrlString = "http://localhost:8081";
    static URL uconUrl;

    static final Logger log = LoggerFactory.getLogger(DALTest.class);
    static DAL dal = DAL.getInstance();
    static TestPIPReputation pipReputation;
    static PIPSessions pipSessions;
    static TestPIPTimer pipTimer;

    public DALTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        log.info("Setting up environment...");
        uconUrl = new URL("http://localhost:8080");
        pipSessions = new PIPSessions();
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
        dal.begin();
        pipSessions.init(null);
        pipReputation.init(null);
        assertEquals(0, dal.listSessions().size());
        //pipTimer.init();
        Collection<UconAttribute> l = dal.listAttributes(new URL(pepUrlString));
        if (l.size() > 0) {
            log.error("no attribute attributed to {} should be in the dal, found {}!", pepUrlString, l.size());
        }
        for (UconAttribute a : l) {
            log.error("**** listAttributes({}): {}", pepUrlString, a);
        }
        assertEquals(0, l.size());
        l = dal.listAttributes();
        if (l.size() > 0) {
            log.error("no attribute should be in the dal, found {}!", l.size());
        }
        for (UconAttribute a : l) {
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
        Collection<UconSession> u = dal.listSessions();
        if (u.size() > 0) {
            log.error(", all sessions in the dal should be cleared, but found {}!", u.size());
        }
        for (UconSession s : u) {
            log.error("**** listSessions(): {}", s);
        }
        assertEquals(0, u.size());
        Collection<UconAttribute> l = dal.listAttributes(new URL(pepUrlString));
        if (l.size() > 0) {
            log.error("all attributes for {} in the dal should be cleared, but found {}!", pepUrlString, l.size());
        }
        for (UconAttribute a : l) {
            log.error("**** listAttributes({}): {}", pepUrlString, a);
        }
        assertEquals(0, l.size());
/*
        l = dal.listAttributes();
        if (l.size() > 0) {
            log.error(", all attributes in the dal should be cleared, but found {}!", l.size());
        }
        for (UconAttribute a : l) {
            log.error("**** listAttributes(): {}", a);
        }
        assertEquals(0, l.size());
        */
        assertEquals(0, dal.listUnmanagedAttributes(pipReputation.getUUID()).size());
        assertEquals(0, dal.listManagedAttributes(pipReputation.getUUID()).size());
        assertEquals(0, dal.listUnmanagedAttributes(pipSessions.getUUID()).size());
        assertEquals(0, dal.listManagedAttributes(pipSessions.getUUID()).size());
        log.info("after teardown everything looks ok!");
        dal.rollback();
    }

    private UconRequest newRequest(String subjectValue) throws Exception {
        PepRequest pepRequest = PepRequest.newInstance(
                subjectValue,
                "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                " ",
                "issuer");
        UconRequest uconRequest = new UconRequest();
        uconRequest.copy(pepRequest);
        return uconRequest;
    }
       
    
    private void assertSessionsValueEquals(String value) {
        Collection<PepAttributeInterface> l = dal.listManagedAttributes(pipSessions.getUUID());
        assertEquals(1, l.size());
        UconAttribute sessions = (UconAttribute) l.iterator().next();
        assertEquals(pipSessions.id, sessions.getId());
        assertEquals(pipSessions.category, sessions.getCategory());
        assertEquals(null, sessions.getParent());
        assertEquals(value, sessions.getValue());
    }
    
    @Test
    public void test1_SharedManagedAttributeShouldWork() throws Exception {
        UconRequest uconRequest1 = newRequest("user1");
        pipSessions.onBeforeTryAccess(uconRequest1);
        log.info("starting session for {}", uconRequest1);
        UconSession uconSession1 = new UconSession();
        uconSession1.setPepUrl(pepUrlString);
        uconSession1.setCustomId("custom1");
        uconSession1.setStateType(StateType.BEGIN);
        uconSession1.setStateName("INIT"); // FIXME
        uconSession1 = dal.startSession(uconSession1, uconRequest1);  
        assertSessionsValueEquals("0");
        pipSessions.onAfterTryAccess(uconRequest1, uconSession1);
        uconSession1.setStateType(StateType.ONGOING);
        uconSession1.setStateName("ONGOING"); // FIXME
        log.info("calling saveSession() after onAfterTryAccess");
        dal.saveSession(uconSession1, uconRequest1);
        log.info("tryAccess emulated correctly");
        pipSessions.onBeforeStartAccess(uconRequest1, uconSession1);
        dal.saveSession(uconSession1, uconRequest1);
        pipSessions.onAfterStartAccess(uconRequest1, uconSession1);
        dal.saveSession(uconSession1, uconRequest1);
        assertSessionsValueEquals("1");
        log.info("startAccess emulated correctly");
        pipSessions.onBeforeEndAccess(uconRequest1, uconSession1);
        dal.saveSession(uconSession1, uconRequest1);
        pipSessions.onAfterEndAccess(uconRequest1, uconSession1);
        dal.saveSession(uconSession1, uconRequest1);
        assertSessionsValueEquals("0");
        dal.endSession(uconSession1);
        log.info("endAccess emulated correctly");
        assertEquals(0, dal.listSessions().size());
//        assertEquals(0, dal.listManagedAttributes(pipSessions.getUUID()).size());
        assertEquals(0, dal.listUnmanagedAttributes(pipSessions.getUUID()).size());
//        assertEquals(0, dal.listAttributes().size());
    }
    
    private void assertReputationValueEquals(String value, String forUser) {
        Collection<PepAttributeInterface> l = dal.listUnmanagedAttributes(pipReputation.getUUID());
        assertEquals(1, l.size());
        UconAttribute reputation = (UconAttribute) l.iterator().next();
        assertEquals(pipReputation.id, reputation.getId());
        assertEquals(pipReputation.category, reputation.getCategory());
        assertEquals(value, reputation.getValue());
        assertNotEquals(null, reputation.getParent());
        assertEquals(pipReputation.subjectId, reputation.getParent().getId());
        assertEquals(pipReputation.category, reputation.getParent().getCategory());
        assertEquals(forUser, reputation.getParent().getValue());
        assertEquals(reputation.getParent().getChildren().iterator().next(), reputation);
    }
    
    @Test
    public void test2_PrivateUnmanagedAttributeWithOneSessionShouldWork() throws Exception {
        UconRequest uconRequest1 = newRequest("user1");
        log.info("emulating tryAccess for {}", uconRequest1);
        pipReputation.onBeforeTryAccess(uconRequest1);
        UconSession uconSession1 = new UconSession();
        uconSession1.setPepUrl(pepUrlString);
        uconSession1.setCustomId("custom1");
        uconSession1.setStateType(StateType.BEGIN);
        uconSession1.setStateName("INIT"); //FIXME
        uconSession1 = dal.startSession(uconSession1, uconRequest1);
        assertReputationValueEquals("bronze", "user1");
        log.info("tryAccess emulated correctly");

        pipReputation.onBeforeEndAccess(uconRequest1, uconSession1);
        dal.saveSession(uconSession1, uconRequest1);
        pipReputation.onAfterEndAccess(uconRequest1, uconSession1);
        dal.saveSession(uconSession1, uconRequest1);
        dal.endSession(dal.getSession(uconSession1.getUuid(), uconUrl));
        log.info("endAccess emulated correctly");

        assertEquals(0, dal.listSessions().size());
//        assertEquals(0, dal.listAttributes().size());
        assertEquals(0, dal.listManagedAttributes(pipReputation.getUUID()).size());
        assertEquals(0, dal.listUnmanagedAttributes(pipReputation.getUUID()).size());
    }

    private void assertReputation(String sessionUUID, String reputation, String forUserValue) throws Exception {
        // check if added attribute is correct
        UconSession uconSession = dal.getSession(sessionUUID, uconUrl);
        PepRequest r = new UconRequest();
        for(UconAttribute a: uconSession.getAttributes()) {
            r.add(a);
        }
        UconAttribute a = (UconAttribute) r.getAttribute(pipReputation.category, pipReputation.id);
        //log.error("***** reputation: {}, parent: {}", a, a.getParent());
        assertNotEquals(null, a);
        assertEquals(reputation, a.getValue());
        assertEquals(forUserValue, a.getParent().getValue());
    }
    
    @Test
    public void test2_PrivateUnmanagedAttributeWithTwoSessionsShouldNotMessUp() throws Exception {
        UconRequest uconRequest1 = newRequest("user1");
        log.info("emulating tryAccess for {}", uconRequest1);
        pipReputation.onBeforeTryAccess(uconRequest1);
        UconSession uconSession1 = new UconSession();
        uconSession1.setUconUrl(new URL(pepUrlString));
        uconSession1.setCustomId("custom1");
        uconSession1.setStateType(StateType.BEGIN);
        uconSession1.setStateName("INIT"); //FIXME
        uconSession1 = dal.startSession(uconSession1, uconRequest1);
        String sessionUuid1 = uconSession1.getUuid();
        assertReputation(sessionUuid1, "bronze", "user1");
        pipReputation.onAfterTryAccess(uconRequest1, uconSession1);
        log.info("tryAccess emulated correcly");
        UconRequest uconRequest2 = newRequest("user2");
        log.info("emulating tryAccess for {}", uconRequest2);
        pipReputation.onBeforeTryAccess(uconRequest2);
        UconSession uconSession2 = new UconSession();
        uconSession2.setUconUrl(new URL(pepUrlString));
        uconSession2.setCustomId("custom2");
        uconSession2.setStateType(StateType.BEGIN);
        uconSession2.setStateName("INIT"); //FIXME
        uconSession2 = dal.startSession(uconSession2, uconRequest2);
        String sessionUuid2 = uconSession2.getUuid();
        assertNotEquals(sessionUuid1, sessionUuid2);
        // check if added attribute is correct
        assertReputation(sessionUuid2, "gold", "user2");
        assertReputation(sessionUuid1, "bronze", "user1");
        pipReputation.onAfterTryAccess(uconRequest2, uconSession2);
        assertReputation(sessionUuid2, "gold", "user2");
        assertReputation(sessionUuid1, "bronze", "user1");
        log.info("tryAccess emulated correctly");
        dal.endSession(dal.getSession(sessionUuid1, uconUrl));
        assertEquals(1, dal.listUnmanagedAttributes(pipReputation.getUUID()).size());
        dal.endSession(dal.getSession(sessionUuid2, uconUrl));
        assertEquals(0, dal.listUnmanagedAttributes(pipReputation.getUUID()).size());
    }

    @Test
    public void test3_sessionGaloreWithPrivateAttributes() throws Exception {
        log.info("start");
        assertEquals(0, dal.listSessions().size());
        for(int i = 0; i < 2500; i++) {
            log.info("create session: {}", i);
            UconSession uconSession1 = new UconSession();
            uconSession1.setPepUrl(pepUrlString);
            uconSession1.setCustomId("custom."+i);
            uconSession1.setStateType(StateType.BEGIN);
            uconSession1.setStateName("INIT"); //FIXME
            UconRequest uconRequest = newRequest("user1");
            dal.startSession(uconSession1, uconRequest);
        }
        for(UconSession uconSession: dal.listSessions())
            dal.endSession(uconSession);
        assertEquals(0, dal.listSessions().size());
        log.info("end");
    }
  
    @Test
    public void test4_sessionGaloreWithSharedAttributes() throws Exception {
        log.info("start");
        assertEquals(0, dal.listSessions().size());
        for(int i = 0; i < 2500; i++) {
            log.info("create session: {}", i);
            UconSession uconSession1 = new UconSession();
            uconSession1.setPepUrl(pepUrlString);
            uconSession1.setCustomId("custom."+i);
            uconSession1.setStateType(StateType.BEGIN);
            uconSession1.setStateName("INIT"); //FIXME
            UconRequest uconRequest = newRequest("user1");
            UconAttribute shared = dal.newSharedAttribute("ID"+i, "TYPE", "VALUE", "ISSUER", "PEPURL", "FACTORYID");
            uconRequest.add(shared);
            dal.startSession(uconSession1, uconRequest);
        }
        for(UconSession uconSession: dal.listSessions())
            dal.endSession(uconSession);
        assertEquals(0, dal.listSessions().size());
        log.info("end");
    }
    
}
