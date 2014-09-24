/*
 */
package it.cnr.iit.retrail.demo;

import it.cnr.iit.retrail.client.PEP;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.PepSession;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.xmlrpc.XmlRpcException;

public class Authenticator extends PEP {
    public boolean validate(String user, String password) {
        boolean ok = false;
        try {
            PepAccessRequest req = PepAccessRequest.newInstance(
                    user,
                    "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                    " ",
                    "issuer");
            log.info("sending request: {}", req);
            PepSession s = tryAccess(req);
            log.info("got decision: {}", s);
            ok = s.getDecision() == PepAccessResponse.DecisionEnum.Permit;
        }
        catch(Exception e) {
            log.error("Unexpected exception: {}", e.getMessage());
        }
        return ok;
    }

    public Authenticator(URL pdpUrl, URL myUrl) throws XmlRpcException, UnknownHostException {
        super(pdpUrl, myUrl);
    }
}