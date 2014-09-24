/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
package it.cnr.iit.retrail.demo;

import static it.cnr.iit.retrail.demo.Main.log;
import it.cnr.iit.retrail.server.UCon;
import it.cnr.iit.retrail.test.TestPIPReputation;
import it.cnr.iit.retrail.test.TestPIPSessions;
import it.cnr.iit.retrail.test.TestPIPTimer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Application. This class handles navigation and user session.
 */
public class Main extends Application {
    static final Logger log = LoggerFactory.getLogger(Main.class);
    
    private Stage stage;
    private User loggedUser;
    private final double MINIMUM_WINDOW_WIDTH = 390.0;
    private final double MINIMUM_WINDOW_HEIGHT = 500.0;
    
    private final String pdpUrlString = "http://localhost:8080";
    private final String pepUrlString = "http://localhost:8081";
    private UCon ucon;
    private Authenticator authenticator;
    private TestPIPSessions pipSessions;
    private TestPIPTimer pipTimer;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(Main.class, (java.lang.String[])null);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
             log.info("Setting up Ucon server...");
            ucon = UCon.getInstance(
                    getClass().getResource("/META-INF/policies/pre"),
                    getClass().getResource("/META-INF/policies/on"),
                    getClass().getResource("/META-INF/policies/post"));
            pipSessions = new TestPIPSessions();
            ucon.addPIP(pipSessions);
            TestPIPReputation reputation = new TestPIPReputation();
            reputation.reputationMap.put("carniani", "bronze");
            reputation.reputationMap.put("mori", "gold");
            ucon.addPIP(reputation);
            pipTimer = new TestPIPTimer(2);
            ucon.addPIP(pipTimer);
            ucon.init();
            
            log.info("Setting up PEP client...");
            URL pdpUrl = new URL(pdpUrlString);
            URL myUrl = new URL(pepUrlString);
            authenticator = new Authenticator(pdpUrl, myUrl);
            // clean up previous sessions, if any, by clearing the recoverable
            // access flag. This ensures the next heartbeat we'll have a clean
            // ucon status (the first heartbeat is waited by init()).
            authenticator.setAccessRecoverableByDefault(false);
            authenticator.init();        // We should have no sessions now
            
            log.info("Firing GUI...");
            stage = primaryStage;
            stage.setTitle("FXML Login Sample");
            stage.setMinWidth(MINIMUM_WINDOW_WIDTH);
            stage.setMinHeight(MINIMUM_WINDOW_HEIGHT);
            gotoMainView();
            primaryStage.show();
            
        } catch (IOException | XmlRpcException ex) {
            log.error("unexpected exception: {}", ex.getMessage());
        }
    }

    public User getLoggedUser() {
        return loggedUser;
    }
        
    public boolean userLogging(String userId, String password){
        if (authenticator.validate(userId, password)) {
            loggedUser = User.of(userId);
            gotoProfile();
            return true;
        } else {
            return false;
        }
    }
    
    void userLogout(){
        loggedUser = null;
        gotoMainView();
    }
    
    private void gotoProfile() {
        try {
            //ProfileController profile = (ProfileController) replaceSceneContent("profile.fxml");
            //profile.setApp(this);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    private void gotoMainView() {
        try {
            MainViewController login = (MainViewController) replaceSceneContent("/META-INF/gui/mainView.fxml");
            login.setApp(this);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    private Initializable replaceSceneContent(String fxml) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        InputStream in = getClass().getResourceAsStream(fxml);
        log.info("fxml: {}, in: {}", fxml, in);
        loader.setLocation(Main.class.getResource(fxml));
        AnchorPane page;
        try {
            page = (AnchorPane) loader.load(in);
        } finally {
            in.close();
        } 
        Scene scene = new Scene(page, 800, 600);
        stage.setScene(scene);
        stage.sizeToScene();
        return (Initializable) loader.getController();
    }
}
