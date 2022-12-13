package Server;

import MySql.SqlOperation;
import MySql.User;
import net.sf.json.JSONObject;
import oop.GameEndsException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

public class Server {
	public ArrayList<User> userInDataBase=new ArrayList<>();
	private ArrayList<User> userOnline=new ArrayList<>();
	private DataInputStream inputStream=null;
	private int timeLimit=30000;

	public static void main(String[] args) {
		try {
			new Server();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public static String getServerTime() {
		//Time in simple date format
		SimpleDateFormat serverDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		Date serverTime=new Date(System.currentTimeMillis());
		return serverDateFormat.format(serverTime);
	}

	public Server() throws Exception {
		//Load all users
		SqlOperation.loadAllUsers(userInDataBase);

		for (User user:userInDataBase)
			System.out.println(user.toString());

		//Socket connection
		try {
			//start server
			ServerSocket serverSocket=new ServerSocket(9019);
			System.out.printf("[%s]Server is online.\n",getServerTime());

			//ready for users to login
			User pendingUser=null;
			while (true) {
				Socket userSocket=serverSocket.accept();
				if (userSocket==null) continue;
				inputStream=new DataInputStream(userSocket.getInputStream());
				String infoPackagestring=inputStream.readUTF();
				JSONObject infoPackage=JSONObject.fromObject(infoPackagestring);
				User user=new User();
				user.setUserName(infoPackage.getString("userName"));
				user.setEncryptedPassword(User.encryptByMD5(infoPackage.getString("password")));
				user.setUserSocket(userSocket);
				System.out.println("["+getServerTime()+"]New user:\n"+user.toString());
				int actionType=infoPackage.getInt("actionType");
				if (actionType==1) {//register
					if (SqlOperation.addNewUser(user)) {
						userInDataBase.add(user);
						userOnline.add(user);
						JSONObject tmpMessage=new JSONObject();
						tmpMessage.put("signalType",1);
						tmpMessage.put("actionType",1);
						tmpMessage.put("result",true);
						sendMessage(user.getUserSocket(),tmpMessage);
						if (pendingUser!=null) {
							Game game=new Game(pendingUser,user);
							pendingUser.setGame(game);
							user.setGame(game);
							pendingUser=null;
							tmpMessage=new JSONObject();
							tmpMessage.put("signalType",1);
							tmpMessage.put("actionType",3);
							tmpMessage.put("result",true);
						}
						else {
							pendingUser=user;
							tmpMessage.put("signalType",1);
							tmpMessage.put("actionType",3);
							tmpMessage.put("result",false);
						}
					}
					else {
						JSONObject tmpMessage=new JSONObject();
						tmpMessage.put("signalType",1);
						tmpMessage.put("actionType",1);
						tmpMessage.put("result",false);
						sendMessage(user.getUserSocket(),tmpMessage);
					}
				} else if (actionType==2) {//login
					boolean mk=false;
					for (User cur:userInDataBase) {
						if (cur.getUserName().equals(user.getUserName())) {
							if (!cur.tryPassword(user.getEncryptedPassword())) {
								JSONObject tmpMessage=new JSONObject();
								tmpMessage.put("signalType",1);
								tmpMessage.put("actionType",1);
								tmpMessage.put("result",false);
								sendMessage(user.getUserSocket(),tmpMessage);
								break;
							}
							user.setWinGameCounter(cur.getWinGameCounter());
							user.setLoseGameCounter(cur.getLoseGameCounter());
							userOnline.add(user);
							mk=true;
							break;
						}
					}
					JSONObject tmpMessage=new JSONObject();
					tmpMessage.put("signalType",1);
					tmpMessage.put("actionType",2);
					tmpMessage.put("result",mk);
					sendMessage(user.getUserSocket(),tmpMessage);
					if (mk) {
						if (pendingUser!=null) {
							Game game=new Game(pendingUser,user);
							pendingUser.setGame(game);
							user.setGame(game);
							pendingUser=null;
							tmpMessage.put("signalType",1);
							tmpMessage.put("actionType",3);
							tmpMessage.put("result",true);
							sendMessage(user.getUserSocket(),tmpMessage);
						}
						else {
							pendingUser=user;
							tmpMessage.put("signalType",1);
							tmpMessage.put("actionType",3);
							tmpMessage.put("result",false);
							sendMessage(user.getUserSocket(),tmpMessage);
						}
					}
				} else {
					JSONObject tmpMessage=new JSONObject();
					tmpMessage.put("signalType",5);
					tmpMessage.put("actionType",1);
					sendMessage(user.getUserSocket(),tmpMessage);
				}
				new Thread(new ServerThread(userSocket)).start();
			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	public static void sendMessage(Socket userSocket,JSONObject messageInfo) {
		try {
			DataOutputStream outputStream=new DataOutputStream(userSocket.getOutputStream());
			outputStream.writeUTF(messageInfo.toString());
		} catch (IOException ioException) {
			System.out.printf("[%s]Message is rejected by the client.",getServerTime());
			System.out.println(messageInfo.toString());
		}
	}

	class ServerThread implements Runnable {
		private final Socket socket;

		public ServerThread(Socket socket) {
			this.socket=socket;
		}

		@Override
		public void run() {
			JSONObject tmpMessage;
			try {
				DataInputStream userCallStream=new DataInputStream(socket.getInputStream());
				while (true) {
					String userCallString=userCallStream.readUTF();
					JSONObject userCallInfo=JSONObject.fromObject(userCallString);
					int signalType=userCallInfo.getInt("signalType");
					int actionType=userCallInfo.getInt("actionType");
					if (signalType!=2&&signalType!=3) {
						tmpMessage=new JSONObject();
						tmpMessage.put("signalType",5);
						tmpMessage.put("actionType",1);
						sendMessage(socket,tmpMessage);
						continue;
					}
					User caller=null;
					for (User i:userOnline)
						if (i.getUserSocket()==socket) {
							caller=i;
							break;
						}
					Game game=caller.getGame();
					if (signalType==3) {
						sendMessage(socket,getRankList(caller));
						continue;
					}

					if (caller.getGame().equals(game.getLastGamer().getGamerName())) {
						//a click of the same user
						if ((System.currentTimeMillis()-game.getLastMoveTimeStamp())/timeLimit%2==0) {
							tmpMessage=new JSONObject();
							tmpMessage.put("signalType",5);
							tmpMessage.put("actionType",5);
							sendMessage(socket,tmpMessage);
							//Todo: mute the sentence if there's no time limit
							continue;
						}
					}
					else {
						//a click of a different user
						if ((System.currentTimeMillis()-game.getLastMoveTimeStamp())/timeLimit%2==1) {
							tmpMessage=new JSONObject();
							tmpMessage.put("signalType",5);
							tmpMessage.put("actionType",5);
							sendMessage(socket,tmpMessage);
							//Todo: mute the sentence if there's no time limit
							continue;
						}
					}

					if (actionType==1) {
						try {
							game.clickOnBoard(userCallInfo.getInt("clickX"),userCallInfo.getInt("clickY"));
						} catch (GameEndsException gameEndsException) {
							tmpMessage=new JSONObject();
							tmpMessage.put("signalType",5);
							tmpMessage.put("actionType",3);
							tmpMessage.put("Info",gameEndsException.toString());
							sendMessage(game.getGamer1().getGamerSocket(),tmpMessage);
							sendMessage(game.getGamer2().getGamerSocket(),tmpMessage);
						} catch (Exception exception) {
							exception.printStackTrace();
						}
					} else if (actionType==2) {
						try {
							game.undoPreviousOperation();
						} catch (Exception exception) {
							exception.printStackTrace();
						}
					} else if (actionType==3) {
						User partner=null;
						for (User i:userOnline)
							if (i.getGame()==game&&i!=caller) {
								partner=i;
								break;
							}
						Game newGame=new Game(caller,partner);
						caller.setGame(newGame);
						partner.setGame(newGame);
					} else if (actionType==4) {
						tmpMessage=new JSONObject();
						tmpMessage.put("signalType",5);
						tmpMessage.put("actionType",5);
						tmpMessage.put("ObjectIndex",game.getChessBoard().getObjectIndex(userCallInfo.getInt("clickX"),userCallInfo.getInt("clickY")));
						sendMessage(socket,tmpMessage);
					} else {
						tmpMessage=new JSONObject();
						tmpMessage.put("signalType",5);
						tmpMessage.put("actionType",1);
						sendMessage(socket,tmpMessage);
					}
				}
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	class waitTread implements Runnable {
		@Override
		public void run() {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			JSONObject tmpMessage=new JSONObject();
			tmpMessage.put("signalType",0);
			tmpMessage.put("actionType",0);
		}
	}

	private class UserAndCredit implements Comparable<UserAndCredit> {
		public String userName;
		public double credit;

		public UserAndCredit(String userName,double credit) {
			this.userName=userName;
			this.credit=credit;
		}

		@Override
		public int compareTo(UserAndCredit o) {
			Double cre=this.credit;
			return -cre.compareTo(o.credit);
		}
	}

	private JSONObject getRankList(User user) {
		JSONObject result=new JSONObject();
		result.put("signalType",4);
		result.put("actionType",2);
		ArrayList<UserAndCredit> rankList=new ArrayList<>();
		for (User cur:userInDataBase)
			rankList.add(new UserAndCredit(cur.getUserName(),cur.getWinRate()));
		rankList.sort(Comparator.reverseOrder());
		result.put("rankList",rankList);
		result.put("userScore",user.getWinRate());
		for (int i=0;i<rankList.size();++i)
			if (rankList.get(i).userName.equals(user.getUserName())) {
				result.put("userRank",i);
				break;
			}
		result.put("userCount",rankList.size());
		return result;
	}
}