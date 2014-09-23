/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.client.PEP;
import it.cnr.iit.retrail.commons.PepSession;
import java.net.URL;
import java.net.UnknownHostException;
import org.apache.xmlrpc.XmlRpcException;

/**
 *
 * @author oneadmin
 */
public class TestPEP extends PEP {

    public TestPEP(URL pdpUrl, URL myUrl) throws XmlRpcException, UnknownHostException {
        super(pdpUrl, myUrl);
    }
    
    @Override
    public synchronized void onRevokeAccess(PepSession session) throws Exception {
        log.warn("automatic end access disabled for test purposes - {}", session);
    }
}
