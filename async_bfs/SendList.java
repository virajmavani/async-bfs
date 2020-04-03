package async_bfs;
/*
 * Team Members:
 * Sujal Patel (ssp150930)
 * Harshil Shah (hxs155030)
 * Sagar Mehta (sam150930)
 * 
 * This class represents the SendList which is the data structure that holds the messages of the process, until they are ready to send.
 */
public class SendList {
	private Processes process;
	private Message message;
	private boolean sent = false;
	
	public Processes getProcess() {
		return process;
	}
	public void setProcess(Processes process) {
		this.process = process;
	}
	public Message getMessage() {
		return message;
	}
	public void setMessage(Message message) {
		this.message = message;
	}
	public boolean isSent() {
		return sent;
	}
	public void setSent(boolean sent) {
		this.sent = sent;
	}
	
}
