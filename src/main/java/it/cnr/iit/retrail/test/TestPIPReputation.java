/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.commons.PepSession;
import it.cnr.iit.retrail.server.pip.impl.PIP;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */

public class TestPIPReputation extends PIP {
    public final Map<String, String> reputationMap;
    
    public TestPIPReputation() {
        super();
        this.reputationMap = new HashMap<>();
        this.log = LoggerFactory.getLogger(TestPIPReputation.class);
    }
    
    @Override
    public void onBeforeTryAccess(PepAccessRequest request) {
        PepRequestAttribute subject = request.getCategories().get("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject").iterator().next();
        String reputation = reputationMap.get(subject.value);
        if(reputation != null) {
            log.warn("subject {} has reputation {}", subject.value, reputation);
            PepRequestAttribute test = newPrivateAttribute("reputation", "http://www.w3.org/2001/XMLSchema#string", reputation, "http://localhost:8080/federation-id-prov/saml", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
            test.expires = new Date();
            request.add(test);
        } else 
            log.warn("subject {} has no reputation attribute -- ignoring", subject.value);
    }
    
    @Override
    protected void refresh(PepRequestAttribute pepAttribute, PepSession session) {
        pepAttribute.expires = new Date();
    }
}
