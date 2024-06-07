package com.example.chat;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class LoginController {
    @FXML
    private TextField userName;
    @FXML
    private TextField passWord;
    @FXML
    private Button connect;

    @FXML
    public void initialize() {}

    public void login(ActionEvent event) {
        String username = userName.getText();
        String password = passWord.getText();

        // Mã hóa username và password
        String encodedUsername = Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8));
        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));

        try (Socket socket = new Socket("localhost", 12345);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.write(encodedUsername + "\n");
            writer.write(encodedPassword + "\n");
            writer.flush();

            // Đọc tín hiệu đăng nhập từ server
            String loginSignal = reader.readLine();

            // Kiểm tra tín hiệu đăng nhập từ server
            if ("success".equals(loginSignal)) {
                // Gửi yêu cầu đăng nhập thành công và chuyển giao diện
                Platform.runLater(() -> {
                    // Mở giao diện client và kết nối mới đến server từ phía client
                    openClientInterface(username, event);
                });
            } else {
                System.out.println("Login failed. Please try again."); // Xử lý đăng nhập thất bại
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openClientInterface(String username, ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/example/chat/client.fxml"));
            Parent client = loader.load();
            Scene scene = new Scene(client);
            ClientController controller = loader.getController();
            controller.setUsername(username);
            controller.connectToServer(username);
            stage.setTitle(username);
            stage.setResizable(false);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
