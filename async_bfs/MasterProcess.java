package async_bfs;
/*
 * Team Members:
 * Tanu Rampal (txr180007)
 * Viraj Mavani (vdm180000)
 * This class is the main class. It acts as master thread that synchronizes rounds.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class MasterProcess {

	//for debug purposes.
	private int masterProcessId;
	
	//masterQ -> The Q in which processes signal master they are ready for next round.
	//doneQ -> The Q with 1 capacity where in Root process with write Done message once converge cast is complete on Root.
	private BlockingQueue<Message> masterQ, doneQ;
	
	//readyToSendQ -> The Q to synchronize the sending of messages between processes so all the processes are at same level of execution of round.
	private BlockingQueue<Message> readyToSendQ;
	
	//number of processes.
	private int numProcesses;
	
	//The ID of the root process.
	public static int rootProcessID;
	
	//for debugging purposes.
	int roundNo = 0;
	
	//To break off the while loop emulating the continuous run of system.
	boolean algorithmCompleted = false;
	
	// To send the NEXT message to all the processes.
	private ArrayList<BlockingQueue<Message>> processRoundQ = new ArrayList<BlockingQueue<Message>>();
	
	// Input Q of processes to which other processes can write.
	private ArrayList<BlockingQueue<Message>> interProcessQ = new ArrayList<BlockingQueue<Message>>();
	
	//Constructor
	public MasterProcess(int MProcessId, int[] ProcessIds) {
		this.masterProcessId = MProcessId;
		this.numProcesses = ProcessIds.length;

		masterQ = new ArrayBlockingQueue<>(numProcesses);
		doneQ = new ArrayBlockingQueue<>(5);
		readyToSendQ = new ArrayBlockingQueue<>(numProcesses);

		Message readyMessage;
		BlockingQueue<Message> processRQ, interProcessQueue;
		for (int i = 0; i < numProcesses; i++) {
			readyMessage = new Message(ProcessIds[i], Message.MessageType.READY, Integer.MIN_VALUE, 'X');
			masterQ.add(readyMessage);
			// Will only hold one NEXT message from master thread, but extra capacity of unseen contingencies.
			processRQ = new ArrayBlockingQueue<>(5);
			// Expecting there will be only one message per process to the each process. Extra capacity to avoid Queue full error.
			interProcessQueue = new ArrayBlockingQueue<>(numProcesses*2);
			processRoundQ.add(processRQ);
			interProcessQ.add(interProcessQueue);
		}
	}

	public boolean checkAllProcessesReady() {
		if (masterQ.size() < numProcesses) {
			return false;
		}
		int count = 0;
		Message message;
		for (int i = 0; i < numProcesses; i++) {
			try {
				message = masterQ.take();
				if (message.getMessageType() != Message.MessageType.READY) {
					return false;
				}
				if (message.getMessageType() == Message.MessageType.READY) {
					count++;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (count == numProcesses) {
			return true;
		}
		return false;
	}
	
	public void startNewRound() {
		Iterator<BlockingQueue<Message>> iter = processRoundQ.iterator();
		BlockingQueue<Message> q;
		Message msg;
		masterQ.clear();
		//Write the next message to appropriate queue of processes.
		synchronized (this) {
			while (iter.hasNext()) {
				q = iter.next();
				q.clear();
				msg = new Message(masterProcessId, Message.MessageType.NEXT, Integer.MIN_VALUE, 'X');
				q.add(msg);
			}
		}
	}

	public BlockingQueue<Message> getMasterQ() {
		return masterQ;
	}

	public BlockingQueue<Message> getDoneQ() {
		return doneQ;
	}
	
	public BlockingQueue<Message> getReadyToSendQ() {
		return readyToSendQ;
	}

	public ArrayList<BlockingQueue<Message>> getProcessRoundQ() {
		return processRoundQ;
	}

	public ArrayList<BlockingQueue<Message>> getInterProcessQ() {
		return interProcessQ;
	}

	public boolean isAlgorithmCompleted() {
		if (checkRootProcessDone())
		{
			System.out.println("************ Algorithm Completed *****************");
			algorithmCompleted = true;
		}
		return algorithmCompleted;
	}
	
	public boolean checkRootProcessDone() {
		if (doneQ.size() > 0){
			try {
				Message msg = doneQ.take();
				if(msg.getProcessId() == rootProcessID)
					return true;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public boolean checkAllReadyToSend() {
		if(readyToSendQ.size() == numProcesses)
			return true;
		return false;
	}
	
	public void signalSend(){
		synchronized(this){
			readyToSendQ.clear();
		}
	}
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		HashMap<Integer,ArrayList<Integer>> outputList= new HashMap<Integer,ArrayList<Integer>>();
		BufferedReader inputReader = null;
		try {
			if (args.length > 0 && args != null) {
				inputReader = new BufferedReader(new FileReader(new File(args[0])));
			} else {
				inputReader = new BufferedReader(new FileReader(new File("new_input.txt")));
			}

			// Parse the input file. Order of expected input is number of nodes, node ids, leader id, edge weight matrix.
			int n = -1, rootId = -1;
			String s = inputReader.readLine();
			ArrayList<String> input = new ArrayList<>();
			while (s != null) {
				input.add(s);
				s = inputReader.readLine();
			}

			// Saving number of nodes/processes
			n = new Integer(input.get(0));
			
			// Saving Node/Process Id
			int[] ids = new int[n];			
			for (int i = 0; i < n; i++) {
				ids[i] = i;
			}
			
			// Saving leader Id
			rootId = new Integer(input.get(1));
			
			// Saving Edge weights
			int numEdges = 0;
			String[][] neighbours = new String[n][n];
			for (int i = 2; i < n + 2; i++) {
				neighbours[i - 2] = input.get(i).trim().split(", ");
				for ( String e : neighbours[i - 2] ) {
					if ( e.equals("1") ) {
						numEdges += 1;
					}
				}
			}

			// As bidrectional, we divide by 2
			numEdges /= 2;
			
			// Setup the processes.
			int MasterProcessID = -1;
			MasterProcess mp = new MasterProcess(MasterProcessID, ids);
			if (n != -1)
				mp.numProcesses = n;
			if (rootId != -1)
				MasterProcess.rootProcessID = rootId;
			Processes[] process = new Processes[n];

			for (int i = 0; i < n; i++) {
				process[i] = new Processes(ids[i]);
			}

			for (int i = 0; i < n; i++) {
				ArrayList<Edge> neighbourEdges = new ArrayList<Edge>();
				for (int j = 0; j < n; j++) {
					if (!neighbours[i][j].equalsIgnoreCase("0")) {
						Edge e = new Edge(process[i], process[j], Integer.parseInt(neighbours[i][j]));
						neighbourEdges.add(e);
					}
				}
				process[i].setEdges(neighbourEdges);

			}

			for (int i = 0; i < n; i++) {
				process[i].setQIn(mp.getInterProcessQ().get(i));
				process[i].setQRound(mp.getProcessRoundQ().get(i));

			}
			// Create process threads and get them started.
			Thread[] T = new Thread[n];
			for (int i = 0; i < n; i++) {
				process[i].setQMaster(mp.getMasterQ());
				process[i].setQDone(mp.getDoneQ());
				process[i].setQReadyToSend(mp.getReadyToSendQ());
				T[i] = new Thread(process[i]);
				T[i].start();
			}
			// Continuous running of the algorithm until shortest path tree is not built.
			while (!mp.isAlgorithmCompleted()) {
				if (mp.checkAllProcessesReady()) {
					mp.startNewRound();
					mp.roundNo++;
					while(!mp.checkAllReadyToSend());
					mp.signalSend();
				}
			}
			// Termination step.
			for (int i = 0; i < n; i++) {
				T[i].stop();
			}
			for (int i = 0; i < T.length; i++) {
				try {
					T[i].join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			int totalMessages = 0;
			System.out.println("Average messages sent per Edge: " + String.valueOf((float) totalMessages / numEdges));
			// Printing adjacency list
			System.out.println("******************* Adjacency List *******************");
			for (Map.Entry<Integer, ArrayList<Integer>> entry : outputList.entrySet()) {
				if(entry.getKey()>=0){
					System.out.print(entry.getKey());
				    for(Integer i: entry.getValue()){
				    	System.out.print( " -> "+ i);
				    }
				    System.out.println();
				}
			    
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
