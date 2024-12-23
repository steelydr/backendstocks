package stocks.models;

import java.time.Instant;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Name;

public class ChatData {
    private String chatId;
    private String senderEmail;
    private String recipientEmail;
    private String message;
    
    private boolean isAI;
    private Instant timestamp;

    // Default constructor
    public ChatData() {
        this.timestamp = Instant.now();
    }

    // Constructor with fields
    public ChatData(String chatId, String senderEmail, String recipientEmail, String message, boolean isAI) {
        this.chatId = chatId;
        this.senderEmail = senderEmail;
        this.recipientEmail = recipientEmail;
        this.message = message;
        this.isAI = isAI;
        this.timestamp = Instant.now();
    }

    // Getters and setters
    @Description("Unique identifier for the chat conversation")
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    @Description("Email of the message sender")
    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    @Description("Email of the message recipient")
    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    @Description("Content of the message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Description("Indicates if the message is from AI")
    @Name("isAI")
    public boolean isAI() {
        return isAI;
    }

    public void setAI(boolean isAI) {
        this.isAI = isAI;
    }

    @Description("Timestamp when the message was created")
    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ChatData{" +
                "chatId='" + chatId + '\'' +
                ", senderEmail='" + senderEmail + '\'' +
                ", recipientEmail='" + recipientEmail + '\'' +
                ", message='" + message + '\'' +
                ", ai=" + isAI +
                ", timestamp=" + timestamp +
                '}';
    }
}
