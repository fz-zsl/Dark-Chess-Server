package oop;

import Server.Server;
import net.sf.json.JSONObject;
import Server.Game;

public class GameEndsException extends Exception {
	private String info;

	public GameEndsException(String info, Game game) {
		this.info=info;
		JSONObject winnerMessage=new JSONObject();
		winnerMessage.put("signalType",5);
		winnerMessage.put("actionType",3);
		winnerMessage.put("winnerName",info);
		Server.sendMessage(game.getGamer1().getGamerSocket(),winnerMessage);
		Server.sendMessage(game.getGamer2().getGamerSocket(),winnerMessage);
	}

	public int getInfo() {
		return Integer.parseInt(info);
	}
}
