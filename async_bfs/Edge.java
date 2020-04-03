package async_bfs;
/**
 * Team Members:
 * Sujal Patel (ssp150930)
 * Harshil Shah (hxs155030)
 * Sagar Mehta (sam150930)
 *
 * This class is the object for edges. 
 */
public class Edge {
	// End-point processes.
		private Processes P1,P2;
		// Edge weight.
		private int Weight;
		// Constructor.
		public Edge(Processes p1, Processes p2, int weight) {
			this.P1 = p1;
			this.P2 = p2;
			this.Weight = weight;
		}
		// getter/setter functions.
		public Processes getP1() {
			return P1;
		}

		public Processes getP2() {
			return P2;
		}

		public int getWeight() {
			return Weight;
		}
		// Function to get neighbor process object.
		public Processes getNeighbour(Processes P){
			if(P == P1)
				return P2;
			return P1;
		}
}
