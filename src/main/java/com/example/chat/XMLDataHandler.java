package com.example.chat;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;

public class XMLDataHandler {
    public static void writeToXML(ChatHistory chatHistory, String filePath) {
        try {
            JAXBContext context = JAXBContext.newInstance(ChatHistory.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(chatHistory, new File(filePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ChatHistory readFromXML(String filePath) {
        try {
            JAXBContext context = JAXBContext.newInstance(ChatHistory.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (ChatHistory) unmarshaller.unmarshal(new File(filePath));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

