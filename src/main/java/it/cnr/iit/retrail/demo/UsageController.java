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
    
    static public void changePoliciesTo(String prePath, String onPath, String postPath, String tryStartPath, String tryEndPath) throws Exception {
        ucon.setPolicy(UConInterface.PolicyEnum.PRE, UsageController.class.getResource(prePath));
        ucon.setPolicy(UConInterface.PolicyEnum.ON, UsageController.class.getResource(onPath));
        ucon.setPolicy(UConInterface.PolicyEnum.POST, UsageController.class.getResource(postPath));
        ucon.setPolicy(UConInterface.PolicyEnum.TRYSTART, UsageController.class.getResource(tryStartPath));
        ucon.setPolicy(UConInterface.PolicyEnum.TRYEND, UsageController.class.getResource(tryEndPath));
    }
    
    static public UsageController getInstance() throws Exception {
        if (instance == null) {
            log.info("Setting up Ucon embedded server...");
            ucon = UConFactory.getInstance(new URL(pdpUrlString));
            changePoliciesTo("/META-INF/policies1/pre1.xml",
                             "/META-INF/policies1/on1.xml",
                             "/META-INF/policies1/post1.xml",
                             "/META-INF/policies1/trystart1.xml",
                             "/META-INF/policies1/tryend1.xml"
            );
            pipSessions = new PIPSessions();
            ucon.addPIP(pipSessions);
            TestPIPReputation reputation = new TestPIPReputation();
            reputation.put("Carniani", "bronze");
            reputation.put("Mori", "bronze");
            reputation.put("ZioPino", "bronze");
            reputation.put("visitor", "none");
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
