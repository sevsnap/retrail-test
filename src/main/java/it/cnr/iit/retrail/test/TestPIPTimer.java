/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and onTryAccess the template in the editor.
 */

package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.server.pip.StandAlonePIP;
import java.util.Collection;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public class TestPIPTimer extends StandAlonePIP {
    final private int timeToGo;
    final private int resolution = 1;
    
    public TestPIPTimer(int timeToGo) {
        super();
        this.log = LoggerFactory.getLogger(TestPIPTimer.class);
        this.timeToGo = timeToGo;
    }
    
    @Override
    public void onStartAccess(PepAccessRequest request) {
        PepRequestAttribute a = newAttribute("timer", "http://www.w3.org/2001/XMLSchema#integer", Integer.toString(timeToGo), "http://localhost:8080/federation-id-prov/saml", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
        request.add(a);
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(1000*resolution);
                Collection<PepRequestAttribute> attributes = listAttributes();
                for(PepRequestAttribute a: attributes) {
                    Integer ttg = Integer.parseInt(a.value);
                    if(ttg > 0) {
                        ttg = Integer.max(0, ttg - resolution); 
                        a.value = ttg.toString(); 
                        log.info("awaken {}", a);
                        notifyChanges(a);
                    }
                }
            } catch (InterruptedException ex) {
                log.error("interrupted");
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }
    }
    
}