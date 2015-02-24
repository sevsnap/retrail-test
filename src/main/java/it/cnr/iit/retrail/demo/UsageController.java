/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.demo;

import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.impl.UConFactory;
import it.cnr.iit.retrail.test.TestPIPReputation;
import it.cnr.iit.retrail.server.pip.impl.PIPSessions;
import it.cnr.iit.retrail.test.TestPIPTimer;
import java.net.URL;

public class UsageController extends PEP {

    static public final String pdpUrlString = "http://0.0.0.0:8080";
    static private final String pepUrlString = "http://localhost:8081";
    static private UsageController instance = null;
    static private UConInterface ucon = null;

    static private PIPSessions pipSessions;
    static private TestPIPTimer pipTimer;
    private MainViewController application = null;
    
    public void setMain(MainViewController application){
        this.application = application;
    }
    
    static public void loadBehaviour(String resourceName) throws Exception {
        ucon.loadBehaviour(UsageController.class.getResourceAsStream(resourceName));
    }
    
    static public UsageController getInstance() throws Exception {
        if (instance == null) {
            log.info("Setting up Ucon embedded server...");
            ucon = UConFactory.getInstance(new URL(pdpUrlString));
            loadBehaviour("/META-INF/ucon1.xml");
            pipSessions = new PIPSessions();
            ucon.getPIPChain().add(pipSessions);
            TestPIPReputation reputation = new TestPIPReputation();
            reputation.put("Carniani", "bronze");
            reputation.put("Mori", "bronze");
            reputation.put("ZioPino", "bronze");
            reputation.put("visitor", "none");
            ucon.getPIPChain().add(reputation);
            pipTimer = new TestPIPTimer(10);
            ucon.getPIPChain().add(pipTimer);
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

    private UsageController(URL pdpUrl, URL myUrl) throws Exception {
        super(pdpUrl, myUrl);
    }
    
    @Override
    protected boolean shouldRecoverAccess(PepSession session) {
        return super.shouldRecoverAccess(session);
    }

    @Override
    public synchronized void onRevokeAccess(PepSession session) throws Exception {
        log.warn("Firing RevokeEvent for user {}!", session.getCustomId());
        application.onRevoke(session);
    }
    
    @Override
    public synchronized void onObligation(PepSession session, String obligation) throws Exception {
        application.onObligation(session, obligation);  
    }
}
