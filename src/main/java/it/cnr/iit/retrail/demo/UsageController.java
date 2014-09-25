/*
 */
package it.cnr.iit.retrail.demo;

import it.cnr.iit.retrail.client.PEP;
import it.cnr.iit.retrail.commons.PepSession;
import it.cnr.iit.retrail.server.UCon;
import it.cnr.iit.retrail.test.TestPIPReputation;
import it.cnr.iit.retrail.test.TestPIPSessions;
import it.cnr.iit.retrail.test.TestPIPTimer;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.xmlrpc.XmlRpcException;

public class UsageController extends PEP {

    static public final String pdpUrlString = "http://localhost:8080";
    static private final String pepUrlString = "http://localhost:8081";
    static private UsageController instance = null;
    static private UCon ucon = null;

    static private TestPIPSessions pipSessions;
    static private TestPIPTimer pipTimer;
    private MainViewController application = null;
    
    public void setMain(MainViewController application){
        this.application = application;
    }
    
    static public UsageController getInstance() throws Exception {
        if (instance == null) {
            log.info("Setting up Ucon embedded server...");
            ucon = UCon.getInstance(
                    UsageController.class.getResource("/META-INF/policies/pre"),
                    UsageController.class.getResource("/META-INF/policies/on"),
                    UsageController.class.getResource("/META-INF/policies/post"));
            pipSessions = new TestPIPSessions();
            ucon.addPIP(pipSessions);
            TestPIPReputation reputation = new TestPIPReputation();
            reputation.reputationMap.put("carniani", "bronze");
            reputation.reputationMap.put("mori", "bronze");
            ucon.addPIP(reputation);
            pipTimer = new TestPIPTimer(10);
            ucon.addPIP(pipTimer);
            ucon.init();

            log.info("Setting up PEP component");
            instance = new UsageController(new URL(pdpUrlString), new URL(pepUrlString));
            // clean up previous sessions, if any, by clearing the recoverable
            // access flag. This ensures the next heartbeat we'll have a clean
            // ucon status (the first heartbeat is waited by init()).
            instance.setAccessRecoverableByDefault(false);
            instance.init();        // We should have no sessions now
        }
        return instance;
    }

    private UsageController(URL pdpUrl, URL myUrl) throws XmlRpcException, UnknownHostException {
        super(pdpUrl, myUrl);
    }
    
    @Override
    protected boolean shouldRecoverAccess(PepSession session) {
        return super.shouldRecoverAccess(session);
    }

    @Override
    public synchronized void onRevokeAccess(PepSession session) throws Exception {
        log.warn("Firing RevokeEvent for user {}!", session.getCustomId());
        application.onUserMustLeaveRoom(session.getCustomId());
    }
}
