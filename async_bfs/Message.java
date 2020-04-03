package async_bfs;
/*
 * Team Members:
 * Tanu Rampal (txr180007)
 * Viraj Mavani (vdm180000)
 * This represents a message which has processId, message type, distance from the root, and debug character information.
 */
public class Message {
	// Type of messages
	public enum MessageType {
		READY, NEXT, EXPLORE, ACK, NACK, DONE;
	}
	// Process ID of sender
	private int processID;
	private MessageType mType;
	private int hops;
	private char debugCharacter;
	private int timeToSend;
	private int rootID;
	
	public Message(int PID, MessageType Mtype, int dist, char debugChar){
		this.processID = PID;
		this.mType = Mtype;
		this.hops = dist;
		this.debugCharacter = debugChar;
	}
	
	
	public Message(int processID, MessageType mType, int hops, char debugCharacter, int timeToSend, int rootID) {
		this.processID = processID;
		this.mType = mType;
		this.hops = hops;
		this.debugCharacter = debugCharacter;
		this.timeToSend = timeToSend;
		this.rootID = rootID;
	}


	// getter/setter functions
	public int getProcessId() {
		return processID;
	}

	public void setProcessId(int processId) {
		this.processID = processId;
	}

	public MessageType getMessageType() {
		return mType;
	}

	public void setMessageType(MessageType mtype) {
		this.mType = mtype;
	}

	public double getDistance() {
		return hops;
	}

	public void setDistance(int hops) {
		this.hops = hops;
	}

	public char getDebugChar() {
		return debugCharacter;
	}

	public void setDebugChar(char debugchar) {
		this.debugCharacter = debugchar;
	}
	
	public int getTimeToSend() {
		return timeToSend;
	}
	public void setTimeToSend(int timeToSend) {
		this.timeToSend = timeToSend;
	}
	
	
	public int getRootID() {
		return rootID;
	}

	public void setRootID(int rootID) {
		this.rootID = rootID;
	}

	@Override
	public String toString() {
		return "Message [processID=" + processID + ", mType=" + mType + ", hops=" + hops + ", debugCharacter="
				+ debugCharacter + ", timeToSend=" + timeToSend + ", rootID=" + rootID + "]";
	}

	// Debug function
	public String debug(){
		return "From: " + this.processID + " What: " + this.mType;
	}

}
