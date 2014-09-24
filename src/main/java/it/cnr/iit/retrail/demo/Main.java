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


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(Main.class, (java.lang.String[])null);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Firing GUI...");
        stage = primaryStage;
        stage.setTitle("reTRAIL demonstrator");
        stage.setMinWidth(MINIMUM_WINDOW_WIDTH);
        stage.setMinHeight(MINIMUM_WINDOW_HEIGHT);
        gotoMainView();
        primaryStage.show();
    }

    public User getLoggedUser() {
        return loggedUser;
    }
        
    public boolean userGoingToDoor(String userId) throws Exception {
        User user = User.getInstance(userId);
        if (user.goToDoor()) {
            gotoProfile();
            return true;
        } else {
            return false;
        }
    }
    
    public boolean userEnteringRoom(String userId) throws Exception {
        User user = User.getInstance(userId);
        if (user.enterRoom()) {
            gotoProfile();
            return true;
        } else {
            return false;
        }
    }
    
    public boolean userLeaving(String userId) throws Exception {
        User user = User.getInstance(userId);
        if (user.leave()) {
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
            UsageController.getInstance().setMain(login);
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
