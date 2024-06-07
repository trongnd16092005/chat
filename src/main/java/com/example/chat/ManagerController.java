package com.example.chat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javax.xml.bind.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.*;

public class ManagerController {
    @FXML
    private TabPane tabPane;
    @FXML
    private TextField chatText;
    @FXML
    private Button sendButton;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private final Map<String, BufferedWriter> clientWriters = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, TextArea> clientTextAreas = Collections.synchronizedMap(new HashMap<>());
    private final List<Message> messageHistory = Collections.synchronizedList(new ArrayList<>());
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
                System.out.println("Server started on port 12345");

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Client connected: " + clientSocket.getInetAddress());
                        new Thread(() -> handleClient(clientSocket)).start();
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            e.printStackTrace();
                        } else {
                            System.out.println("Server socket closed.");
                        }
                    }
                }
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    e.printStackTrace();
                } else {
                    System.out.println("Server socket closed.");
                }
            }
        });
        serverThread.start();
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String encodedUsername = reader.readLine();
            String encodedPassword = reader.readLine();

            if (encodedUsername == null || encodedPassword == null) {
                System.out.println("Client disconnected before sending credentials.");
                clientSocket.close();
                return;
            }

            String username = new String(Base64.getDecoder().decode(encodedUsername), StandardCharsets.UTF_8);
            String password = new String(Base64.getDecoder().decode(encodedPassword), StandardCharsets.UTF_8);

            if (authenticate(username, password)) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                writer.write("success\n");
                writer.flush();

                Platform.runLater(() -> setupClientInterface(clientSocket, username));
            } else {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
                    writer.write("failure\n");
                    writer.flush();
                }
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean authenticate(String username, String password) {
        DatabaseConnection connectNow = new DatabaseConnection();
        Connection connectDB = connectNow.getConnection();

        String verifyLogin = "SELECT count(1) FROM chat.users WHERE username = ? AND password = ?";

        try {
            PreparedStatement statement = connectDB.prepareStatement(verifyLogin);
            statement.setString(1, username);
            statement.setString(2, password);

            ResultSet queryResult = statement.executeQuery();

            if (queryResult.next() && queryResult.getInt(1) == 1) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void setupClientInterface(Socket clientSocket, String username) {
        TextArea textArea = new TextArea();
        textArea.setEditable(false);

        Tab tab = new Tab(username, textArea);
        tabPane.getTabs().add(tab);
        clientTextAreas.put(username, textArea);
        displayMessageHistory(username);

        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            clientWriters.put(username, writer);

            // Bắt đầu thread lắng nghe tin nhắn từ client
            new Thread(() -> listenForMessages(clientSocket, username)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForMessages(Socket clientSocket, String username) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String message;
            while ((message = reader.readLine()) != null) {
                String finalMessage = message;
                Platform.runLater(() -> {
                    TextArea textArea = clientTextAreas.get(username);
                    if (textArea != null) {
                        textArea.appendText(username + ": " + finalMessage + "\n");
                    }
                    Message msg = new Message(username, "Manager", finalMessage, LocalDateTime.now());
                    messageHistory.add(msg);
                    saveMessageToXML();
                });
            }
        } catch (IOException e) {
            // Xử lý khi client ngắt kết nối hoặc gặp lỗi
            Platform.runLater(() -> handleClientDisconnection(username));
        }
    }

    private void handleClientDisconnection(String username) {
        clientTextAreas.remove(username);
        BufferedWriter writer = clientWriters.remove(username);
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        tabPane.getTabs().removeIf(tab -> tab.getText().equals(username));
        System.out.println("Client " + username + " disconnected.");
    }

    private void sendMessage() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) return;

        String clientName = selectedTab.getText();
        BufferedWriter writer = clientWriters.get(clientName);
        if (writer == null) return;

        try {
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
        if (textArea != null) {
            for (Message message : messageHistory) {
                if (message.getReceiver().equals(clientName) || message.getSender().equals(clientName)) {
                    textArea.appendText(message.getSender() + ": " + message.getContent() + "\n");
                }
            }
        }
    }

    private void saveMessageToXML() {
        try {
            JAXBContext context = JAXBContext.newInstance(ChatHistory.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setMessages(messageHistory);

            marshaller.marshal(chatHistory, new File(chatHistoryFile));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private void loadMessageHistory() {
        try {
            File file = new File(chatHistoryFile);
            if (!file.exists()) return;

            JAXBContext context = JAXBContext.newInstance(ChatHistory.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            ChatHistory chatHistory = (ChatHistory) unmarshaller.unmarshal(file);
            messageHistory.clear();
            messageHistory.addAll(chatHistory.getMessages());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
