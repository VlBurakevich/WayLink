module com.solution.waylink {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.solution.waylink to javafx.fxml;
    exports com.solution.waylink;
}