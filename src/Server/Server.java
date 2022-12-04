package Server;

import MySql.SqlOperation;
import MySql.User;
import net.sf.json.JSONObject;

import javax.xml.crypto.Data;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Server {
	private ArrayList<User> userInDataBase=new ArrayList<>();
	private ArrayList<User> userOnline=new ArrayList<User>();
	private DataInputStream inputStream=null;
	private DataOutputStream outputStream=null;

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

		//Socket connection
		try {
			//start server
			ServerSocket serverSocket=new ServerSocket(9018);
			System.out.printf("[%s]Server.Server is online.\n",getServerTime());

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
				int actionType=infoPackage.getInt("actionType");
				if (actionType==1) {//register
					if (SqlOperation.addNewUser(user)) {
						userInDataBase.add(user);
						userOnline.add(user);
						JSONObject tmpMessage=new JSONObject();
						tmpMessage.put("signalType",1);
						tmpMessage.put("actionType",1);
						tmpMessage.put("result",true);
						if (pendingUser!=null) {
							Game game=new Game(pendingUser,user);
							pendingUser.setGame(game);
							user.setGame(game);
						}
						sendMessage(user.getUserSocket(),tmpMessage);
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
						}
						else pendingUser=user;
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
			//Todo: put generalInit somewhere in the program
			try {
				DataInputStream userCallStream=new DataInputStream(socket.getInputStream());
				while (true) {
					String userCallString=userCallStream.readUTF();
					JSONObject userCallInfo=JSONObject.fromObject(userCallString);
					int signalType=userCallInfo.getInt("signalType");
					int actionType=userCallInfo.getInt("actionType");
					if (signalType!=2&&signalType!=3) {
						JSONObject tmpMessage=new JSONObject();
						tmpMessage.put("signalType",5);
						tmpMessage.put("actionType",1);
						sendMessage(socket,tmpMessage);
						continue;
					}
					if (signalType==3) {//Todo
						JSONObject tmpMessage=new JSONObject();
						tmpMessage.put("Info","Todo: send back rank list.");
						sendMessage(socket,tmpMessage);
						continue;
					}
					//Todo
				}
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
}