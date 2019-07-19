package component;

import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class Document{

    private StringProperty documentName;
    private StringProperty documentPath;

    public Document(String path) {
        this.setDocumentName(Paths.get(path).getFileName().toString());
        this.setDocumentPath(path);
    }

    public Document(File file) {
        this.setDocumentName(file.getName());
        try {
            this.setDocumentPath(file.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final void setDocumentName(String value) {
        documentNameProperty().set(value);
    }
    public final String getDocumentName() {
        return documentNameProperty().get();
    }
    public StringProperty documentNameProperty() {
        if (documentName == null) documentName = new SimpleStringProperty(this, "documentName");
        return documentName;
    }
    public final void setDocumentPath(String value) {
        documentPathProperty().set(value);
    }
    public final String getDocumentPath() {
        return documentPathProperty().get();
    }
    public StringProperty documentPathProperty() {
        if (documentPath == null) documentPath = new SimpleStringProperty(this, "documentPath");
        return documentPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return this.getDocumentName().equals(document.getDocumentName()) &&
                this.getDocumentPath().equals(document.getDocumentPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getDocumentName(), this.getDocumentPath());
    }
}