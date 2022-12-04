package MySql;

import Server.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class SqlOperation {
	public static boolean addNewUser(User newUser) throws Exception {
		//Connect to sql
		Class.forName("org.sqlite.JDBC");
		Connection connection=DriverManager.getConnection("jdbc:sqlite:D:\\DarkChessServer\\database\\DarkChessUsers.sqlite");

		//Seek for existing users
		String sqlInfo="select * from main.Users where userName = ?";
		PreparedStatement preparedStatement=connection.prepareStatement(sqlInfo);
		preparedStatement.setString(1, newUser.getUserName());
		ResultSet resultSet=preparedStatement.executeQuery();
		if (resultSet.next()) {
			System.out.printf("[%s]There is a user with the same name, registration failed.\n",Server.getServerTime());
			connection.close();
			preparedStatement.close();
			return false;
		}

		//Add new user
		String sqlInsertInfo="insert into main.Users(UserName, EncryptedPassword) values(?, ?)";
		preparedStatement=connection.prepareStatement(sqlInsertInfo);

		//Fill in the information
		preparedStatement.setString(1, newUser.getUserName());
		preparedStatement.setString(2, newUser.getEncryptedPassword());

		int modifiedInfoCounter=preparedStatement.executeUpdate();
		if (modifiedInfoCounter>0) {
			System.out.printf("[%s]User %s has been registered successfully.\n",Server.getServerTime(),newUser.getUserName());
			connection.close();
			preparedStatement.close();
			return true;
		}
		System.out.printf("[%s]User %s's request for registration has been rejected.\n",Server.getServerTime(),newUser.getUserName());
		connection.close();
		preparedStatement.close();
		return false;
	}

	public static void loadAllUsers(ArrayList<User> userArrayList) throws Exception {
		//Connect to sql
		Class.forName("org.sqlite.JDBC");
		Connection connection=DriverManager.getConnection("jdbc:sqlite:D:\\DarkChessServer\\database\\DarkChessUsers.sqlite");
		System.out.printf("[%s]SQLite is connected successfully.\n",Server.getServerTime());

		//Get all user
		String sqlInfo="select * from main.Users";
		PreparedStatement preparedStatement=connection.prepareStatement(sqlInfo);
		ResultSet resultSet=preparedStatement.executeQuery();
		while (resultSet.next()) {
			userArrayList.add(new User(resultSet.getString(2),resultSet.getString(3),resultSet.getInt(4),resultSet.getInt(5)));
		}
	}

	public static void updateUser(User user) throws Exception {
		//Connect to sql
		Class.forName("org.sqlite.JDBC");
		Connection connection=DriverManager.getConnection("jdbc:sqlite:D:\\DarkChessServer\\database\\DarkChessUsers.sqlite");

		//Update user's information
		String sqlInfo="update main.Users set winGameCounter = ?, loseGameCounter = ? where userName = ?";
		PreparedStatement preparedStatement=connection.prepareStatement(sqlInfo);
		preparedStatement.setInt(1,user.getWinGameCounter());
		preparedStatement.setInt(2,user.getLoseGameCounter());
		preparedStatement.setString(3, user.getUserName());
		preparedStatement.executeUpdate();
		connection.close();
		preparedStatement.close();
	}
}
