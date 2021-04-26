package io.github.jgcodes.bitfs0x.querier;

import io.github.jgcodes.bitfs0x.querier.util.Resources;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ResourceBundle;

public class Launcher extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader loader = new FXMLLoader(Launcher.class.getResource("/assets/fxml/main.fxml"));

    stage.setScene(loader.load());
    Controller c = loader.getController();

    stage.setTitle("BitFSQL Querier");

    stage.getIcons().addAll(
      new Image(Resources.get(Launcher.class, "/assets/icons/icon.png").toExternalForm()),
      new Image(Resources.get(Launcher.class, "/assets/icons/icon-64.png").toExternalForm()),
      new Image(Resources.get(Launcher.class, "/assets/icons/icon-32.png").toExternalForm()),
      new Image(Resources.get(Launcher.class, "/assets/icons/icon-16.png").toExternalForm())
    );
    stage.show();
  }
}
