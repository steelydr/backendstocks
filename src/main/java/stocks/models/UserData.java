// UserData.java
package stocks.models;

import java.util.List;

import org.eclipse.microprofile.graphql.Type;

@Type
public class UserData {
    private String email;
    private String password;
    private String displayName;
    private String photoURL;
    private String birthdate;
    private List<GameData> games;
    private List<ChatData> chats;

    public UserData() {}

    public UserData(String email, String password, String displayName, String photoURL, String birthdate, List<GameData> games, List<ChatData> chats) {
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        this.photoURL = photoURL;
        this.birthdate = birthdate;
        this.games = games;
        this.chats = chats;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoURL() { return photoURL; }
    public void setPhotoURL(String photoURL) { this.photoURL = photoURL; }

    public String getBirthdate() { return birthdate; }
    public void setBirthdate(String birthdate) { this.birthdate = birthdate; }

    public List<GameData> getGames() { return games; }
    public void setGames(List<GameData> games) { this.games = games; }

    public List<ChatData> getChats() { return chats; }
    public void setChats(List<ChatData> chats) { this.chats = chats; }

    @Override
    public String toString() {
        return "UserData{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", displayName='" + displayName + '\'' +
                ", photoURL='" + photoURL + '\'' +
                ", birthdate='" + birthdate + '\'' +
                ", games=" + games +
                ", chats=" + chats +
                '}';
    }
}