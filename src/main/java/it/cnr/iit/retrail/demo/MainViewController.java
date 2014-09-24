/*
 */
package it.cnr.iit.retrail.demo;

import it.cnr.iit.retrail.commons.PepSession;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;

import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.slf4j.LoggerFactory;

/**
 * Main View Controller.
 */
public class MainViewController extends AnchorPane implements Initializable {

    @FXML
    BorderPane user1;
    @FXML
    Button goToDoorButton;
    @FXML
    Button enterRoomButton;
    @FXML
    Button leaveButton;
    @FXML
    Label errorMessage;

    private Main application;
    private String userId = "carniani";

    static final org.slf4j.Logger log = LoggerFactory.getLogger(MainViewController.class);

    public void setApp(Main application) {
        this.application = application;
    }

    private void addHandlers(BorderPane userView) {
        userView.getLeft().setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                try {
                    processUserLeaving(null);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
        });
        userView.getRight().setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                try {
                    User user = User.getInstance(userId);
                    if (user.getStatus() == PepSession.Status.TRY) {
                        processUserEnteringRoom(null);
                    } else {
                        processUserGoingToDoor(null);
                    }
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
            User user = User.getInstance(userId);
            updateUserView(user1, user);
            goToDoorButton.setDisable(false);
            enterRoomButton.setDisable(true);
            leaveButton.setDisable(true);
            addHandlers(user1);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    private void updateUserView(BorderPane userView, User user) {
        String name;
        int x;
        ImageView icon = (ImageView) userView.getCenter();
        ImageView leftArrow = (ImageView) userView.getLeft();
        ImageView rightArrow = (ImageView) userView.getRight();
        Label label = (Label) userView.getBottom();
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        switch (user.getStatus()) {
            default:
                name = "/META-INF/gui/userGray.png";
                x = 0;
                goToDoorButton.setDisable(false);
                enterRoomButton.setDisable(true);
                leaveButton.setDisable(true);
                leftArrow.setVisible(false);
                rightArrow.setVisible(true);
                break;
            case TRY:
                name = "/META-INF/gui/userBlue.png";
                x = 300;
                goToDoorButton.setDisable(true);
                enterRoomButton.setDisable(false);
                leaveButton.setDisable(false);
                leftArrow.setVisible(true);
                rightArrow.setVisible(true);
                break;
            case ONGOING:
                name = "/META-INF/gui/userGreen.png";
                x = 600;
                goToDoorButton.setDisable(true);
                enterRoomButton.setDisable(true);
                leaveButton.setDisable(false);
                leftArrow.setVisible(true);
                rightArrow.setVisible(false);

                break;
            case REVOKED:
                name = "/META-INF/gui/userRed.png";
                x = 600;
                goToDoorButton.setDisable(true);
                enterRoomButton.setDisable(true);
                leaveButton.setDisable(false);
                leftArrow.setVisible(true);
                rightArrow.setVisible(false);
                break;
        }
        InputStream is = getClass().getResourceAsStream(name);
        Image image = new Image(is);
        icon.setImage(image);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), user1);
        tt.setToX(x);
        tt.play();
        label.setText(user.getCustomId());
    }

    public void processUserGoingToDoor(ActionEvent event) throws Exception {
        User user = User.getInstance(userId);
        if (!user.goToDoor()) {
            showError("User " + userId + " is not allowed to stand at the door");
        } else {
            showMessage("User " + userId + " standing in front of the door");
            updateUserView(user1, user);
        }
    }

    public void processUserEnteringRoom(ActionEvent event) throws Exception {
        User user = User.getInstance(userId);
        if (!user.enterRoom()) {
            showError("User " + userId + " is not allowed to enter the room");
        } else {
            showMessage("User " + userId + " entered the room");
            updateUserView(user1, user);
        }
    }

    public void processUserLeaving(ActionEvent event) throws Exception {
        User user = User.getInstance(userId);
        if (!user.leave()) {
            showError("User " + userId + " is not allowed to leave");
        } else {
            showMessage("User " + userId + " gone away");
            updateUserView(user1, user);
        }
    }

    public void onUserMustLeaveRoom(String userId) throws Exception {
        User user = User.getInstance(userId);
        showError("User " + userId + " must leave room immediately!");
        updateUserView(user1, user);
    }

    private void showError(String error) {
        errorMessage.setTextFill(Color.RED);
        errorMessage.setText(error);
    }

    private void showMessage(String message) {
        errorMessage.setTextFill(Color.WHITE);
        errorMessage.setText(message);
    }
}
