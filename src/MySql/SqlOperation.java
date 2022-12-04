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
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		Connection connection=DriverManager.getConnection("jdbc:sqlserver://localhost:9018;DatabaseName=UserDB","root","DarkChessAdmin");

		//Seek for existing users
		String sqlInfo="select * from darkchess.user where userName = ?";
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
		sqlInfo="insert into darkchess.user(userName, encPassword) values(?, ?)";
		preparedStatement=connection.prepareStatement(sqlInfo);

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
		Class.forName("com.mysql.jdbc.Driver");
		Connection connection=DriverManager.getConnection("jdbc:mysql://localhost:8080,DatabaseName=UserDB","root","DarkChessAdmin");

		//Get all user
		String sqlInfo="select * from darkchess.user";
		PreparedStatement preparedStatement=connection.prepareStatement(sqlInfo);
		ResultSet resultSet=preparedStatement.executeQuery();
		while (resultSet.next()) {
			userArrayList.add(new User(resultSet.getString(2),resultSet.getString(3),resultSet.getInt(4),resultSet.getInt(5)));
		}
	}

	public static void updateUser(User user) throws Exception {
		//Connect to sql
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		Connection connection=DriverManager.getConnection("jdbc:sqlserver://localhost:9018;DatabaseName=UserDB","root","DarkChessAdmin");

		//Update user's information
		String sqlInfo="update darkchess.user set winGameCounter = ?, loseGameCounter = ? where userName = ?";
		PreparedStatement preparedStatement=connection.prepareStatement(sqlInfo);
		preparedStatement.setInt(1,user.getWinGameCounter());
		preparedStatement.setInt(2,user.getLoseGameCounter());
		preparedStatement.setString(3, user.getUserName());
		preparedStatement.executeUpdate();
		connection.close();
		preparedStatement.close();
	}
}
