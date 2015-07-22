/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.demo;

import it.cnr.facedetection.MotionDetection;
import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.impl.UConFactory;
import java.net.URL;

public class UsageController extends PEP {

    static public final String pdpUrlString = "http://0.0.0.0:8090";
    static private final String pepUrlString = "http://localhost:8091";
    static private UsageController instance = null;
    static private UConInterface ucon = null;

    private MainViewController application = null;

    public void setMain(MainViewController application) {
        this.application = application;
    }

    static public void loadBehaviour(String resourceName) throws Exception {
        ucon.loadConfiguration(UsageController.class.getResourceAsStream(resourceName));
    }

    static public UsageController getInstance() throws Exception {
        if (instance == null) {
            log.info("Setting up Ucon embedded server...");
            ucon = UConFactory.getInstance(new URL(pdpUrlString));
            loadBehaviour("/ucon4.xml");
            ucon.init();

            log.info("Setting up PEP component");
            instance = new UsageController(new URL(pdpUrlString), new URL(pepUrlString));
            instance.init();        // We should have no sessions now
            
            MotionDetection.startDetection();
        }
        return instance;
    }

    private UsageController(URL pdpUrl, URL myUrl) throws Exception {
        super(pdpUrl, myUrl);
    }

    @Override
    public void onRecoverAccess(PepSession session) throws Exception {
        // Remove previous run stale sessions
        endAccess(session);
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
    
    public UConInterface getUcon(){
        return ucon;
    }
    
    public MainViewController getMainViewController(){
        return application;
    }
}
