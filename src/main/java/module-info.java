module org.example.javaoop {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.javaoop to javafx.fxml;
    exports org.example.javaoop;
}