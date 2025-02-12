package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.SceneManager;
import nz.ac.auckland.se206.Timer;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the suspect room view. Handles user interactions within the suspect room
 * where the user can chat with the suspect and guess their profession.
 */
public class SuspectRoomController {

  @FXML private TextArea text;
  @FXML private TextField textInput;
  @FXML private Button goToJanitor;
  @FXML private VBox menuBox; // Root layout of the scene
  @FXML private Button btnSend;
  @FXML private Button dropdownButton1;
  @FXML private Button dropdownButton2;
  @FXML private Button dropdownButton3;
  @FXML private Label timerLbl;
  @FXML private javafx.scene.image.ImageView animationImage;

  // Declare the ImageView for the pen-writing animation
  private TranslateTransition animation;
  private ParallelTransition parallelTransition;

  private String profession;
  private ChatCompletionRequest chatCompletionRequest;
  private String currentPersonTalking;
  private final List<String> chatMessages =
      Collections.synchronizedList(new CopyOnWriteArrayList<>());

  private boolean isMenuVisible = false; // Tracks menu visibility
  private CrimeSceneController crimeSceneController =
      (CrimeSceneController) SceneManager.getController(SceneManager.AppUi.CRIME_SCENE);

  @FXML
  private void initialize() {
    textInput.setDisable(true);
    btnSend.setDisable(true);

    toggleMenuDropdown(dropdownButton1);
    toggleMenuDropdown(dropdownButton2);
    toggleMenuDropdown(dropdownButton3);

    // Initialize the pen-writing animation
    InputStream animationImageStream = getClass().getResourceAsStream("/images/pen.png");
    animationImage.setImage(new Image(animationImageStream));
    animation = new TranslateTransition(Duration.seconds(2), animationImage);
    animation.setFromX(50);
    animation.setToX(200);
    animation.setCycleCount(TranslateTransition.INDEFINITE);
    animation.setAutoReverse(true);
    animationImage.setVisible(false); // Initially hide the animation image

    RotateTransition rotateAnimation = new RotateTransition(Duration.seconds(0.5), animationImage);
    rotateAnimation.setFromAngle(0);
    rotateAnimation.setToAngle(15);
    rotateAnimation.setCycleCount(TranslateTransition.INDEFINITE);
    rotateAnimation.setAutoReverse(true);

    parallelTransition = new ParallelTransition(animation, rotateAnimation);

    textInput.setOnKeyPressed(
        event -> {
          switch (event.getCode()) {
            case ENTER:
              try {
                onSendMessage(null); // Trigger the send message method when Enter is pressed
                textInput.clear();
              } catch (ApiProxyException | IOException e) {
                e.printStackTrace();
              }
              break;
            default:
              break;
          }
        });

    // Get the timer instance
    Timer timer = Timer.getTimer();

    // Create a string binding that updates the time left every second
    StringBinding timeLayout =
        Bindings.createStringBinding(
            () -> {
              int time = timer.getTimeLeft().get();
              int mins = time / 60;
              int secs = time % 60;
              return String.format("%1d:%02d", mins, secs);
            },
            timer.getTimeLeft());

    // Bind the timer label to the time layout
    timerLbl.textProperty().bind(timeLayout);
    timer.start();
  }

