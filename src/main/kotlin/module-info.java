module edu.cwru.raycaster {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;


    opens edu.cwru.raycaster to javafx.fxml;
    exports edu.cwru.raycaster;
}