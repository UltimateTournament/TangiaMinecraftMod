package co.tangia.minecraftmod;

public class CommandComponent {
  public String command;

  public String getMessage(String playerName, String displayName) {
    return this.command.replaceAll("$DISPLAYNAME", displayName).replaceAll("$PLAYERNAME", playerName);
  }
}
