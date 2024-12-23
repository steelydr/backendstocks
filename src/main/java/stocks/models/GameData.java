package stocks.models;

import org.eclipse.microprofile.graphql.Type;

@Type
public class GameData {
    private String gameId;
    private String email;
    private String symbol; // Stock symbol
    private String userPrediction; // Prediction like "up" or "down"
    private boolean won;
    private int coinsEarned;

    // Default constructor
    public GameData() {
    }

    public GameData(String gameId, String email, String symbol, String userPrediction, boolean won, int coinsEarned) {
        this.gameId = gameId;
        this.email = email;
        this.symbol = symbol;
        this.userPrediction = userPrediction;
        this.won = won;
        this.coinsEarned = coinsEarned;
    }

    // Getters and setters
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getUserPrediction() { return userPrediction; }
    public void setUserPrediction(String userPrediction) { this.userPrediction = userPrediction; }

    public boolean isWon() { return won; }
    public void setWon(boolean won) { this.won = won; }

    public int getCoinsEarned() { return coinsEarned; }
    public void setCoinsEarned(int coinsEarned) { this.coinsEarned = coinsEarned; }

    @Override
    public String toString() {
        return "{" +
                "gameId='" + gameId + '\'' +
                ", email='" + email + '\'' +
                ", symbol='" + symbol + '\'' +
                ", userPrediction='" + userPrediction + '\'' +
                ", won=" + won +
                ", coinsEarned=" + coinsEarned +
                
                '}';
    }
}
