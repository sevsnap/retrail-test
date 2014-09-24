/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.demo;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 *
 * @author oneadmin
 */
public class RevokeEvent extends Event {
    
    public static final EventType<RevokeEvent> ACCESS_REVOKED = new EventType(ANY, "ACCESS_REVOKED");
    
    public RevokeEvent() {
        this(ACCESS_REVOKED);
    }
    
    public RevokeEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }
    public RevokeEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
    }  
}