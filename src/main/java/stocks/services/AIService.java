package stocks.services;

import java.util.List;

import stocks.models.ChatData;

public interface AIService {
    String generateResponse(String userMessage, List<ChatData> conversationHistory);
}