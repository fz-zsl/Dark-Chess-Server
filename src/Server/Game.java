package Server;

import MySql.User;
import net.sf.json.JSONObject;

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

	public Gamer getGamer1() {
		return gamer1;
	}

	public Gamer getGamer2() {
		return gamer2;
	}

	public ChessBoard getChessBoard() {
		return chessBoard;
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
					tmpMessage=new JSONObject();
					tmpMessage.put("signalType",4);
					tmpMessage.put("actionType",1);
					tmpMessage.put("eatenChessCount1",gamer1.getEatenChessCount());
					tmpMessage.put("eatenChessCount2",gamer2.getEatenChessCount());
					Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
					Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
				}
			}
		} else {
			if (chessBoard.getObjectIndex(clickX,clickY)<0) return;
			//there is no previous click - first click
			if (!chessBoard.getFlipped(clickX,clickY)) {
				//flip
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

	//Operations

	private int[] operationType=new int[100005];
	private int[] srcPosition=new int[100005];
	private int[] destPosition=new int[100005];
	public int sizeOfStack=0;

	/*
	0 - flip (pos)
	1 - move (src,dest)
	2 - eat  (pos,object index of the eaten chess)
	 */

	public void addOperationToStack(int op,int... positions) {
		JSONObject tmpMessage=new JSONObject();
		operationType[sizeOfStack]=op;
		if (op==0) {//flip a chess
			srcPosition[sizeOfStack]=positions[0];
		} else if (op==1) {//move a chess
			srcPosition[sizeOfStack]=positions[0];
			destPosition[sizeOfStack]=positions[1];
		} else if (op==2) {//eat a chess
			srcPosition[sizeOfStack]=positions[0];
			destPosition[sizeOfStack]=positions[1];
			if (positions[1]%50==0) {
				tmpMessage=new JSONObject();
				tmpMessage.put("signalType",5);
				tmpMessage.put("actionType",2);
				tmpMessage.put("generalType",1);
				Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
				Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
			}
			if (positions[1]%50==16) {
				tmpMessage=new JSONObject();
				tmpMessage.put("signalType",5);
				tmpMessage.put("actionType",2);
				tmpMessage.put("generalType",0);
				Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
				Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
			}
		}
		++sizeOfStack;
		if (op<2) chessBoard.currentSide^=1;
	}

	public int popOperationFromStack() {
		--sizeOfStack;
		if (operationType[sizeOfStack]<2) chessBoard.currentSide^=1;
		return operationType[sizeOfStack]*10000+
				srcPosition[sizeOfStack]*100+
				destPosition[sizeOfStack];
	}

	//Undo previous operation
	public void undoPreviousOperation() throws Exception {
		JSONObject tmpMessage=new JSONObject();
		chessBoard.clearPossibleMoves();
		tmpMessage=new JSONObject();
		tmpMessage.put("signalType",2);
		tmpMessage.put("actionType",5);
		Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
		Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
		int lastOperation=popOperationFromStack();
		int type=lastOperation/10000;
		int srcPosition=lastOperation%10000/100;
		int destPosition=lastOperation%100;
		if (type==0) {
			chessBoard.flipBackChess(srcPosition/10,srcPosition%10);
			tmpMessage=new JSONObject();
			tmpMessage.put("signalType",3);
			tmpMessage.put("actionType",1);
			tmpMessage.put("objectIndex",chessBoard.getObjectIndex(srcPosition/10,srcPosition%10));
			Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
			Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
		} else if (type==1) {
			chessBoard.moveChess(destPosition,srcPosition);
			tmpMessage=new JSONObject();
			tmpMessage.put("signalType",2);
			tmpMessage.put("actionType",2);
			tmpMessage.put("objectIndex",chessBoard.getObjectIndex(destPosition/10,destPosition%10));
			tmpMessage.put("preX",destPosition/10);
			tmpMessage.put("preY",destPosition%10);
			tmpMessage.put("curX",srcPosition/10);
			tmpMessage.put("curY",srcPosition%10);
			Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
			Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
		} else if (type==2) {
			if (destPosition%50<16) {//undo: black side eats red chess
				gamer2.modifyPoints(-scorePerChess[ChessBoard.indexToChess[destPosition%50]]);
			} else {//undo: red side eats black chess
				gamer1.modifyPoints(-scorePerChess[ChessBoard.indexToChess[destPosition%50]]);
			}
			lastOperation=popOperationFromStack();
			int destOfLastOperation=lastOperation%100;
			int srcOfLastOperation=lastOperation%10000/100;
			chessBoard.moveChess(destOfLastOperation,srcOfLastOperation);
			chessBoard.chessInit(srcPosition/10,srcPosition%10,destPosition%50,destPosition/50>0);
			tmpMessage=new JSONObject();
			tmpMessage.put("signalType",2);
			tmpMessage.put("actionType",2);
			tmpMessage.put("objectIndex",chessBoard.getObjectIndex(destOfLastOperation/10,destOfLastOperation%10));
			tmpMessage.put("preX",destOfLastOperation/10);
			tmpMessage.put("preY",destOfLastOperation%10);
			tmpMessage.put("curX",srcOfLastOperation/10);
			tmpMessage.put("curY",srcOfLastOperation%10);
			Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
			Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
			tmpMessage=new JSONObject();
			tmpMessage.put("signalType",4);
			tmpMessage.put("actionType",1);
			tmpMessage.put("eatenChessCount1",gamer1.getEatenChessCount());
			tmpMessage.put("eatenChessCount2",gamer2.getEatenChessCount());
			Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
			Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
			tmpMessage=new JSONObject();
			tmpMessage.put("signalType",3);
			tmpMessage.put("actionType",2);
			tmpMessage.put("curX",destOfLastOperation/10);
			tmpMessage.put("curY",destOfLastOperation%10);
			Server.sendMessage(gamer1.getGamerSocket(),tmpMessage);
			Server.sendMessage(gamer2.getGamerSocket(),tmpMessage);
		}
	}
}
