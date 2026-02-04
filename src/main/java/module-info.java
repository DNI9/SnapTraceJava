module com.snaptrace {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    
    requires com.github.kwhat.jnativehook;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.apache.poi.ooxml;
    requires org.slf4j;
    
    requires java.desktop;
    
    opens com.snaptrace to javafx.fxml;
    opens com.snaptrace.controller to javafx.fxml;
    opens com.snaptrace.model to com.fasterxml.jackson.databind;
    
    exports com.snaptrace;
    exports com.snaptrace.controller;
    exports com.snaptrace.model;
    exports com.snaptrace.service;
    exports com.snaptrace.config;
}
