package MySql;

import Server.Game;

import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User {
	private String userName;
	private String encryptedPassword;
	private Socket userSocket;
	private int winGameCounter;
	private int loseGameCounter;
	private Game game;

	public User() {}

	public User(String userName,String encryptedPassword,int winGameCounter,int loseGameCounter) {
		this.userName=userName;
		this.encryptedPassword=encryptedPassword;
		this.winGameCounter=winGameCounter;
		this.loseGameCounter=loseGameCounter;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName=userName;
	}

	public String getEncryptedPassword() {
		return encryptedPassword;
	}

	public void setEncryptedPassword(String encryptedPassword) {
		this.encryptedPassword=encryptedPassword;
	}

	public Socket getUserSocket() {
		return userSocket;
	}

	public void setUserSocket(Socket userSocket) {
		this.userSocket=userSocket;
	}

	public double getWinRate() {
		return 1.0*winGameCounter/(winGameCounter+loseGameCounter);
	}

	public int getWinGameCounter() {
		return winGameCounter;
	}

	public void setWinGameCounter(int winGameCounter) {
		this.winGameCounter=winGameCounter;
	}

	public void winAGame() {
		++winGameCounter;
	}

	public int getLoseGameCounter() {
		return loseGameCounter;
	}

	public void setLoseGameCounter(int loseGameCounter) {
		this.loseGameCounter=loseGameCounter;
	}

	public void loseAGame() {
		++loseGameCounter;
	}

	public void setGame(Game game) {
		this.game=game;
	}

	public Game getGame(Game game) {
		return game;
	}

	public static String encryptByMD5(String plainText) {
		byte[] secretBytes=null;
		try {
			secretBytes=MessageDigest.getInstance("md5").digest(plainText.getBytes());
		} catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			throw new RuntimeException(noSuchAlgorithmException);
		}
		String md5code=new BigInteger(1,secretBytes).toString(16);
		for (int i=0;i<32-md5code.length();++i) md5code="0"+md5code;
		return md5code;
	}

	public boolean tryPassword(String password) {
		return encryptedPassword.equals(password);
	}
}