  /**
   * Toggles the menu dropdown when the mouse enters or exits the menu button.
   *
   * @param button the button to toggle the dropdown for
   */
  @FXML
  private void toggleMenuDropdown(Button button) {
    // Set the initial style of the button when the mouse enters
    button.setOnMouseEntered(
        event -> {
          // Make it darker when entered
          button.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7)");
        });
    // Set the style of the button when the mouse exits
    button.setOnMouseExited(
        event -> {
          // Make it lighter when exited
          button.setStyle("-fx-background-color: rgba(151, 151, 151, 0.7)");
        });
  }

  /** Sets the visited room to true when the user sends a message to the bank manager. */
  @FXML
  private void managerSetTrue() {
    String message = textInput.getText().trim();
    if (message != null && !message.isEmpty()) {
      crimeSceneController.addVisitedRoom("bankManager");
    }
    textInput.clear();
  }

  /** Sets the visited room to true when the user sends a message to the janitor. */
  @FXML
  private void janitorSetTrue() {
    String message = textInput.getText().trim();
    if (message != null && !message.isEmpty()) {
      crimeSceneController.addVisitedRoom("janitor");
    }

    textInput.clear();
  }

  /** Sets the visited room to true when the user sends a message to the policeman. */
  @FXML
  private void policemanSetTrue() {
    String message = textInput.getText().trim();
    if (message != null && !message.isEmpty()) {
      crimeSceneController.addVisitedRoom("policeman");
    }

    textInput.clear();
  }

  /** Toggles the visibility of the drop-down menu. */
  @FXML
  // Function to toggle the visibility of the drop-down menu
  private void onClickToggleMenu() {
    isMenuVisible = !isMenuVisible;
    menuBox.setVisible(isMenuVisible);
  }

  /**
   * Switches to the policeman room and opens the chat with the policeman.
   *
   * @throws IOException if the FXML file is not found
   */
  @FXML
  private void onClickCopMenu() throws IOException {
    // Now switch rooms
    App.setRoot("copRoom");
    // Open the chat with the policeman
    App.openChat(null, "policeman");
  }

  /**
   * Switches to the janitor room and opens the chat with the janitor.
   *
   * @throws IOException if the FXML file is not found
   */
  @FXML
  private void onClickJanitorMenu() throws IOException {
    // Now switch rooms
    App.setRoot("janitorRoom");
    // Open the chat with the janitor
    App.openChat(null, "janitor");
  }

  /**
   * Switches to the bank manager room and opens the chat with the bank manager.
   *
   * @throws IOException if the FXML file is not found
   */
  @FXML
  private void onClickBankManagerMenu() throws IOException {
    // Now switch rooms
    App.setRoot("bankManagerRoom");
    // Open the chat with the bank manager
    App.openChat(null, "bankManager");
  }

  /**
   * Switches to the crime scene room.
   *
   * @param event the action event triggered by the menu button
   * @throws IOException if the FXML file is not found
   */
  @FXML
  private void onClickCrimeSceneMenu(ActionEvent event) throws IOException {
    Parent crimeSceneRoot = SceneManager.getUiRoot(SceneManager.AppUi.CRIME_SCENE);
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

    stage.getScene().setRoot(crimeSceneRoot);
  }

  /**
   * Generates the system prompt based on the profession.
   *
   * @return the system prompt string
   */
  private String getSystemPrompt() {
    Map<String, String> map = new HashMap<>();
    map.put("profession", profession);
    String fileName = String.format("%s.txt", profession);
    return PromptEngineering.getPrompt(fileName, map);
  }

  /**
   * Sets the profession for the chat context and initializes the ChatCompletionRequest.
   *
   * @param profession the profession to set
   */
  public void setProfession(String profession) {
    this.profession = profession;
    // Initialize the ChatCompletionRequest
    try {
      ApiProxyConfig config = ApiProxyConfig.readConfig();
      // Set the parameters for the ChatCompletionRequest
      chatCompletionRequest =
          new ChatCompletionRequest(config)
              .setN(1)
              .setTemperature(0.2)
              .setTopP(0.5)
              .setMaxTokens(100);
      // Send the initial system prompt
      runGpt(new ChatMessage("system", getSystemPrompt()));
    } catch (ApiProxyException e) {
      e.printStackTrace();
    }
  }

  /**
   * Appends a chat message to the chat text area.
   *
   * @param msg the chat message to append
   */
  private void onAppendChatMessage(ChatMessage msg) {
    synchronized (chatMessages) {
      // Add the message to the chatMessages list
      chatMessages.add(msg.getContent());
      Platform.runLater(
          () -> {
            // Clear the text area if the role changes
            if (!msg.getRole().equals(currentPersonTalking)) {
              text.clear();
            }

            // Append the message to the text area
            if (msg.getRole().equals("assistant")) {
              text.appendText(msg.getContent() + "\n\n");
            } else if (msg.getRole().equals("user")) {
              text.appendText("You: " + msg.getContent() + "\n\n");
            }
            // Update the currentPersonTalking
            currentPersonTalking = profession;
          });
    }
  }

  /** Shows the pen-writing animation when GPT is thinking. */
  private void showThinkingMessage() {
    Platform.runLater(
        () -> {
          animationImage.setVisible(true); // Show the pen-writing animation
          parallelTransition.play(); // Start the animation
        });
  }

  /** Clears the pen-writing animation when GPT is done thinking. */
  private void clearThinkingMessage() {
    Platform.runLater(
        () -> {
          parallelTransition.stop(); // Stop the animation
          animationImage.setVisible(false); // Hide the pen-writing animation
        });
  }

  /**
   * Runs the GPT model to generate a response to the user message.
   *
   * @param msg the user message to send to the GPT model
   */
  private void runGpt(ChatMessage msg) {
    Thread thread =
        new Thread(
            () -> {
              try {
                System.out.println(profession);

                showThinkingMessage();
                chatCompletionRequest.addMessage(msg);
                ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
                Choice result = chatCompletionResult.getChoices().iterator().next();
                chatCompletionRequest.addMessage(result.getChatMessage());
                clearThinkingMessage();
                onAppendChatMessage(result.getChatMessage());
                textInput.setDisable(false);
                btnSend.setDisable(false);
                // this way the people will not speak out loud
                // TextToSpeech.speak(result.getChatMessage().getContent());
              } catch (ApiProxyException e) {
                e.printStackTrace();
              }
            });

    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Sends a message to the GPT model.
   *
   * @param event the action event triggered by the send button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onSendMessage(ActionEvent event) throws ApiProxyException, IOException {
    textInput.setDisable(true);
    btnSend.setDisable(true);

    String message = textInput.getText().trim();
    if (message.isEmpty()) {
      textInput.setDisable(false);
      btnSend.setDisable(false);
      return;
    }
    if (profession.equals("policeman")) {
      crimeSceneController.addVisitedRoom("policeman");

    } else if (profession.equals("bankManager")) {
      crimeSceneController.addVisitedRoom("bankManager");

    } else if (profession.equals("janitor")) {
      crimeSceneController.addVisitedRoom("janitor");
    }
    // textInput.clear();
    ChatMessage msg = new ChatMessage("user", message);
    onAppendChatMessage(msg);
    runGpt(msg);
  }
}
