import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * The program that facilitates interaction between the user and the MySQL server of their choice.
 * @author Jayke R. Huempfner
 */
public class ServerComm {

	// using information coming from http://dev.mysql.com/doc/connector-j/en/connector-j-usagenotes-basic.html
	
	private Connection conn = null;
	
	/**
	 * Connect to a server at localhost:3306.
	 * @param db The name of the database (schema) to connect to.
	 * @param user The name of the user accessing the database.
	 * @param password The password for the user accessing the database.
	 */
	public void connect(String db, String user, String password)
	{
        try {
            // The newInstance() call is a work around for some
            // broken Java implementations

            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
        	ex.printStackTrace();
            // handle the error
        }
		
		try {
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + db + "?user=" + user + "&password=" + password);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Connect to a server with a custom connection string.
	 * @param connection The connection string used to locate and connect to the database. Strings must start with "jdbc:mysql://".
	 */
	public void connect(String connection)
	{
        try {
            // The newInstance() call is a work around for some
            // broken Java implementations

            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
        	ex.printStackTrace();
            // handle the error
        }
		
		try {
			conn = DriverManager.getConnection(connection);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// me and my glorious method-naming skills
	
	/**
	 * The function that forms the command-line interface allowing the user to work with the MySQL server.
	 */
	public void interact()
	{
		// as long as this is false, the program will continue to run
		boolean exit = false;
		
		// name of the table to work with
		String table  = "";
		
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		
		System.out.println("Welcome to the MySQL Server Connection Sample! Your connection has been set up.");
		System.out.println("Use the command 'Help' to see what commands are available.");
		
		while (exit == false)
		{
			System.out.print(">"); // Add '>' to give visual indication of "type here!"
			String com = sc.nextLine();
			
			switch (com.toLowerCase())
			{
			case "help":
				System.out.println("The following commands are available to you:");
				System.out.println();
				System.out.println("'Add': Add a new record to the table.");
				System.out.println("'About': Learn more about this program.");
				System.out.println("'Display': Show all records and data within a table.");
				System.out.println("'Delete': Delete records in a table that match a single condition.");
				System.out.println("'Describe': Describe the columns of the table.");
				System.out.println("'Exit': Quit the program.");
				System.out.println("'Help': Display this help screen.");
				System.out.println("'SQL': Enter Direct SQL mode to send commands straight to the server.");
				System.out.println("'Table': Select which table in the database to use.");
				break;
			case "exit":
				exit = true;
				break;
			case "about":
				System.out.println("MySQL Server Connection Sample");
				System.out.println("Created by Jacob R. Huempfner, January 2015");
				System.out.println("jaykebird.github.com");
				break;
			case "table":
				// Select the table to work with
				System.out.println("Type the name of the table you wish to work with:");
				
				// get and display a list of tables
				ResultSet rst = makeCall("SHOW TABLES");
				try {
					while (rst.next())
					{
						System.out.println(rst.getString(1));
					}
				} catch (SQLException e1) {
					errorOccurred(e1, "SHOW TABLES");
				}
				
				System.out.print("Table>"); // Add '>' to give visual indication of "type here!"
				String tbl = sc.nextLine();
				
				table = tbl;
				System.out.println("The table " + tbl + " has been selected.");
				break;
			case "display":
				if (table.isEmpty())
				{
					System.out.println("Please select a table to use with the 'Table' command.");
					break;
				}
				
				String queryd = "SELECT * FROM " + table;
				
				ResultSet rs = makeCall(queryd);
				ResultSetMetaData rsmd;
				
				if (rs == null)
				{
					// This means no data was returned.
					// The error should've been shown to the user
					// in the makeCall function.
					break;
				}
				
				try
				{
					rsmd = rs.getMetaData();
					
					int cols = rsmd.getColumnCount();
					String[] colnames = new String[cols];
					
					System.out.println("Table columns:");
					for (int i = 1; i <= cols; i++)
					{
						colnames[i - 1] = rsmd.getColumnName(i);
						System.out.println("Column " + Integer.toString(i) + ": " + rsmd.getColumnName(i) + ", of type " + rsmd.getColumnTypeName(i));
					}
					
					while (rs.next())
					{
						System.out.println();
						
						for (int i = 0; i < cols; i++)
						{
							System.out.println(colnames[i] + " = " + rs.getString(i + 1));
						}
					}
				}
				catch (SQLException e)
				{
					errorOccurred(e, queryd);
				}
				
				break;
			case "describe":
				if (table.isEmpty())
				{
					System.out.println("Please select a table to use with the 'Table' command.");
					break;
				}
				
				try
				{
					ArrayList<ColumnData> cols = ColumnData.buildColumnData(makeCall("DESCRIBE " + table));
					
					for (ColumnData col : cols)
					{
						System.out.println("Column " + col.getIndex() + ": " + col.getName() + (col.getNullable()?" (not-nullable)":" (nullable)") + " of type " + col.getDataType() + ( col.isPrimaryKey() ?" is a primary key":""));
					}
				}
				catch (SQLException e)
				{
					errorOccurred(e, "DESCRIBE " + table);
				}
				
				break;
			case "add":
				if (table.isEmpty())
				{
					System.out.println("Please select a table to use with the 'Table' command.");
					break;
				}
				
				String qat = "DESCRIBE " + table; // string needed for displaying correct information in error data
				
				try {
					ArrayList<ColumnData> cols = ColumnData.buildColumnData(makeCall("DESCRIBE " + table));
					
					System.out.println("Adding a new record to the table:");
					
					ArrayList<String> svals = new ArrayList<String>(); // list of columns and values
					
					for (ColumnData col : cols)
					{
						boolean fval = false; // is the entered value valid for this column's data type
						String cval = ""; // the value to be entered for this column
						while (fval == false)
						{
							System.out.println("Enter value for column " + col.getName() + " (" + col.getDataType() + ")");
							System.out.print("Value>"); // Add '>' to give visual indication of "type here!"
							String pval = sc.nextLine();
							
							boolean res = col.isValidValue(pval);
							
							if (res == false)
							{
								System.out.println("The value '" + pval + "' is in an invalid format for this column.");
							}
							else
							{
								cval = pval; // have a valid value
							}
							
							fval = res;
						}
						
						if (col.isNumberType() == false)
						{
							cval = "'" + cval + "'";
						}
						
						// For binary strings for the data type BIT
						// strings are formatted as "b'100101'"
						if (col.getType() == ColumnDataType.Bit)
						{
							cval = "b" + cval;
						}
						
						svals.add(col.getName() + "=" + cval); // add value with column name into the arraylist	
					}
					
					// now to use the arraylist to enter the command into MySQL
					
					String vallist = "";
					
					for (String val : svals)
					{
						vallist = vallist + ", " + val;
					}
					
					vallist = vallist.substring(2); // remove the leading comma
					
					qat = "INSERT INTO " + table + " SET " + vallist;
					
					// enter command into MySQL
					int addres = makeUpdateCall("INSERT INTO " + table + " SET " + vallist);
					
					System.out.println(addres + " rows updated");
					
				} catch (SQLException e) {
					errorOccurred(e, qat);
					//e.printStackTrace();
				}
				
				break;
			case "delete":
				if (table.isEmpty())
				{
					System.out.println("Please select a table to use with the 'Table' command.");
					break;
				}
				
				String qdt = "DESCRIBE " + table; // string needed for displaying correct information in error data
				
				try {
					ArrayList<ColumnData> cols = ColumnData.buildColumnData(makeCall("DESCRIBE " + table));
					
					System.out.println("Deleting records from the table based upon a single condition:");
					
					ArrayList<String> colnames = new ArrayList<String>(); // list of columns
					
					for (ColumnData col : cols)
					{
						System.out.println("Column " + col.getIndex() + ": " + col.getName() + (col.getNullable()?" (not-nullable)":" (nullable)") + " of type " + col.getDataType());
						colnames.add(col.getName());
					}
					
					boolean validname = false;
					String colname = ""; // name of the column to compare against
					
					while (validname == false)
					{
						System.out.print("Name of the column to compare against>");
						
						String name = sc.nextLine();
						if (colnames.contains(name))
						{
							validname = true;
							colname = name;
						}
						else
						{
							System.out.println("There is no column with this name.");
						}
					}
				
					// get the ColumnData for the specified column
					int index = colnames.indexOf(colname);
					ColumnData colm = cols.get(index);
					
					System.out.println("Type the value to compare.");
					System.out.println("Table records that have this value in this column will be deleted.");
					System.out.print("Value>");
					
					String wvalue = sc.nextLine();
					
					if (colm.isNumberType() == false)
					{
						wvalue = "'" + wvalue + "'";
					}
					
					// formulate where condition
					String where = colname + "=" + wvalue;
					
					qdt = "DELETE FROM " + table + " WHERE " + where;
					
					// enter command into MySQL
					int delch = makeUpdateCall("DELETE FROM " + table + " WHERE " + where);
					
					System.out.println(delch + " rows updated");
					
				} catch (SQLException e) {
					errorOccurred(e, qdt);
					//e.printStackTrace();
				}
				
				break;
			case "sql":
				System.out.println("Direct SQL mode activated");
				System.out.println("Each command will be sent straight to the SQL server.");
				System.out.println("Type 'exit' to leave Direct SQL mode.");
				
				boolean dsm = true;
				
				while (dsm)
				{
					System.out.print("Command>");
					String command = sc.nextLine();
					
					if (command.toLowerCase().equals("exit"))
					{
						dsm = false;
					}
					else if (command.toUpperCase().startsWith("UPDATE") || command.toUpperCase().startsWith("INSERT") || command.toUpperCase().startsWith("DELETE") || command.toUpperCase().startsWith("ALTER"))
					{
						int dsmres = makeUpdateCall(command);
						System.out.println(dsmres + " rows updated");
					}
					else
					{
						ResultSet dsmrs = makeCall(command);
						
						ResultSetMetaData dsmrsmd;
						
						if (dsmrs != null)
						{
							// This means no data was returned.
							// The error should've been shown to the user
							// in the makeCall function.
							try
							{
								dsmrsmd = dsmrs.getMetaData();
								
								int cols = dsmrsmd.getColumnCount();
								String[] colnames = new String[cols];
								
								System.out.println("Table columns:");
								for (int i = 1; i <= cols; i++)
								{
									colnames[i - 1] = dsmrsmd.getColumnName(i);
									System.out.println("Column " + Integer.toString(i) + ": " + dsmrsmd.getColumnName(i) + ", of type " + dsmrsmd.getColumnTypeName(i));
								}
								
								while (dsmrs.next())
								{
									System.out.println();
									
									for (int i = 0; i < cols; i++)
									{
										System.out.println(colnames[i] + " = " + dsmrs.getString(i + 1));
									}
								}
							}
							catch (SQLException e)
							{
								errorOccurred(e, command);
							}
						}
					}
				}
				
				System.out.println("Leaving Direct SQL mode");
				break;
			default:
				
				System.out.println("Could not interpret command '" + com + "'.");
				System.out.println("Use the 'Help' command to display a list of available commands.");
				break;
			}
		}
		
	}
	
	/**
	 * Send a SQL query statement to the MySQL server and return the results as a ResultSet object.
	 * If an error is encountered, the program will automatically handle it.
	 * @param command The query statement to be sent.
	 * @return The ResultSet that represents the data returned from the query statement.
	 */
	private ResultSet makeCall(String command)
	{
		Statement stmt = null;
		try
		{
			// Create SQL statement and return the resulting data from the query
			stmt = conn.createStatement();
			return stmt.executeQuery(command);
		}
		catch (SQLException e)
		{
			errorOccurred(e, command);
			
			// null out the statement object, to release memory
		    if (stmt != null) {
		        try { stmt.close(); } catch (SQLException sqlEx) { } // ignore
	
		        stmt = null;
		    }
		}
		
		return null;
		
	}
	
	/**
	 * Send a SQL update statement to the MySQL server and return the results as a ResultSet object.
	 * If an error is encountered, the program will automatically handle it.
	 * @param command The query statement to be sent.
	 * @return The row count as a result of this statement. 0 if row count is not relevant to the statement.
	 */
	private int makeUpdateCall(String command)
	{
		Statement stmt = null;
		try
		{
			// Create SQL statement and return the resulting data from the query
			stmt = conn.createStatement();
			return stmt.executeUpdate(command);
		}
		catch (SQLException e)
		{
			errorOccurred(e, command);
			
			// null out the statement object, to release memory
		    if (stmt != null) {
		        try { stmt.close(); } catch (SQLException sqlEx) { } // ignore
	
		        stmt = null;
		    }
		}
		
		return 0;
		
	}
	
	private void errorOccurred(SQLException e, String query)
	{
		System.out.println("An error occurred while sending a query:");
		System.out.println("Query: " + query);
		System.out.println("SQL State: " + e.getSQLState());
		System.out.println("Error message: " + e.getMessage());
		System.out.println("Error code: " + e.getErrorCode());
		System.out.println();
	}
	
}
