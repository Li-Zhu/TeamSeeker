public enum MessageType {
		USAGE_DISPLAY(0),
		LOGIN_SUCCESS(1),
		LOGIN_FAIL(2),
		SIGNUP_SUCCESS(3),
		SIGNUP_FAIL(4),
		CAL_DISPLAY(5),
		TIME_TO_PLAY(6),
		WAIT_NOTIF(9);
		
		int val;
		private MessageType(int val) {
			this.val = val;
		}
		
		public int value() {
			return val;
		}
		
		public static MessageType get(int val) {
			switch (val) {
			case 0:
				return USAGE_DISPLAY;
			case 1:
				return LOGIN_SUCCESS;
			case 2:
				return LOGIN_FAIL;
			case 3:
				return SIGNUP_SUCCESS;
			case 4:
				return SIGNUP_FAIL;
			case 5:
				return CAL_DISPLAY;
			case 6:
				return TIME_TO_PLAY;
			case 9:
				return WAIT_NOTIF;
			default:
				return null;
			}
		}
	}