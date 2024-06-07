module com.example.chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.xml.bind;
    requires java.sql;
//    requires java.xml.bind;


    opens com.example.chat to javafx.fxml,java.xml.bind;
    exports com.example.chat;
}