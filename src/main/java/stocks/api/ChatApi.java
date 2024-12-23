package stocks.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import stocks.models.ChatData;

@GraphQLApi
public class ChatApi {

    @Inject
    RedissonClient redissonClient;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Retrieve all chat messages for a given user.
     * This will return all messages from any conversation where the user is either
     * the sender or the recipient.
     */
    @Query("getChatsByUser")
    public List<ChatData> getChatsByUser(@Name("email") String email) {
        try {
            RMapCache<String, String> cache = redissonClient.getMapCache("chatsCache");
            List<ChatData> result = new ArrayList<>();

            // Iterate through each conversation
            for (String chatId : cache.keySet()) {
                String json = cache.get(chatId);
                if (json == null) continue;
                List<ChatData> conversation = objectMapper.readValue(json, new TypeReference<List<ChatData>>() {});
                
                // Collect messages where the user is involved
                for (ChatData msg : conversation) {
                    if (email.equals(msg.getSenderEmail()) ||
                       (msg.getRecipientEmail() != null && email.equals(msg.getRecipientEmail()))) {
                        result.add(msg);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching chats for user", e);
        }
    }

    /**
     * Send a message. If a conversation between the sender and recipient already exists,
     * it will use the same chatId. If not, it will create a new one.
     * 
     * For simplicity, we define chatId as a stable key based on the two participants.
     * If talking to AI, we just use "AI" as one of the participants.
     */
    @Mutation("sendMessage")
    public String sendMessage(@Name("senderEmail") String senderEmail,
                              @Name("recipientEmail") String recipientEmail,
                              @Name("message") String message,
                              @Name("isAI") boolean isAI) {
        try {
            RMapCache<String, String> cache = redissonClient.getMapCache("chatsCache");

            // If it's AI chat, the recipientEmail is always "AI"
            if (isAI) {
                recipientEmail = "AI";
            } else {
                if (recipientEmail == null || recipientEmail.isEmpty()) {
                    throw new RuntimeException("recipientEmail must be provided for user-to-user chats");
                }
            }

            // Create a stable chatId using sorted emails
            String chatId = generateChatId(senderEmail, recipientEmail);

            // Try to fetch existing conversation
            String existingConversationJson = cache.get(chatId);
            List<ChatData> conversation;
            if (existingConversationJson != null) {
                conversation = objectMapper.readValue(existingConversationJson, new TypeReference<List<ChatData>>() {});
            } else {
                conversation = new ArrayList<>();
            }

            // Add the new message
            ChatData chat = new ChatData(chatId, senderEmail, recipientEmail, message, isAI);
            conversation.add(chat);

            // Store the updated conversation back into the cache
            cache.put(chatId, objectMapper.writeValueAsString(conversation), 30, TimeUnit.DAYS);

            return "Message sent successfully";
        } catch (Exception e) {
            throw new RuntimeException("Error sending message", e);
        }
    }

    @Query("getChatsByParticipants")
public List<ChatData> getChatsByParticipants(@Name("senderEmail") String senderEmail, 
                                              @Name("recipientEmail") String recipientEmail) {
    try {
        RMapCache<String, String> cache = redissonClient.getMapCache("chatsCache");
        List<ChatData> result = new ArrayList<>();

        // Generate the chatId for the given participants
        String chatId = generateChatId(senderEmail, recipientEmail);
        
        // Fetch the conversation for this chatId
        String json = cache.get(chatId);
        if (json == null) {
            return result; // Return empty if no conversation exists
        }

        // Convert the stored conversation data into List<ChatData>
        List<ChatData> conversation = objectMapper.readValue(json, new TypeReference<List<ChatData>>() {});

        // Add all messages from the conversation
        result.addAll(conversation);

        return result;
    } catch (Exception e) {
        throw new RuntimeException("Error fetching chats for participants", e);
    }
}


    /**
     * Generate a stable chatId for two participants by sorting their identifiers.
     * For user-to-user: "alice@example.com" and "bob@example.com"
     * chatId might look like: "alice@example.com:bob@example.com"
     * For user-to-AI: "alice@example.com" and "AI"
     * chatId might look like: "AI:alice@example.com" (alphabetically sorted)
     */
    private String generateChatId(String user1, String user2) {
        return Stream.of(user1, user2)
                     .sorted()
                     .collect(Collectors.joining(":"));
    }
}
