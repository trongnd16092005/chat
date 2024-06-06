package com.example.chat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManagerController {

    @FXML
    private TabPane tabPane;
    @FXML
    private TextField chatText;
    @FXML
    private Button sendButton;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private Map<String, BufferedWriter> clientWriters = new HashMap<>();
    private Map<String, TextArea> clientTextAreas = new HashMap<>();
    private List<Message> messageHistory = new ArrayList<>();
    private final String chatHistoryFile = "chat.xml";

    @FXML
    public void initialize() {
        loadMessageHistory();
        sendButton.setDisable(true);
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            sendButton.setDisable(newTab == null);
        });

        sendButton.setOnAction(event -> sendMessage());

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(12345);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    if (clientSocket != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String staffName = reader.readLine();
                        System.out.println("0");
                        // Tạo luồng mới để xử lý client
                        new Thread(() -> {
                            try {
                                TextArea textArea = new TextArea();
                                textArea.setEditable(false);

                                Tab tab = new Tab(staffName, textArea);
                                Platform.runLater(() -> tabPane.getTabs().add(tab));

                                // Lắng nghe tin nhắn từ client
                                listenForMessages(clientSocket, staffName);
                                System.out.println("1");

                                // Hiển thị lịch sử tin nhắn sau khi lắng nghe tin nhắn từ client
                                Platform.runLater(() -> displayMessageHistory(staffName));

                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                                clientWriters.put(staffName, writer);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

    }

    private void listenForMessages(Socket clientSocket, String staffName) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String message;
                System.out.println("Listening for messages from " + staffName + "...");
                while ((message = reader.readLine()) != null) {
                    System.out.println("Received message from " + staffName + ": " + message);
                    String finalMessage = message;
                    Platform.runLater(() -> {
                        TextArea textArea = clientTextAreas.get(staffName);
                        if (textArea != null) {
                            textArea.appendText(staffName + ": " + finalMessage + "\n");
                        }
                        Message msg = new Message(staffName, "Manager", finalMessage, LocalDateTime.now());
                        messageHistory.add(msg);
                        saveMessageToXML();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessage() {
        try {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab == null) return;

            String clientName = selectedTab.getText();
            BufferedWriter writer = clientWriters.get(clientName);
            if (writer == null) return;

            String message = chatText.getText();
            if (message.trim().isEmpty()) return;

            writer.write(message + "\n");
            writer.flush();

            TextArea textArea = clientTextAreas.get(clientName);
            if (textArea != null) {
                textArea.appendText("Manager: " + message + "\n");
            }

            Message msg = new Message("Manager", clientName, message, LocalDateTime.now());
            messageHistory.add(msg);
            saveMessageToXML();
            chatText.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayMessageHistory(String clientName) {
        TextArea textArea = clientTextAreas.get(clientName);
        System.out.println("2");
        if (textArea != null) {
            for (Message message : messageHistory) {
                if (message.getReceiver().equals(clientName) || message.getSender().equals(clientName)) {
                    textArea.appendText(message.getSender() + ": " + message.getContent() + "\n");
                }
            }
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
}
