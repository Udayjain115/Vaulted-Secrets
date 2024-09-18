package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.SceneManager;
import nz.ac.auckland.se206.Timer;

public class RulebookController {

  @FXML private ImageView dustImage;
  @FXML private ImageView dustImage2;
  @FXML private Label timerLbl;

  private boolean isDustOff1 = false;
  private boolean isDustOff2 = false;

  private ImageCursor dustBrushCursor;

  @FXML
  public void initialize() {
    // Load the original cursor image
    Image originalCursorImage = new Image(getClass().getResourceAsStream("/images/brush.png"));

    // Create an ImageCursor with the scaled image
    dustBrushCursor =
        new ImageCursor(
            originalCursorImage, originalCursorImage.getWidth(), originalCursorImage.getHeight());

    // Set event handlers for dustImage
    dustImage.setOnMouseEntered(
        event -> {
          dustImage.setCursor(dustBrushCursor);
        });
    dustImage.setOnMouseExited(
        event -> {
          dustImage.setCursor(Cursor.DEFAULT);
        });

    // Set event handlers for dustImage2
    dustImage2.setOnMouseEntered(
        event -> {
          dustImage2.setCursor(dustBrushCursor);
        });
    dustImage2.setOnMouseExited(
        event -> {
          dustImage2.setCursor(Cursor.DEFAULT);
        });

        Timer timer = Timer.getTimer();
      StringBinding timeLayout = Bindings.createStringBinding(() -> {
        int time = timer.getTimeLeft().get();
        int mins = time/60;
        int secs = time % 60;
        return String.format("%1d:%02d", mins, secs);
      },timer.getTimeLeft());

      timerLbl.textProperty().bind(timeLayout);
      timer.start();
  }

  public void dustClickedOn() {

    if (!isDustOff1) {
      FadeTransition ft = new FadeTransition(Duration.millis(1000), dustImage);
      ft.setFromValue(1.0);
      ft.setToValue(0.0);
      ft.play();
      isDustOff1 = true;
    }
  }

  public void dustClickedOn2() {
    if (!isDustOff2) {
      FadeTransition ft2 = new FadeTransition(Duration.millis(1000), dustImage2);
      ft2.setFromValue(1.0);
      ft2.setToValue(0.0);
      ft2.play();
      isDustOff2 = true;
    }
  }

  /**
   * Handles the event when the close button (X) is clicked.
   *
   * @throws IOException
   */
  @FXML
  public void closeRulebook(MouseEvent event) throws IOException {
    Parent crimeSceneRoot = SceneManager.getUiRoot(SceneManager.AppUi.CRIME_SCENE);
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

  

    stage.getScene().setRoot(crimeSceneRoot);
    
  }
}
