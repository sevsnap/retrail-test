/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.demo;

import it.cnr.iit.retrail.client.impl.Replay;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.impl.PepSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;

import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.slf4j.LoggerFactory;

/**
 * Main View Controller.
 */
public class MainViewController extends AnchorPane implements Initializable {

    @FXML
    BorderPane user1;
    @FXML
    BorderPane user2;
    @FXML
    BorderPane user3;
    @FXML
    Label errorMessage;
    @FXML
    MenuButton policyButton;
    @FXML
    ToggleButton recButton;
    @FXML
    ToggleButton playButton;

    static final org.slf4j.Logger log = LoggerFactory.getLogger(MainViewController.class);
   
    private Replay replay = new Replay();
    
    private void addListeners(final BorderPane userView) throws Exception {
        final String userId = userView.getId();
        final User user = User.getInstance(userId);
        updateUserView(userView);
        userView.getLeft().setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                try {
                    Status prevStatus = user.getStatus();
                    log.info("User: {}", user);
                    if (!user.leave()) {
                        showError("User " + userId + " is not allowed to enter the room");
                    } else if (prevStatus == Status.REVOKED || prevStatus == Status.ONGOING) {
                        showMessage("User " + userId + " jas left the room");
                        playSound("/META-INF/gui/doorShut.wav");
                        playSound("/META-INF/gui/footsteps.wav");
                    } else {
                        showMessage("User " + userId + " has left the entrance door");
                        playSound("/META-INF/gui/footsteps.wav");
                    }
                    updateUserView(userView);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
        });
        userView.getRight().setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                try {
                    if (user.getStatus() == Status.TRY) {
                        if (!user.enterRoom()) {
                            showError("User " + userId + " is not allowed to enter the room!");
                        } else {
                            showMessage("User " + userId + " entered the room");
                            playSound("/META-INF/gui/doorShut.wav");
                        }
                    } else if (!user.goToDoor()) {
                        showError("User " + userId + " is not allowed to stand at the door");
                    } else {
                        showMessage("User " + userId + " standing in front of the door");
                        playSound("/META-INF/gui/footsteps.wav");
                    }
                    updateUserView(userView);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            showMessage("Welcome!");
            addListeners(user1);
            addListeners(user2);
            addListeners(user3);
            recButton.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    try {
                        if (recButton.isSelected()) {
                            FileChooser fileChooser = new FileChooser();
                            fileChooser.setTitle("Save Retrail File");
                            fileChooser.getExtensionFilters().addAll(
                                    new FileChooser.ExtensionFilter("XML Files", "*.xml"),
                                    new FileChooser.ExtensionFilter("All Files", "*.*"));
                            Stage mainStage = (Stage) playButton.getScene().getWindow();
                            File selectedFile = fileChooser.showSaveDialog(mainStage);
                            if(selectedFile != null) {
                                UsageController.getInstance().client.startRecording(selectedFile);
                                recButton.setText("OFF");
                                playButton.setDisable(true);
                            } else recButton.setSelected(false);
                        } else {
                            UsageController.getInstance().client.stopRecording();
                            recButton.setText("REC");
                            playButton.setDisable(false);
                        }
                    } catch (Exception ex) {
                        log.error("while handling recButton event: {}", ex);
                    }
                }
            });
            playButton.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    if (playButton.isSelected()) {
                        try {
                            FileChooser fileChooser = new FileChooser();
                            fileChooser.setTitle("Open etrail File");
                            fileChooser.getExtensionFilters().addAll(
                                    new FileChooser.ExtensionFilter("XML Files", "*.xml"),
                                    new FileChooser.ExtensionFilter("All Files", "*.*"));
                            Stage mainStage = (Stage) playButton.getScene().getWindow();
                            File selectedFile = fileChooser.showOpenDialog(mainStage);
                            if(selectedFile != null) {
                                playButton.setText("STOP");
                                recButton.setDisable(true);
                                replay.play(selectedFile, UsageController.getInstance());
                            } else playButton.setSelected(false);
                        } catch (Exception ex) {
                            log.error("while replaying: {}", ex);
                        }
                    } else {
                        playButton.setText("PLAY");
                        recButton.setDisable(false);
                        try {
                            replay.stop();
                        } catch (Exception ex) {
                            log.error("while stopping: {}", ex);
                        }
                    }
                }
            });
            policyButton.setText(
                    "One at a time");
            policyButton.setTextFill(Paint.valueOf("black"));
            MenuItem policy1;
            policy1 = new MenuItem("One at a time");

            policy1.setOnAction(
                    new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event
                        ) {
                            try {
                                showMessage("Policy changed: only one person at a time is now allowed");
                                policyButton.setText("One at a time");
                                UsageController.changePoliciesTo(
                                        "/META-INF/policies1/pre1.xml",
                                        "/META-INF/policies1/on1.xml",
                                        "/META-INF/policies1/post1.xml",
                                        "/META-INF/policies1/trystart1.xml",
                                        "/META-INF/policies1/tryend1.xml"
                                );
                            } catch (Exception ex) {
                                log.error(ex.getMessage());
                            }
                        }
                    }
            );
            MenuItem policy2;
            policy2 = new MenuItem("Two people");

            policy2.setOnAction(
                    new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event
                        ) {
                            try {
                                showMessage("Policy changed: only 2 people at a time are now allowed");
                                policyButton.setText("Two people");
                                UsageController.changePoliciesTo(
                                        "/META-INF/policies2/pre2.xml",
                                        "/META-INF/policies2/on2.xml",
                                        "/META-INF/policies2/post2.xml",
                                        "/META-INF/policies2/trystart2.xml",
                                        "/META-INF/policies2/tryend2.xml"
                                );
                            } catch (Exception ex) {
                                log.error(ex.getMessage());
                            }
                        }
                    }
            );
            MenuItem policy3;
            policy3 = new MenuItem("One cam inside");

            policy3.setOnAction(
                    new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event
                        ) {
                            try {
                                showMessage("Policy changed: only one cam inside room");
                                policyButton.setText("One cam inside");
                                UsageController.changePoliciesTo(
                                        "/META-INF/policies3/pre3.xml",
                                        "/META-INF/policies3/on3.xml",
                                        "/META-INF/policies3/post3.xml",
                                        "/META-INF/policies3/trystart3.xml",
                                        "/META-INF/policies3/tryend3.xml"
                                );
                            } catch (Exception ex) {
                                log.error(ex.getMessage());
                            }
                        }
                    }
            );
            policyButton.getItems()
                    .setAll(policy1, policy2, policy3);

        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    public static void playSound(final String name) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Clip clip = AudioSystem.getClip();
                    clip.addLineListener(new LineListener() {
                        @Override
                        public void update(LineEvent event) {
                            if (event.getType() == LineEvent.Type.STOP) {
                                if (event.getLine() instanceof Clip) {
                                    log.debug("end of {} playback", name);
                                    event.getLine().close();
                                }
                            }
                        }
                    });
                    try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(
                            Main.class.getResourceAsStream(name))) {
                        clip.open(inputStream);
                        log.debug("playing {}", name);
                        clip.start();
                    }
                } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
                    log.error(e.getMessage());
                }
            }
        }).start();
    }

    private void setUserViewText(BorderPane userView, String text) {
        Label label = (Label) userView.getBottom();
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.setText(text);
    }

    private void updateUserView(BorderPane userView) throws Exception {
        String name;
        int x;
        User user = User.getInstance(userView.getId());
        log.info("updating {}, view {}={}", user, userView.getId(), userView);
        ImageView icon = (ImageView) userView.getCenter();
        ImageView leftArrow = (ImageView) userView.getLeft();
        ImageView rightArrow = (ImageView) userView.getRight();
        switch (user.getStatus()) {
            default:
                name = "/META-INF/gui/userGray.png";
                x = 0;
                leftArrow.setVisible(false);
                rightArrow.setVisible(true);
                break;
            case TRY:
                name = "/META-INF/gui/userBlue.png";
                x = 250;
                leftArrow.setVisible(true);
                rightArrow.setVisible(true);
                break;
            case ONGOING:
                name = "/META-INF/gui/userGreen.png";
                x = 520;
                leftArrow.setVisible(true);
                rightArrow.setVisible(false);
                break;
            case REVOKED:
                name = "/META-INF/gui/userRed.png";
                x = 450;
                leftArrow.setVisible(true);
                rightArrow.setVisible(false);
                break;
        }
        Image image;
        try (InputStream is = getClass().getResourceAsStream(name)) {
            image = new Image(is);
        }
        icon.setImage(image);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), userView);
        tt.setToX(x);
        tt.play();
    }

    private BorderPane findUserView(String userId) {
        if (user1.getId().equals(userId)) {
            return user1;
        } else if (user2.getId().equals(userId)) {
            return user2;
        }
        return user3;
    }

    public void onRevoke(final PepSession session) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                String userId = session.getCustomId();
                showError("Access for user " + userId + " revoked!");
                try {
                    updateUserView(findUserView(userId));
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
        });
    }

    private void showError(String error) {
        errorMessage.setTextFill(Color.RED);
        errorMessage.setText(error);
    }

    private void showMessage(String message) {
        errorMessage.setTextFill(Color.WHITE);
        errorMessage.setText(message);
    }

    public void onObligation(PepSession session, final String obligation) throws Exception {
        log.info("*** Obligation {} received!", obligation);
        final String userId = session.getCustomId();
        final String obscuredId = session.getUuid();
        final BorderPane userView = findUserView(userId);
        final User user = User.getInstance(userId);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                switch (obligation) {
                    case "sayWelcome": 
                        playSound("/META-INF/gui/tryOk.wav");

                        break;
                    case "sayStandOff":
                        playSound("/META-INF/gui/tryFail.wav");
                        break;
                    case "sayDetected":
                        playSound("/META-INF/gui/entrance.wav");
                        break;
                    case "sayDenied":
                        playSound("/META-INF/gui/denied.wav");
                        break;
                    case "sayRevoked":
                        playSound("/META-INF/gui/revoked.wav");
                        break;
                    case "showUser":
                        setUserViewText(userView, userId);
                        break;
                    case "hideUser":
                        setUserViewText(userView, obscuredId);
                        break;
                    case "startAccess":
                        user.enterRoom();
                         {
                            try {
                                updateUserView(findUserView(userId));
                            } catch (Exception ex) {
                                log.error("while executing obligation: {}", ex);
                            }
                        }
                        showMessage("user entered room");
                        break;
                    default:
                        log.error("Unknown obligation: {} -- ignoring", obligation);
                        break;
                }
            }
        });
    }
}
