package component;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import java.awt.Desktop;
import java.util.*;
import java.io.*;

public class Controller {

    @FXML
    private MenuItem closeMenuItem;

    @FXML
    private MenuBar menuBar;

    @FXML
    private MenuItem addFilesMenuItem;

    @FXML
    private MenuItem clearMenuItem;

    @FXML
    private MenuItem removeMenuItem;

    @FXML
    private TableView<Document> documentTable;

    @FXML
    private TableColumn<Document, String> documentNameColumn;

    @FXML
    private TableColumn<Document, String> documentPathColumn;

    @FXML
    private TextField inputTextField;

    @FXML
    private Text promptText;

    @FXML
    private Button clearButton;

    @FXML
    private Stage stage;

    private static final String READMEPATH = "README.txt";

    private static final String SAVEPATH = "save";

    private QueryModel queryModel = new QueryModel(SAVEPATH);

    private ObservableList<Document> tableItems;

    @FXML
    private void initialize() {
        inputTextField.textProperty().addListener((ObservableValue<? extends String> observable,
                                                   String oldValue, String newValue) -> {queryAndRefresh();}
        );
        tableItems = FXCollections.observableArrayList();
        queryAndRefresh();
        documentTable.setItems(tableItems);
        documentNameColumn.setCellValueFactory(new PropertyValueFactory("documentName"));
        documentPathColumn.setCellValueFactory(new PropertyValueFactory("documentPath"));
    }

    @FXML
    private void handleCloseMenuItem(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    private void handleAddFilesMenuItem(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Files to Add");
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
        if (selectedFiles != null) {
            for (File file: selectedFiles) {
                Document document = new Document(file);
                queryModel.addDocument(document);
            }
            queryAndRefresh();
            queryModel.saveToFile(SAVEPATH);
        }
    }

    @FXML
    private void handleAddPathMenuItem(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a Folder to Add");
        File selectedPath = directoryChooser.showDialog(stage);
        if (selectedPath != null) {
            File[] selectedFiles = selectedPath.listFiles();
            if (selectedFiles.length>0) {
                for (File file: selectedFiles) {
                    Document document = new Document(file);
                    queryModel.addDocument(document);
                }
                queryAndRefresh();
                queryModel.saveToFile(SAVEPATH);
            }
        }
    }

    @FXML
    private void handleTableClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            ObservableList documents = documentTable.getSelectionModel().getSelectedItems();
            if (documents!=null&&documents.size()==1) {
                Document document = (Document) documents.get(0);
                try {
                    ProcessBuilder pb = new ProcessBuilder("Notepad.exe", document.getDocumentPath());
                    pb.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @FXML
    private void handleClearButton(ActionEvent event) {
        inputTextField.clear();
    }

    @FXML
    private void handleClearMenuItem(ActionEvent event) {
        queryModel.removeAllDocuments();
        queryAndRefresh();
        queryModel.saveToFile(SAVEPATH);
    }

    @FXML
    private void handleRebuildIndexMenuItem(ActionEvent event) {
        queryModel.rebuildIndex();
        queryAndRefresh();
    }

    @FXML
    private void handleRemoveMenuItem(ActionEvent event) {
        ObservableList documents = documentTable.getSelectionModel().getSelectedItems();
        if (documents!=null&&documents.size()>0) {
            documents.forEach(doc->queryModel.removeDocument((Document) doc));
            queryAndRefresh();
            queryModel.saveToFile(SAVEPATH);
        }
    }

    @FXML
    private void handleReadMeMenuItem(ActionEvent event) {
        try {
            Desktop.getDesktop().open(new File(READMEPATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void queryAndRefresh() {
        String inputText = inputTextField.getText();
        if (inputText==null|inputText.isEmpty()) {
            tableItems.setAll(queryModel.getDocuments());
            promptText.setText("Please Input a Query");
            promptText.setFill(Color.BLUE);
            promptText.setVisible(true);
        } else if (QueryModel.isQuery(inputText)) {
            tableItems.setAll(queryModel.query(inputText));
            promptText.setText("The Query is Executed");
            promptText.setFill(Color.GREEN);
            promptText.setVisible(true);
        } else {
            tableItems.setAll();
            promptText.setText("The Query is Invalid");
            promptText.setFill(Color.RED);
            promptText.setVisible(true);
        }
    }
}