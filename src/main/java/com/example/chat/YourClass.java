package com.example.chat;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDateTime;

@XmlRootElement
public class YourClass {

    // Field for LocalDateTime
    private LocalDateTime localDateTimeField;

    // Getter for LocalDateTime
    @XmlElement(name = "localDateTimeField")
    public LocalDateTime getLocalDateTimeField() {
        return localDateTimeField;
    }

    // Setter for LocalDateTime
    public void setLocalDateTimeField(LocalDateTime localDateTimeField) {
        this.localDateTimeField = localDateTimeField;
    }
}
