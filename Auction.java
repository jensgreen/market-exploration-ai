package exploration;

import java.util.PriorityQueue;

import rescuecore2.worldmodel.EntityID;

public class Auction {
	
	public final ExplorationTask item;
	
	// Bids are sorted by value: highest --> lowest
	public final PriorityQueue<Bid> bids = new PriorityQueue<Bid>();
	private final int expectedNumBids;
	private final int reservePrice;
	private final EntityID auctioneer;
	
	public Auction(EntityID auctioneer, ExplorationTask item, int reservePrice, int expectedBids) {
		if (item == null) throw new IllegalArgumentException("item cannot be null");
		if (auctioneer == null) throw new IllegalArgumentException("auctioneer cannot be null");
		if (reservePrice < 0) throw new IllegalArgumentException("reservePrive cannot be < 0");

		this.item = item;
		this.auctioneer = auctioneer;
		this.reservePrice = reservePrice;
		this.expectedNumBids = expectedBids;
	}
	
	public void addBid(Bid bid) {
		this.bids.add(bid);
	}
	
	public AuctionOpening open() {
		return new AuctionOpening(auctioneer, item, reservePrice);
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
	
	public String toString() {
		return "<Auction: @" + auctioneer.toString() + ", ->" + item.goal.toString() + ", $" + reservePrice + ">";
	}
}
