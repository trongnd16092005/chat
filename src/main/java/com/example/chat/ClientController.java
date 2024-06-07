package com.example.chat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClientController {
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private Button button;

    private Socket clientSocket;
    private BufferedWriter writer;
    private List<Message> messageHistory = new ArrayList<>();
    private final String chatHistoryFile = "chat.xml";
    private String username;

    @FXML
    public void initialize() {
        loadMessageHistory();
        button.setOnAction(event -> sendMessage());
    }

    public void connectToServer(String username) {
        try {
            String serverIP = "localhost";
            int serverPort = 12345;

            clientSocket = new Socket(serverIP, serverPort);
            if (clientSocket != null && !clientSocket.isClosed()) {
                writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                writer.write(username + "\n");
                writer.flush();

                new Thread(this::listenForMessages).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenForMessages() {
        while (true) {  // Thêm vòng lặp vô hạn để duy trì kết nối
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String message;
                while ((message = reader.readLine()) != null) {
                    String finalMessage = message;
                    Platform.runLater(() -> {
                        textArea.appendText("Manager: " + finalMessage + "\n");
                        Message msg = new Message("Manager", username, finalMessage, LocalDateTime.now());
                        messageHistory.add(msg);
                        saveMessageToXML();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                reconnectToServer();  // Thử tái kết nối khi gặp lỗi
            }
        }
    }
    private void reconnectToServer() {
        while (true) {
            try {
                Thread.sleep(5000);  // Chờ 5 giây trước khi thử lại
                connectToServer(username);
                break;  // Thoát vòng lặp nếu kết nối thành công
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void sendMessage() {
        try {
            String message = textField.getText();
            if (message.trim().isEmpty()) return;
            writer.write(message + "\n");
            writer.flush();
            textArea.appendText(username + ": " + message + "\n");
            Message msg = new Message(username, "Manager", message, LocalDateTime.now());
            messageHistory.add(msg);
            saveMessageToXML();
            textField.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMessageHistory() {
        try {
            File file = new File(chatHistoryFile);
            if (file.exists()) {
                JAXBContext jaxbContext = JAXBContext.newInstance(ChatHistory.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                ChatHistory chatHistory = (ChatHistory) jaxbUnmarshaller.unmarshal(file);
                messageHistory = chatHistory.getMessages();
                displayMessageHistory(); // Display message history after loading
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private void saveMessageToXML() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ChatHistory.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setMessages(messageHistory);

            marshaller.marshal(chatHistory, new FileWriter(chatHistoryFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayMessageHistory() {
        for (Message msg : messageHistory) {
            textArea.appendText(msg.getSender() + ": " + msg.getContent() + "\n");
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
