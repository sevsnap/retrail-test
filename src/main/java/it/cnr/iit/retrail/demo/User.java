/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package it.cnr.iit.retrail.demo;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.PepSession;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class User {

    static final Logger log = LoggerFactory.getLogger(User.class);
    static final Map<String, User> users = new HashMap<>();
    private final String id;
    private PepSession pepSession = null;

    private User(String id) {
        this.id = id;
    }

    static public User getInstance(String id) throws Exception {
        User user = users.get(id);
        if(user == null) {
            user = new User(id);
            users.put(id, user);
        }
        return user;
    }

    public String getId() {
        return id;
    }

    public boolean goToDoor() {
        boolean ok = false;
        try {
            PepAccessRequest req = PepAccessRequest.newInstance(
                    id,
                    "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                    " ",
                    "issuer");
            log.info("sending request: {}", req);
            pepSession = UsageController.getInstance().tryAccess(req, id);
            log.info("got decision: {}", pepSession);
            ok = pepSession.getDecision() == PepAccessResponse.DecisionEnum.Permit;
        } catch (Exception e) {
            log.error("Unexpected exception: {}", e.getMessage());
        }
        return ok;
    }

    public boolean enterRoom() {
        boolean ok = false;
        try {
            pepSession = UsageController.getInstance().startAccess(pepSession);
            ok = pepSession.getDecision() == PepAccessResponse.DecisionEnum.Permit;
        } catch (Exception e) {
            log.error("Unexpected exception: {}", e.getMessage());
        }
        return ok;
    }

    public boolean leave() {
        boolean ok = false;
        try {
            pepSession = UsageController.getInstance().endAccess(pepSession);
            ok = pepSession.getDecision() == PepAccessResponse.DecisionEnum.Permit;
            if(ok)
                users.remove(id);
        } catch (Exception e) {
            log.error("Unexpected exception: {}", e.getMessage());
        }
        return ok;
    }

}
