package co.tangia.minecraftmod;

public class CommandComponent {
  public String command;
  public String displayName;

  public String getMessage(String playerName) {
    return this.command.replaceAll("$DISPLAYNAME", this.displayName).replaceAll("$PLAYERNAME", playerName);
  }
}
