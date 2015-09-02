import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class GameCenter {

	private static final String GAME_POKER = "poker";
	private static final int GAME_MEMBER_CNT = 2;
	public static boolean useHourMinFlag = true;

	static List<String> waitList = new LinkedList<String>();
	
	private static int maxDays = -1;
	private static Calendar assistCal = null;
	
	private static TreeMap<Long, List<String>> signUpInfo
		= new TreeMap<Long, List<String>>();
	private static Map<String, Socket> online = new HashMap<String, Socket>();
	
	private static boolean hasGame(String name) {
		return name.equals(GAME_POKER);
	}
	
	public static void init() {
		assistCal = Calendar.getInstance();
		if (useHourMinFlag) {
			
		} else {
			assistCal.set(Calendar.DAY_OF_MONTH, 1);
			assistCal.set(Calendar.HOUR_OF_DAY, 0);
		}
		assistCal.set(Calendar.MINUTE, 0);
		assistCal.set(Calendar.SECOND, 0);
		assistCal.set(Calendar.MILLISECOND, 0);
		maxDays = assistCal.getActualMaximum(Calendar.DATE); 
		//baseTime = assistCal.getTimeInMillis();
		
		new GameNotifThread().start();
	}
	
	static long halfHour = 30 * 60 * 1000;
	static class GameNotifThread extends Thread {
		public GameNotifThread() {
			setDaemon(true);
		}
		
		public void run() {
			while(true) {
				synchronized(signUpInfo) {
					long time = System.currentTimeMillis();
					Map<Long, List<String>> upto = 
							new TreeMap<Long, List<String>>(signUpInfo.headMap(time + 1));
					for(Long key : upto.keySet()) {
						signUpInfo.remove(key);
						List<String> users = upto.get(key);
						List<String> onlineUsersForCurrGame = new ArrayList<String>();
						for (String user : users) {
							if (isUserOnline(user)) {
								onlineUsersForCurrGame.add(user);
							}
						}
						int tableCnt = onlineUsersForCurrGame.size() / GAME_MEMBER_CNT;
						if (tableCnt > 0) {
							for(int i = 0; i < tableCnt; i++) {
								for (int j = 0; j < GAME_MEMBER_CNT; j++) {
										String user = onlineUsersForCurrGame.get(GAME_MEMBER_CNT * i + j);
										System.out.println("User: " + user + " is playing at table: " + (i+1));
										removeUserFromWaitList(user);
										notifyToPlay(user);
								}
							}
						}
						
						if (onlineUsersForCurrGame.size() % GAME_MEMBER_CNT != 0) { //check waiting list
							int need = GAME_MEMBER_CNT - (onlineUsersForCurrGame.size() % GAME_MEMBER_CNT);
							List<String> filteredWaitList = new ArrayList<String>(waitList);
							filteredWaitList.removeAll(onlineUsersForCurrGame);
							if (filteredWaitList.size() < need) {
								System.out.println("Need " + need + " more people to start the game, "
										+ "wait list count not enough: " + filteredWaitList.size());
								System.out.println("Reschedule the game after half an hour");
								signUpInfo.put(time + halfHour, new ArrayList<String>());
							} else {
								for (int j = tableCnt * GAME_MEMBER_CNT; j < onlineUsersForCurrGame.size(); j++) {
										String user = onlineUsersForCurrGame.get(j);
										System.out.println("User: " + user + " is playing at table: " + (tableCnt+1));
										removeUserFromWaitList(user);
										notifyToPlay(user);
								}
								
								int count = 0;
								for (String user : new ArrayList<String>(waitList)) {
									if (count++ >= need) {
										break;
									}
                  System.out.println("A user : " + user + 
                  		" from wait-list is playing at table: " + (tableCnt+1));
									notifyToPlay(user);
							    removeUserFromWaitList(user); //from the actual list
								}
							}
						}
						
					}
					
					if (signUpInfo.size() == 0) {
						try {
							signUpInfo.wait();
            } catch (InterruptedException e) {
            }
					} else {
						long waitTime = signUpInfo.firstKey() - time;
						if (waitTime > 0) {
							try {
								signUpInfo.wait(waitTime);
	            } catch (InterruptedException e) {
	            }
						}
					}
				}			
			}
		}
		
		
	}
	
	private static void notifyToPlay(String user) {
		Socket socket = online.get(user);
    try {
      DataOutputStream output = new DataOutputStream(
      		socket.getOutputStream());
      //TODO need synchronization protection on output
      output.writeInt(MessageType.TIME_TO_PLAY.val);
    } catch (IOException e) {
      e.printStackTrace();
    }
	}
	
	public static String getCalInfo(String userName) {
		
		if (signUpInfo.size() == 0) {
			return "Empty calender - not signed up for any time slot";
		} 
		
		int day = -1;
		StringBuffer sb = new StringBuffer();
		synchronized(signUpInfo) {
			for(Long millis: signUpInfo.keySet()) {
				List<String> users = signUpInfo.get(millis);
				
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(millis);
				int newday = cal.get(Calendar.DAY_OF_MONTH);
				int hour = cal.get(Calendar.HOUR_OF_DAY);
				
				if (useHourMinFlag) {
					newday = cal.get(Calendar.HOUR_OF_DAY);
					hour = cal.get(Calendar.MINUTE);
				}
				
				if (day == -1 ||(day != -1 && newday != day)) {					
					sb.append("  " + (useHourMinFlag ? "hour" : "day") + ": " + newday + "\n");
				}
				
				sb.append("    " + (useHourMinFlag ? "minute" : "hour") + ": " 
						+ hour + ", users: " + users + "\n");				
				day = newday;
			}
		}
		
		return "Game sign-up calender:\n" + sb.toString();
	}
	
	public static void userGetOnline(String user, Socket socket) {
		online.put(user, socket);
	}
	
	public static void userGetOffline(String user) {
		online.remove(user);
		waitList.remove(user);
	}
	
	public static boolean isUserOnline(String user) {
		return online.containsKey(user);
	}
	
	public static void addUserToWaitList(String user) {
		if(!waitList.contains(user)){
			waitList.add(user);
			System.out.println("User added to wait list: " + user);
			System.out.println("Current wait list: " + waitList);
		}
	}
	
	public static void removeUserFromWaitList(String user) {
		if(waitList.contains(user)){
			waitList.remove(user);
			System.out.println("User removed from wait list: " + user);
			System.out.println("Current wait list: " + waitList);
		}
	}
	
	
	public static boolean signup(String userName, String time, String gameName, String gameRole) {
		if (!hasGame(gameName)) {
			System.err.println("Bad game name: " + gameName);
			return false;
		}
		
		String[] fields = time.trim().split("/");
		if (fields.length != 2) {
			System.err.println("Bad time: " + time);
			return false;
		}
		
		int day = -1;
		int hour = -1;
		try {
			day = Integer.parseInt(fields[0]);
			hour = Integer.parseInt(fields[1]);
		} catch (Exception e) {}
		
		if (useHourMinFlag) {			
			if (day < 0 || day > 23 || hour < 0 || hour > 59) {
				System.err.println("Bad hour or minute value: " + time);
				return false;
			}
		} else {
			if (day < 0 || day > maxDays || hour < 0 || hour > 23) {
				System.err.println("Bad day or hour value: " + time);
				return false;
			}
		}
		
		synchronized(signUpInfo) {
			if (useHourMinFlag) {			
				assistCal.set(Calendar.HOUR_OF_DAY, day);
				assistCal.set(Calendar.MINUTE, hour);
			} else {
				assistCal.set(Calendar.DAY_OF_MONTH, day);
				assistCal.set(Calendar.HOUR_OF_DAY, hour);
			}
			List<String> users = signUpInfo.get(assistCal.getTimeInMillis());
			if (users == null) {
				users = new ArrayList<String>();
				signUpInfo.put(assistCal.getTimeInMillis(), users);
			}		
			
			if (!users.contains(userName)) {
				users.add(userName);
				System.out.println("User successfully signed up! User name: " + userName + ", signed time: " + assistCal.getTime());
			} 
			signUpInfo.notifyAll();
		}
		return true;
	}
}
