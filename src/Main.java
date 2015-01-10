import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		
		String db  = "";
		String user  = "";
		String pass  = "";
		
		if (args.length == 3)
		{
			// database name, username, and password passed as arguments
			
			db = args[0];
			user = args[1]; 
			pass = args[2];
			
			// it's inconsistent to make 'user' and 'pass' the second and third arguments here
			// when they're the first and second in the previous block, I know,
			// but I decided to do it because 1. it follows the order of the parameters for 
			// the function. 2. The database name is the most important argument, and I like
			// putting the arguments in order of importance.
		}
		else
		{
			System.out.println("When running this program, make sure you specify a database, username, and password.");
			System.out.println("For example, 'MySqlConnSample.jar myDatabase Username Pass123'.");
			
			@SuppressWarnings("resource")
			Scanner sc = new Scanner(System.in);
			System.out.println("The program will exit when you press 'Enter'.");
			sc.nextLine();
			return;
		}
		
        try {
            // The newInstance() call is a work around for some
            // broken Java implementations

            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) { ex.printStackTrace(); }
        
        ServerComm sc = new ServerComm();
        
        // Important note that the MySQL database must be accessible at "localhost:3306".
        
        sc.connect(db, user, pass);
        sc.interact();
        
	}


}