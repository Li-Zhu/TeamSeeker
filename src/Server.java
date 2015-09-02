import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Server{
	private ServerSocket serverSocket;
	// private static ArrayList<String[]> userInfo = new ArrayList<String[]>();
	// private static ArrayList<String[]> online = new ArrayList<String[]>();

	/*
	public String dateask = "What day are you planning to play? (Type: Won, Tue, Wed, Thu, Fri, Sat or Sun) ";
	public String timeask = "What time are you planning to play? (Format: yyyy-MM-dd HH:mm:ss)";
	public String gameask = "What game are you planning to play?";
	public String whatgame;
	public String whattime;
	public String whatdate;

	public String calendarUSG = "Welcome to gamer calendar! \n"
	    + "type 'add' to add event \n" + "type 'drop' to drop event\n"
	    + "type 'show' to check out the calendar";
	    */
	
	public String usage = "  -C: request to check calendar\n"
			+ "  -S <GameTime(dd/hh)> <GameName> <GameRole> : sign up for a time slot\n"
			+ "  -W: gives client wait list option\n";


	private static Map<String, String> userInfo = new HashMap<String, String>();

	static {
		userInfo.put("Yogi", "bear");
		userInfo.put("Tom", "tom123");
		userInfo.put("Henry", "gosh");
		userInfo.put("Yuzuli", "chan");
	}
	

	public void start(int port) {
		try {
			serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	public Socket accept() throws IOException {
		if (serverSocket == null) {
			throw new RuntimeException("Server socket not initialized!");
		}
		return serverSocket.accept();
	}
	
	private class WorkThread extends Thread {		
		Socket socket;
		public WorkThread(Socket socket) {
			this.socket = socket;
		}
		
		public void run() {
			String userName = null;
			boolean loginOK = false;
			try {
				DataInputStream input = new DataInputStream(
						socket.getInputStream());
				DataOutputStream output = new DataOutputStream(
						socket.getOutputStream());
				// long time = System.currentTimeMillis();
				// output.writeUTF("Socket connected to Server.");
				while (true) {
					//output.writeUTF("Input user name: ");
					userName = input.readUTF();
					//output.writeUTF("Input user password: ");
					String password = input.readUTF();
					System.out.println("Recieved a login request, "
							+ "user name: " + userName + ", password: " + password);
					if (loginAuthentication(userName, password)) {
						loginOK = true;
						GameCenter.userGetOnline(userName, socket);
						output.writeInt(MessageType.LOGIN_SUCCESS.value());
						break;
					} else {
						output.writeInt(MessageType.LOGIN_FAIL.value());
					}
				}
				
				
				//login successful
				//display the menu
				output.writeInt(MessageType.USAGE_DISPLAY.value());
				output.writeUTF(usage);					
				
				String line = null;
				while ((line = input.readUTF()) != null) { //loop checking the user input
					boolean paramsOK = false;
					line = line.trim();
					if (line.length() == 0) continue;
					String[] params = line.replaceAll("\\s+", " ").split(" ");
					switch (params[0]) {
					case "-C":
						if (params.length == 1) {
							paramsOK = true;
							output.writeInt(MessageType.CAL_DISPLAY.value());
							output.writeUTF(GameCenter.getCalInfo(userName));
						}
						break;
					case "-S":
						if (params.length == 4) {
							paramsOK = true;
							if(GameCenter.signup(userName, params[1], params[2], params[3])) {
								output.writeInt(MessageType.SIGNUP_SUCCESS.value());
							} else {
								output.writeInt(MessageType.SIGNUP_FAIL.value());
							}
						}
						break;
					case "-W":
						if (params.length == 1) {
							paramsOK = true;
							GameCenter.addUserToWaitList(userName);
							output.writeInt(MessageType.WAIT_NOTIF.value());
						}
						break;
					default:
						break;
				  }
					
					if (!paramsOK) {
						System.err.println("Bad user input!");
						output.writeInt(MessageType.USAGE_DISPLAY.value());
						output.writeUTF(usage);
					}
			  }
			} catch (Exception e) {
				System.err.println("Exception occurred on user's connection" 
						+ (userName == null ? "" : ": " + userName) 
						+ ", cause: " + e.getLocalizedMessage());
			} finally {
				if (loginOK) {
					GameCenter.userGetOffline(userName);
				}					
			}
				
		}
	}

	public boolean loginAuthentication(String username, String password) {
		if (GameCenter.isUserOnline(username) 
				|| !userInfo.containsKey(username)
				|| !userInfo.get(username).equals(password)
				)
			return false;
		return true;
	}

	private static int portNumber = 8011;	
	public static void main(String[] args) {
		GameCenter.init();
		Server server = new Server();
		try {
			server.start(portNumber);
			System.out.println("Listening for incoming connections...");
			while (true) {
				Socket socket = server.accept();
				System.out.println("Connection established with: " + socket.getInetAddress());
				server.new WorkThread(socket).start();
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
