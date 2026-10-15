package com.datecourse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;
public class login {
    public login(){

    }
    @FXML
    private Button button;
    @FXML
    private Label wrongLogin;
    @FXML
    private TextField id;
    @FXML
    private PasswordField password;

    public void userLogin(ActionEvent event) throws IOException{
        checkLogin();
    }
    private void checkLogin() throws IOException{
        MainApp m= new MainApp();
        if(id.getText().toString().equals("java") && password.getText().toString().equals("2026")){
            m.changeScene("/fxml/Main.fxml");
        }
        else if(id.getText().isEmpty() && password.getText().isEmpty()) {
            wrongLogin.setText("정보를 입력해주세요.");
        }
        else{
            wrongLogin.setText("잚못된 입력 정보입니다.");
        }
    }
}
