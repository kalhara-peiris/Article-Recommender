module org.example.javaoop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires okhttp3;
    requires org.json;
    requires jsoup;
    requires stanford.corenlp;
    requires org.apache.opennlp.tools;


    opens org.example.javaoop to javafx.fxml;
    exports org.example.javaoop;
}