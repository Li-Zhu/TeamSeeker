import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;


public class Client {
	private static InetAddress address;
	private static final int port = 8011;
	private static Socket client_socket;
	private static DataOutputStream output;
	private static DataInputStream input;
	
	public static void main(String [] args) throws IOException{
		connectToServer();
		final BufferedReader reader = new BufferedReader( new InputStreamReader(System.in));
		boolean loginOK = false;
		MessageType type = null;
		try {
			while (!loginOK) {
				System.out.print("Input name: ");
				String name = reader.readLine();

				System.out.print("Input password: ");
				String password = reader.readLine();
				
				output.writeUTF(name);
				output.writeUTF(password);
				
				int status = input.readInt();
				type = MessageType.get(status);
				if (type == null) {
					System.err.println("Unknown state received from server: " + status);
					continue;
				} 
				
				switch (type) {
				case LOGIN_SUCCESS:
					loginOK = true;
					System.out.println("Login successful!");
					break;
				case LOGIN_FAIL:
					System.out.println("Authentification failed!");
					break;
				default:
					System.out.println("Unexpected state: " + type);
				}
			}
			
			new Thread() {
				public void run() {
					while (true) {
						try {
							String line = null;
							while ((line = reader.readLine()) != null) {
								line = line.trim();
								if (line.length() > 0) {
									break;
								}
							}
							if (line == null) { //should never happen
								System.err.println("Null line.");					
								break;
							}
							synchronized(output) {
								output.writeUTF(line);
							}
						} catch(IOException e) {
							System.err.println("Exception occurred, details: " + e.getLocalizedMessage());
							break;
						}
					}
				}
			}.start();
			
			while (true) {
				int status = input.readInt();
				type = MessageType.get(status);
				if (type == null) {
					System.err.println("Unknown state received from server: " + status);
					continue;
				} 
				switch (type) {
				case USAGE_DISPLAY:
					String usage = input.readUTF();
					System.out.println("Usage:\n" + usage);
					break;
				case SIGNUP_SUCCESS:
					System.out.println("Sign up successful!");
					break;
				case SIGNUP_FAIL:
					System.out.println("Sign up failed!");
					break;
				case CAL_DISPLAY:
					System.out.println(input.readUTF());
					break;
				case TIME_TO_PLAY:
					System.out.println("Time to play! You are in the game now");
					break;
				case WAIT_NOTIF:
					System.out.println("Added to wait list, waiting notification...");
					break;
					
				default:
					System.out.println("Unexpected state: " + type);					
				}
			}
			
	  }	catch (Exception e) {
				System.err.println("Exception occurred, details: " + e.getLocalizedMessage());
				System.exit(0);
		}
	}
	
	public static void connectToServer() throws IOException{
		address = InetAddress.getLoopbackAddress();
		client_socket = new Socket(address,port);
		input = new DataInputStream(client_socket.getInputStream()); 
		output = new DataOutputStream(client_socket.getOutputStream()); 
	}

}
