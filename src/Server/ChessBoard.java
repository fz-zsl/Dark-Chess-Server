package Server;

import net.sf.json.JSONObject;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class ChessBoard {
	public int flipCounter;
	public int currentSide;
	private int[][] chessIndex=new int[10][7];
	private int[][] objectIndex=new int[10][7];
	private boolean[][] chessFlipped=new boolean[10][7];
	protected ArrayList<Integer> allPossibleMoves;
	public static final int[] indexToChess=new int[]{
			0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 5, 5, 5, 6, 6,
			10,11,11,12,12,13,13,14,14,15,15,15,15,15,16,16
	};

	public int getChessIndex(int x,int y) {
		return chessIndex[x][y];
	}

	public boolean getFlipped(int x,int y) {
		return chessFlipped[x][y];
	}

	public int getObjectIndex(int x,int y) {
		return objectIndex[x][y];
	}

	public int getWholeChessStatus(int x,int y) {
		//objectIndex+50(for flipped)
		return objectIndex[x][y]+(chessFlipped[x][y]?50:0);
	}

	public void chessInit(int x,int y,int chessObjectIndex,boolean flipped) {
		if (chessObjectIndex<0) chessIndex[x][y]=objectIndex[x][y]=chessObjectIndex;
		else {
			chessIndex[x][y]=indexToChess[chessObjectIndex];
			objectIndex[x][y]=chessObjectIndex;
		}
		chessFlipped[x][y]=flipped;
	}

	public void chessInit(int x,int y,int chessObjectIndex) {
		chessInit(x,y,chessObjectIndex,false);
	}

	public void flipChess(int x,int y) {
		chessFlipped[x][y]=true;
		++flipCounter;
	}

	public void flipBackChess(int x,int y) {
		chessFlipped[x][y]=false;
		--flipCounter;
	}

	public int moveChess(int srcX,int srcY,int destX,int destY) {
		//use MoveChess + ChessInit to undo the MoveChess process
		int eatReport=objectIndex[destX][destY]+(chessFlipped[destX][destY]?50:0);
		//if no eat process happens, eatReport = -1
		chessIndex[destX][destY]=chessIndex[srcX][srcY];
		objectIndex[destX][destY]=objectIndex[srcX][srcY];
		chessFlipped[destX][destY]=chessFlipped[srcX][srcY];
		chessIndex[srcX][srcY]=-1;
		objectIndex[srcX][srcY]=-1;
		chessFlipped[srcX][srcY]=false;
		return eatReport;
	}

	public int moveChess(int src,int dest) {
		return moveChess(src/10,src%10,dest/10,dest%10);
	}

	private boolean canEat(int curIndex,int nextIndex) {
		if (curIndex/10==nextIndex/10) return false;
		curIndex%=10;
		nextIndex%=10;
		if (curIndex<=nextIndex&&nextIndex<6) return true;
		if (curIndex==5&&nextIndex==0) return true;
		return nextIndex==6 && curIndex!=5;
	}

	public ArrayList<Integer> calcPossibleMoves(int clickX, int clickY) {
		//the first element will be itself
		allPossibleMoves.clear();
		allPossibleMoves.add(clickX*10+clickY);
		final int[] moveX=new int[]{-1,0,0,1};
		final int[] moveY=new int[]{0,-1,1,0};
		int curIndex=chessIndex[clickX][clickY];
		if (curIndex%10!=6) {
			//not a cannon
			for (int i=0;i<4;++i) {
				int xx=clickX+moveX[i];
				int yy=clickY+moveY[i];
				if (xx<1||xx>8||yy<1||yy>4) continue;
				if (chessIndex[xx][yy]<0) allPossibleMoves.add(xx*10+yy);//move
				else if (canEat(curIndex,chessIndex[xx][yy])&&chessFlipped[xx][yy])
					allPossibleMoves.add(xx*10+yy);//move and eat
			}
		}
		else {
			//is a cannon
			for (int i=0;i<4;++i) {
				int xx=clickX+moveX[i];
				int yy=clickY+moveY[i];
				boolean screen=false;
				if (xx<1||xx>8||yy<1||yy>4) continue;
				if (chessIndex[xx][yy]>=0) {
					//eat near: an empty chess or a chess of another side
					if ((!chessFlipped[xx][yy])||chessIndex[xx][yy]/10!=curIndex/10)
						allPossibleMoves.add(xx*10+yy);
					screen=true;
				}
				while (true) {
					xx+=moveX[i];
					yy+=moveY[i];
					if (xx<1||xx>8||yy<1||yy>4) break;//out of boarder
					if (chessIndex[xx][yy]<0) continue;//no chess, no problem
					//then there's a chess
					if (!screen) {//the chess will be a screen
						screen=true;
						continue;
					}
					//jump and eat: an empty chess or a chess of another side
					if ((!chessFlipped[xx][yy])||chessIndex[xx][yy]/10!=curIndex/10)
						allPossibleMoves.add(xx * 10 + yy);
					break;
				}
			}
		}
		return allPossibleMoves;
	}

	public void clearPossibleMoves() {
		allPossibleMoves=new ArrayList<>();
	}

	class pairs implements Comparable<pairs> {
		private final int index;
		private final double val;

		private pairs(int index,double val) {
			this.index=index;
			this.val=val;
		}

		@Override
		public int compareTo(pairs o) {
			Double curVal=val;
			return curVal.compareTo(o.val);
		}
	}

	public ChessBoard(Socket... sockets) {
		System.out.printf("[%s]New chessboard.\n",Server.getServerTime());
		flipCounter=0;
		currentSide=-1;
		allPossibleMoves=new ArrayList<>();
		Random rand=new Random(System.currentTimeMillis());
		ArrayList<pairs> chessStatus=new ArrayList<>();
		for (int i=0;i<32;++i) chessStatus.add(new pairs(i,rand.nextDouble()));
		chessStatus.sort(Comparator.naturalOrder());
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		for (int i=1;i<=8;++i) {
			for (int j=1;j<=4;++j) {
				chessInit(i,j,chessStatus.get((i-1)*4+j-1).index);
				JSONObject tmpMessage=new JSONObject();
				tmpMessage.put("signalType",2);
				tmpMessage.put("actionType",6);
				tmpMessage.put("objectIndex",chessStatus.get((i-1)*4+j-1).index);
				tmpMessage.put("curX",i);
				tmpMessage.put("curY",j);
				Server.sendMessage(sockets[0],tmpMessage);
				Server.sendMessage(sockets[1],tmpMessage);
			}
		}
	}
}
