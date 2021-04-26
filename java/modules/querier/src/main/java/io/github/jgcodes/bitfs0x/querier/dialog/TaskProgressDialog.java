package io.github.jgcodes.bitfs0x.querier.dialog;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;

/**
 * A progress dialog bound to a task.
 */
public class TaskProgressDialog extends Dialog<Boolean> {
  private final ReadOnlyObjectWrapper<Task<?>> task;

  public TaskProgressDialog(Task<?> task) {
    super();

    this.task = new ReadOnlyObjectWrapper<>(task);

    this.initModality(Modality.APPLICATION_MODAL);

    DialogPane pane = this.getDialogPane(); {
      pane.getButtonTypes().setAll(ButtonType.CANCEL);
      pane.getStyleClass().addAll("alert", "information");

      DialogHeaderPane headerPane = new DialogHeaderPane();
      {
        headerPane.headerTextProperty().bind(task.titleProperty());
        headerPane.graphicProperty().bind(TaskProgressDialog.this.graphicProperty());
      }
      pane.setHeader(headerPane);

      ProgressBar progress = new ProgressBar();
      progress.progressProperty().bind(task.progressProperty());
      progress.setMaxWidth(Double.MAX_VALUE);
      progress.setPrefWidth(360);

      pane.setContent(progress);

      StackPane expandContainer = new StackPane();
      {
        Label expandLabel = new Label();
        expandLabel.textProperty().bind(task.messageProperty());
        expandLabel.setWrapText(true);
        expandLabel.setMaxWidth(Double.MAX_VALUE);
        expandLabel.setPrefWidth(360);
        StackPane.setAlignment(expandLabel, Pos.TOP_LEFT);

        expandContainer.getChildren().add(expandLabel);
      }
      expandContainer.setMaxWidth(Double.MAX_VALUE);
      expandContainer.setPrefWidth(360);

      pane.setExpandableContent(expandContainer);
    }

    //prevent closing dialog until we're done
    pane.getScene().getWindow().setOnCloseRequest(event -> {
      if (!task.isDone()) {
        event.consume();
      }
    });

    ((Button) pane.lookupButton(ButtonType.CANCEL)).setOnAction(event -> {
      task.setOnCancelled(event1 -> this.close());
      task.cancel();
      event.consume();
    });
    //change "Cancel" to "OK" once task completes
    task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, this::onTaskFinish);
    task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, this::onTaskFinish);
  }

  void onTaskFinish(WorkerStateEvent event) {
    DialogPane pane = this.getDialogPane();
    pane.getButtonTypes().setAll(ButtonType.OK);
  }

  public Task<?> getTask() {
    return task.get();
  }

  public ReadOnlyObjectProperty<Task<?>> taskProperty() {
    return task.getReadOnlyProperty();
  }
}
