package paliy.model;

import paliy.util.DBUtilSQLite;

import java.sql.SQLException;

public class MikrotikDAO {

        //*************************************
        //INSERT new mk
        //*************************************
        public static boolean insertBook(String ip, String mac, String name, String ros, String descrip, String status) throws SQLException, ClassNotFoundException {
            //Declare a INSERT statement
            String updateStmt = " INSERT INTO MIKROTIK ( ip, MAC, NAME, ROS, DESCRIPTION, STATUS) VALUES (" +
                    "'" + ip +
                    "','" + mac +
                    "','" + name +
                    "','" + ros +
                    "','" + descrip +
                    "','" + status +
                    "')";
            try {
                DBUtilSQLite.dbExecuteUpdate(updateStmt);
            } catch (SQLException e) {
                System.out.print("Error occurred while INSERT Operation: " + e);
                return false;
            }
            return true;
        }
}


/*
* CREATE TABLE `Mikrotik` (
	`id`	INTEGER NOT NULL,
	`MAC`	TEXT,
	`name`	TEXT,
	`ros`	TEXT,
	`descripton`	TEXT,
	PRIMARY KEY(id)
);
* */