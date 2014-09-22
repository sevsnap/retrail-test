/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and onTryAccess the template in the editor.
 */

package it.cnr.iit.retrail.test;

import it.cnr.iit.retrail.server.UCon;

/**
 *
 * @author oneadmin
 */
public class ServerTest {

    public static void main(String[] args) throws Exception {
        UCon ucon = UCon.getInstance(
                ServerTest.class.getResource("/META-INF/policies/pre"), 
                ServerTest.class.getResource("/META-INF/policies/on"), 
                ServerTest.class.getResource("/META-INF/policies/post"));
        ucon.addPIP(new TestPIPSessions(1));
        ucon.addPIP(new TestPIPReputation("bronze"));
        ucon.addPIP(new TestPIPTimer(16));
        ucon.init();
        ucon.forever();
        ucon.term();         
    }
    
}
