package async_bfs;
/**
 * Team Members:
 * Sujal Patel (ssp150930)
 * Harshil Shah (hxs155030)
 * Sagar Mehta (sam150930)
 * 
 * This is the individual process class which runs asyncBFS algorithm. 
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class Processes implements Runnable {
	// Current process id
	private int ProcessId;
	/*
	 * qMaster -> write READY message to this Q. qRound -> Receive NEXT signal
	 * from Master process in this Q. qIn -> Interprocess Q. qDone -> Root
	 * signals to Master process about completion of converge cast and algorithm
	 * qReadyToSend -> Write in this Queue READY message to let Master know you
	 * want to send the messages on link now.
	 */
	private BlockingQueue<Message> qMaster, qRound, qIn, qDone, qReadyToSend;

	// enum to express current state of neighbor
	private enum State {
		EXPLORE, NACK, DONE;
	}

	// List of all neighbors of current process
	private ArrayList<Edge> edges;
	private int parentID;
	private int rootID;
	private int distanceFromRoot;
	private boolean isRoot, exploreToSend, firstRound, addReadyMsg = false;
	private boolean doneFlag = false;
	// List in which messages to send in next round are populated.
	private List<SendList> sendList = new ArrayList<SendList>();
	// Save the state of the neighbors.
	private HashMap<Integer, State> stateList = new HashMap<Integer, State>();
	// List in which ID's of processes who sent EXPLORE message to this process
	// are stored.
	private ArrayList<Integer> exploreIDs = new ArrayList<Integer>();

	private HashMap<Integer, Integer> lastMessageSentTimer = new HashMap<Integer, Integer>();

	private Random timeToSendMessage = new Random();

	// For debugging purposes
	int roundNo = 0;
	private boolean debugStatements = false;

	public int messageCount;

	// Constructor
	public Processes(int processId) {
		this.ProcessId = processId;
		edges = new ArrayList<Edge>();
		this.debugStatements = false;
	}

	// Process initialization function.
	public void Initialize() {
		this.exploreToSend = false;
		this.parentID = Integer.MIN_VALUE;
		this.firstRound = true;
		this.rootID = MasterProcess.rootProcessID;
		this.messageCount = 0;
		if (this.ProcessId == MasterProcess.rootProcessID) {
			this.distanceFromRoot = 0;
			this.isRoot = true;
		} else {
			this.distanceFromRoot = Integer.MAX_VALUE;
			this.isRoot = false;
		}
	}

	// Function to add message to this processes Interprocess Q
	public void writeToQIn(Message msg) {
		qIn.add(msg);
	}

	// Function to add single edge. For debugging purposes.
	public void addEdge(Edge e) {
		this.edges.add(e);
	}

	// Function to print parent ID
	public void printParentID() {
		System.out.println(this.parentID);
	}

	// getter/setter functions

	public ArrayList<Edge> getEdges() {
		return edges;
	}

	public void setEdges(ArrayList<Edge> edgeList) {
		edges = edgeList;
	}

	public int getDistanceFromRoot() {
		return distanceFromRoot;
	}

	public void setDistanceFromRoot(int distancefromRoot) {
		distanceFromRoot = distancefromRoot;
	}

	public void setQMaster(BlockingQueue<Message> qmaster) {
		qMaster = qmaster;
	}

	public void setQRound(BlockingQueue<Message> qround) {
		qRound = qround;
	}

	public void setQIn(BlockingQueue<Message> qin) {
		qIn = qin;
	}

	public BlockingQueue<Message> getQDone() {
		return qDone;
	}

	public void setQDone(BlockingQueue<Message> qdone) {
		qDone = qdone;
	}

	public BlockingQueue<Message> getQMaster() {
		return qMaster;
	}

	public BlockingQueue<Message> getQRound() {
		return qRound;
	}

	public BlockingQueue<Message> getQIn() {
		return qIn;
	}

	public int getProcessId() {
		return ProcessId;
	}

	public int getParentID() {
		return parentID;
	}

	public void setParentID(int parentid) {
		parentID = parentid;
	}

	public BlockingQueue<Message> getQReadyToSend() {
		return qReadyToSend;
	}

	public void setQReadyToSend(BlockingQueue<Message> qreadyToSend) {
		qReadyToSend = qreadyToSend;
	}

	// Function that runs the core process code
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Initialize();
		while (true) {
			Message message = null;
			try {
				// check for the start of next round
				while (!(qRound.size() > 0))
					;
				if (qRound.peek() != null)
					message = qRound.take();

				if (message.getMessageType() == Message.MessageType.NEXT) {

					roundNo++;
					if (this.debugStatements)
						System.out.println("Round NO.: " + roundNo);

					for (Entry<Integer, Integer> e : this.lastMessageSentTimer.entrySet()) {
						if (e.getValue() > 0) {
							int time = e.getValue();
							time -= 1;
							e.setValue(time);
							lastMessageSentTimer.replace(e.getKey(), time);
						}
					}

					if (sendList.size() > 0) {
						for (int id = 0; id < sendList.size(); id++) {
							Message toSendMsg = sendList.get(id).getMessage();
							int time = toSendMsg.getTimeToSend();
							if (time > 0) {
								time = time - 1;
							}
							toSendMsg.setTimeToSend(time);
							sendList.get(id).setMessage(toSendMsg);
						}
					}

					this.addReadyMsg = false;

					if (this.isRoot && this.firstRound) {
						Processes neighbourProcess;
						Iterator<Edge> Iter = this.edges.iterator();
						while (Iter.hasNext()) {
							Edge E = Iter.next();
							neighbourProcess = E.getNeighbour(this);
							int Distance = 1;
							int tts = (timeToSendMessage.nextInt(15) + 1);
							message = new Message(this.ProcessId, Message.MessageType.EXPLORE, Distance, 'I', tts,
									rootID);
							SendList sl = new SendList();
							sl.setProcess(neighbourProcess);
							sl.setMessage(message);
							sendList.add(sl);
							stateList.put(neighbourProcess.getProcessId(), State.EXPLORE);
							lastMessageSentTimer.put(neighbourProcess.getProcessId(), tts);
							this.doneFlag = false;
						}
						this.firstRound = false;
					} else
						this.firstRound = false;

					this.exploreToSend = false;
					// Receive all incoming messages
					while (qIn.size() > 0) {

						try {
							message = qIn.take();
							this.messageCount += 1;
							// Explore message handler
							if (message.getMessageType() == Message.MessageType.EXPLORE) {
								if (this.debugStatements)
									System.out.println(
											"Eplore RCVD FROM: " + message.getProcessId() + " AT: " + this.ProcessId);
								exploreIDs.add(message.getProcessId());
								if (this.distanceFromRoot > (int) message.getDistance()) {
									this.distanceFromRoot = (int) message.getDistance();
									this.parentID = message.getProcessId();
									this.exploreToSend = true;
									this.doneFlag = false;
								}
							}
							// DONE message handler
							if (message.getMessageType() == Message.MessageType.DONE) {
								int neighborID = message.getProcessId();
								this.stateList.replace(neighborID, State.DONE);

							}
							// NACK message handler
							if (message.getMessageType() == Message.MessageType.NACK) {
								int neighborID = message.getProcessId();
								this.stateList.replace(neighborID, State.NACK);
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					// Now processing messages
					// sending NACK to unhelpful explore messages
					if (this.exploreIDs.size() > 0) {
						for (int id : this.exploreIDs) {
							if (id != this.parentID) {
								int tts;
								if (lastMessageSentTimer.containsKey(id)) {
									tts = timeToSendMessage.nextInt(15) + 1;
									if (tts <= lastMessageSentTimer.get(id))
									{
										tts += (lastMessageSentTimer.get(id) - tts + 1);
									}
									if (this.debugStatements)
										System.out.println("*** NACK 1 **** TTS: " + tts + " TO: " + id + " FROM: "
												+ this.getProcessId());
									lastMessageSentTimer.replace(id, tts);
								} else {
									tts = timeToSendMessage.nextInt(15) + 1;
									if (this.debugStatements)
										System.out.println("*** NACK 2 *** TTS: " + tts + " TO: " + id + " FROM: "
												+ this.getProcessId());
									lastMessageSentTimer.put(id, tts);
								}
								message = new Message(this.ProcessId, Message.MessageType.NACK, Integer.MAX_VALUE, 'N',
										tts, rootID);
								Processes neighbor;
								Iterator<Edge> iter = this.edges.iterator();
								while (iter.hasNext()) {
									Edge e = iter.next();
									neighbor = e.getNeighbour(this);
									if (neighbor.getProcessId() == id) {
										SendList sl = new SendList();
										sl.setProcess(neighbor);
										sl.setMessage(message);
										sendList.add(sl);
									}
								}
							}
						}
					}

					this.exploreIDs.clear();
					if (this.parentID > -1)
						this.exploreIDs.add(this.parentID);
					// Send EXPLORE to neighbors
					if (this.exploreToSend) {
						Processes neighbor;
						Iterator<Edge> iter = this.edges.iterator();
						if (stateList.size() > 0)
							stateList.clear();
						while (iter.hasNext()) {
							Edge e = iter.next();
							int tts;
							int nbr_id = e.getNeighbour(this).getProcessId();
							if (lastMessageSentTimer.containsKey(nbr_id)) {
								tts = timeToSendMessage.nextInt(15) + 1;
								if (tts <= lastMessageSentTimer.get(nbr_id))
								{
									tts += (lastMessageSentTimer.get(nbr_id) - tts + 1);
								}
								if (this.debugStatements)
									System.out.println("** EXPLORE 1 ** TTS: " + tts + " TO: " + nbr_id + " FROM: "
											+ this.getProcessId());
								lastMessageSentTimer.replace(nbr_id, tts);
							} else {
								tts = timeToSendMessage.nextInt(15) + 1;
								if (this.debugStatements)
									System.out.println("**  EXPLORE 2 ** TTS: " + tts + " TO: " + nbr_id + " FROM: "
											+ this.getProcessId());
								lastMessageSentTimer.put(nbr_id, tts);
							}
							message = new Message(this.ProcessId, Message.MessageType.EXPLORE,
									(this.distanceFromRoot + 1), 'E', tts, rootID);
							neighbor = e.getNeighbour(this);
							if (neighbor.getProcessId() != this.parentID
									&& neighbor.getProcessId() != MasterProcess.rootProcessID) {
								SendList sl = new SendList();
								sl.setProcess(neighbor);
								sl.setMessage(message);
								sendList.add(sl);
								stateList.put(neighbor.getProcessId(), State.EXPLORE);
							}
						}
					}
					if (this.debugStatements)
						System.out.println("Process: " + this.ProcessId + " DONE: " + this.doneFlag);
					// send DONE to parent
					if (!doneFlag) {
						doneFlag = false;
						if (this.stateList.size() == 0 && this.parentID != Integer.MIN_VALUE)
							doneFlag = true;
						else {
							for (Entry<Integer, State> e : this.stateList.entrySet()) {
								if ((e.getValue() == State.NACK || e.getValue() == State.DONE)) {
									doneFlag = true;
								} else {
									doneFlag = false;
									break;
								}
							}
						}

						if (doneFlag) {
							if (this.isRoot) {
								message = new Message(this.ProcessId, Message.MessageType.DONE, Integer.MAX_VALUE, 'D');
								qDone.add(message);
							} else {
								Processes ngbhr;
								Iterator<Edge> Iter = this.edges.iterator();
								while (Iter.hasNext()) {
									Edge E = Iter.next();
									ngbhr = E.getNeighbour(this);
									int nbr_id = ngbhr.getProcessId();
									int tts;
									if (lastMessageSentTimer.containsKey(nbr_id)) {
										tts = timeToSendMessage.nextInt(15) + 1;
										if (tts <= lastMessageSentTimer.get(nbr_id))
										{
											tts += (lastMessageSentTimer.get(nbr_id) - tts + 1);
										}
										if (this.debugStatements)
											System.out.println("** DONE 1 ** TTS: " + tts + " TO: " + nbr_id + " FROM: "
													+ this.getProcessId());
										lastMessageSentTimer.replace(nbr_id, tts);
									} else {
										tts = timeToSendMessage.nextInt(15) + 1;
										if (this.debugStatements)
											System.out.println("*** DONE 2 *** TTS: " + tts + " TO: " + nbr_id
													+ " FROM: " + this.getProcessId());
										lastMessageSentTimer.put(nbr_id, tts);
									}
									message = new Message(this.ProcessId, Message.MessageType.DONE, Integer.MIN_VALUE,
											'D', tts, rootID);

									if (ngbhr.getProcessId() == this.parentID) {
										SendList sl = new SendList();
										sl.setProcess(ngbhr);
										sl.setMessage(message);
										sendList.add(sl);
									}
								}
							}
						}
					}

					// Signal 'Ready to send' and wait.
					Message readyToSendMsg = new Message(this.ProcessId, Message.MessageType.READY, Integer.MIN_VALUE,
							'R');
					synchronized (this) {
						qReadyToSend.add(readyToSendMsg);
					}
					while (qReadyToSend.size() != 0)
						;
					// Send all the outgoing messages.
					if (sendList.size() > 0) {
						for (int id = 0; id < sendList.size(); id++) {
							if (sendList.get(id).isSent())
								continue;
							Message toSendMsg = sendList.get(id).getMessage();
							if (toSendMsg.getTimeToSend() == 0) {
								sendList.get(id).getProcess().writeToQIn(toSendMsg);
								sendList.get(id).setSent(true);
								if (this.debugStatements)
									System.out.println("*Round NO.: " + this.roundNo + " To: "
											+ sendList.get(id).getProcess().getProcessId() + " " + toSendMsg.debug()
											+ "\n");
							}
						}
					}
					// Signal READY for next round
					Message readyMSG = new Message(this.ProcessId, Message.MessageType.READY, Integer.MIN_VALUE, 'R');
					synchronized (this) {
						if (!this.addReadyMsg) {
							qMaster.add(readyMSG);
							this.addReadyMsg = true;
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
