package exploration;

import java.util.PriorityQueue;

public class Auction {
	
	public final ExplorationTask item;
	
	// Bids are sorted by value: highest --> lowest
	public final PriorityQueue<Bid> bids = new PriorityQueue<Bid>();

	private int expectedNumBids;
	
	public Auction(ExplorationTask item, int expectedBids) {
		if (item == null) throw new IllegalArgumentException("Item cannot be null");
		this.item = item;
		this.expectedNumBids = expectedBids;
	}
	
	public void addBid(Bid bid) {
		this.bids.add(bid);
	}
	
	public void open() {
		// TODO keep?
	}
	
	public void close() {
		// TODO keep?
	}
	
	public Bid getBestBid() {
		return bids.peek();
	}
	
	public int numBids() {
		return bids.size();
	}
	
	public int expectedNumBids() {
		return expectedNumBids;
	}
}
