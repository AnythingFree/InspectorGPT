module InspectorGPT {
	requires javafx.graphics;
	requires javafx.controls;
	
    opens test to javafx.fxml; // Open the 'test' package for FXML loading
    opens chat to javafx.fxml; // Open the 'test' package for FXML loading

    exports test;
    exports chat;
}