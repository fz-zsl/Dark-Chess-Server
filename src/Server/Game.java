package Server;

import MySql.User;
import net.sf.json.JSONObject;
import oop.GameEndsException;

import java.net.MalformedURLException;
import java.util.ArrayList;

public class Game {
	private Gamer gamer1;
	private Gamer gamer2;
	private ChessBoard chessBoard;
	public static final int[] scorePerChess={30,10,5,5,5,1,5};

	public Game(User user1, User user2) {
		Gamer gamer1=new Gamer(user1);
		Gamer gamer2=new Gamer(user2);
		chessBoard=new ChessBoard(user1.getUserSocket(),user2.getUserSocket());
	}

	public void clickOnBoard(int clickX, int clickY) throws Exception	{
		/*
		values of clickType:
		-1 | invalid click, which shouldn't appear in the log
		 0 | a click that makes a flip
		 1 | first click for move
		 2 | second click for move
		 */
		int clickIndex=clickX*10+clickY;
		JSONObject tmpMessage;
		if (chessBoard.allPossibleMoves.size()>0) {
			//there is a previous click - second click
			//cancel highlight
			int preX=chessBoard.allPossibleMoves.get(0)/10;
			int preY=chessBoard.allPossibleMoves.get(0)%10;
			tmpMessage=new JSONObject();
			tmpMessage.put("signalType",2);
			tmpMessage.put("actionType",5);
			Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
			Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
			if (chessBoard.getChessIndex(clickX,clickY)>=0
					&&!chessBoard.getFlipped(clickX,clickY)
					&&chessBoard.getChessIndex(preX,preY)%10!=6) {
				//flip
				chessBoard.flipChess(clickX,clickY);
				addOperationToStack(0,clickX*10+clickY);
				chessBoard.clearPossibleMoves();
				tmpMessage=new JSONObject();
				tmpMessage.put("signalType",2);
				tmpMessage.put("actionType",1);
				tmpMessage.put("objectIndex",chessBoard.getObjectIndex(clickX,clickY));
				Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
				Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
			} else if (chessBoard.allPossibleMoves.contains(clickIndex)) {
				//move
				int eatReport=chessBoard.moveChess(preX,preY,clickX,clickY);
				addOperationToStack(1,preX*10+preY,clickX*10+clickY);
				if (eatReport>=0) {
					addOperationToStack(2,clickX*10+clickY,eatReport);
					if (eatReport%50<16) {//black side eats red chess
						gamer2.modifyPoints(scorePerChess[ChessBoard.indexToChess[eatReport%50]]);
					} else {//red side eats black chess
						gamer1.modifyPoints(scorePerChess[ChessBoard.indexToChess[eatReport%50]]);
					}
				}
				chessBoard.clearPossibleMoves();
				tmpMessage=new JSONObject();
				tmpMessage.put("signalType",2);
				tmpMessage.put("actionType",2);
				tmpMessage.put("objectIndex",chessBoard.getObjectIndex(clickX,clickY));
				tmpMessage.put("preX",preX);
				tmpMessage.put("preY",preY);
				tmpMessage.put("curX",clickX);
				tmpMessage.put("curY",clickY);
				Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
				Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
				if (eatReport>=0) {
					tmpMessage=new JSONObject();
					tmpMessage.put("signalType",2);
					tmpMessage.put("actionType",3);
					tmpMessage.put("objectIndex",chessBoard.getObjectIndex(clickX,clickY));
					tmpMessage.put("curX",clickX);
					tmpMessage.put("curY",clickY);
					Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
					Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
					tmpMessage.put("signalType",4);
					tmpMessage.put("actionType",1);
					tmpMessage.put("eatenChessCount1",gamer1.getEatenChessCount());
					tmpMessage.put("eatenChessCount2",gamer2.getEatenChessCount());
					Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
					Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
				}
			}
		} else {
			//there is no previous click - first click
			if (!chessBoard.getFlipped(clickX,clickY)) {
				//flip
				System.out.println("Let's Flip - first click!");
				chessBoard.flipChess(clickX,clickY);
				if (chessBoard.flipCounter==1) {
					chessBoard.currentSide=chessBoard.getChessIndex(clickX,clickY)/10;
					if (chessBoard.currentSide>0) {
						Gamer tmpGamer=gamer1;
						gamer1=gamer2;
						gamer2=tmpGamer;
					}
				}
				addOperationToStack(0,clickX*10+clickY);
				tmpMessage=new JSONObject();
				tmpMessage.put("signalType",2);
				tmpMessage.put("actionType",1);
				tmpMessage.put("objectIndex",chessBoard.getObjectIndex(clickX,clickY));
				Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
				Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
			} else if (chessBoard.getChessIndex(clickX,clickY)/10==chessBoard.currentSide) {
				ArrayList<Integer> APM=chessBoard.calcPossibleMoves(clickX,clickY);
				tmpMessage=new JSONObject();
				tmpMessage.put("signalType",2);
				tmpMessage.put("actionType",4);
				tmpMessage.put("APM",APM);
				Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
				Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
			}
		}
	}


}
