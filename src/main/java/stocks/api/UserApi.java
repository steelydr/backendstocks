package stocks.api;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.redisson.api.RList;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import stocks.models.ChatData;
import stocks.models.GameData;
import stocks.models.UserData;

@GraphQLApi
public class UserApi {

    @Inject
    RedissonClient redissonClient;
    
    @Inject
    ObjectMapper objectMapper;

    @Query("getAllUserEmails")
public List<String> getAllUserEmails() {
    try {
        RMapCache<String, String> userCache = redissonClient.getMapCache("usersCache");
        return userCache.keySet().stream()
                .collect(Collectors.toList());
    } catch (Exception e) {
        throw new RuntimeException("Error fetching user emails", e);
    }
}


    @Query("getUser")
    public UserData getUser(@Name("email") String email) {
        try {
            RMapCache<String, String> userCache = redissonClient.getMapCache("usersCache");
            String userDataJson = userCache.get(email);

            if (userDataJson != null) {
                UserData user = objectMapper.readValue(userDataJson, UserData.class);

                // Fetch associated games
                RList<String> gamesList = redissonClient.getList("games");
                List<GameData> userGames = gamesList.stream()
                        .map(gameJson -> {
                            try {
                                GameData game = objectMapper.readValue(gameJson, GameData.class);
                                return game.getEmail().equals(email) ? game : null;
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(game -> game != null)
                        .collect(Collectors.toList());
                user.setGames(userGames);

                // Fetch associated chats
                RList<String> chatsList = redissonClient.getList("chats");
                List<ChatData> userChats = chatsList.stream()
                        .map(chatJson -> {
                            try {
                                ChatData chat = objectMapper.readValue(chatJson, ChatData.class);
                                // Check if the user is the sender or the recipient
                                return (chat.getSenderEmail().equals(email)
                                        || (chat.getRecipientEmail() != null && chat.getRecipientEmail().equals(email)))
                                        ? chat : null;
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(chat -> chat != null)
                        .collect(Collectors.toList());
                user.setChats(userChats);

                return user;
            }

            throw new RuntimeException("User not found for email: " + email);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching user details", e);
        }
    }

    @Mutation("createUser")
    public String createUser(
            @Name("email") String email,
            @Name("password") String password,
            @Name("displayName") String displayName,
            @Name("photoURL") String photoURL,
            @Name("birthdate") String birthdate) {

        try {
            if (email == null || password == null || displayName == null || birthdate == null) {
                throw new IllegalArgumentException("All fields except photoURL are required");
            }

            UserData user = new UserData(email, password, displayName, photoURL, birthdate, null, null);
            RMapCache<String, String> cache = redissonClient.getMapCache("usersCache");

            if (cache.containsKey(email)) {
                throw new RuntimeException("User already exists with email: " + email);
            }

            cache.put(email, objectMapper.writeValueAsString(user), 24, TimeUnit.HOURS);
            return "User created successfully";
        } catch (Exception e) {
            throw new RuntimeException("Error creating user", e);
        }
    }
}
