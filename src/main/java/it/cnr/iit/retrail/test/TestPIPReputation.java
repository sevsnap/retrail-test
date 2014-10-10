/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
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
        
    public final String id = "reputation";
    public final String category = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
    public final String subjectId = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
    
    
    public TestPIPReputation() {
        super();
        this.reputationMap = new HashMap<>();
        this.log = LoggerFactory.getLogger(TestPIPReputation.class);
    }
    
    @Override
    public void onBeforeTryAccess(PepRequestInterface request) {
        PepAttributeInterface subject = request.getAttribute(category, subjectId);
        String reputation = reputationMap.get(subject.getValue());
        if(reputation != null) {
            log.info("{} - subject {} has reputation {}", request, subject.getValue(), reputation);
            PepAttributeInterface test = newPrivateAttribute(id, "http://www.w3.org/2001/XMLSchema#string", reputation, "http://localhost:8080/federation-id-prov/saml", subject);
            // Make attribute unmanaged (automatically managed by the UCon)
            // because we set an expiry date
            test.setExpires(new Date());
            request.add(test);
        } else 
            log.warn("subject {} has no reputation attribute -- ignoring", subject.getValue());
    }
    
    @Override
    protected void refresh(PepAttributeInterface pepAttribute, PepSessionInterface session) {
        pepAttribute.setExpires(new Date());
    }
}
