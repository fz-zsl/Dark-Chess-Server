package Server;

import MySql.User;
import oop.GameEndsException;

import java.net.Socket;
import java.util.ArrayList;

public class Gamer {
	private String gamerName;
	private int score;
	private Socket gamerSocket;
	private int[] eatenChessCount;

	public Gamer(User user) {
		gamerName=user.getUserName();
		score=0;
		gamerSocket=user.getUserSocket();
		System.out.printf("[%s]Gamer: %s %s\n",Server.getServerTime(),gamerName,gamerSocket);
		eatenChessCount=new int[7];
	}

	public void modifyPoints(int delta,Game game) throws GameEndsException {
		score+=delta;

		if (score>=60)
			throw new GameEndsException(gamerName,game);
	}

	public String getGamerName() {
		return gamerName;
	}

	public int getScore() {
		return score;
	}

	public Socket getGamerSocket() {
		return gamerSocket;
	}

	public ArrayList<Integer> getEatenChessCount() {
		ArrayList<Integer> result=new ArrayList<>();
		for (int i=0;i<7;++i) result.add(eatenChessCount[i]);
		return result;
	}
}
