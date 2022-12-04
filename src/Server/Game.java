package Server;

import MySql.User;

public class Game {
	private Gamer gamer1;
	private Gamer gamer2;
	private ChessBoard chessBoard;
	public Game(User user1, User user2) {
		Gamer gamer1=new Gamer(user1);
		Gamer gamer2=new Gamer(user2);
		chessBoard=new ChessBoard(user1.getUserSocket(),user2.getUserSocket());
	}
}
