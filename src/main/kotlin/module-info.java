module edu.cwru.raycaster {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires java.desktop;
    requires kotlinx.coroutines.core;


    opens edu.cwru.raycaster to javafx.fxml;
    exports edu.cwru.raycaster;
}