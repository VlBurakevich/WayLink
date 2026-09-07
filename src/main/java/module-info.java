module com.solution.waylink {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires static lombok;
    requires com.google.gson;
    requires java.logging;
    requires java.net.http;
    requires webrtc.java;
    requires com.sun.jna.platform;
    requires com.sun.jna;


    opens com.solution.waylink to javafx.fxml;
    exports com.solution.waylink;
}