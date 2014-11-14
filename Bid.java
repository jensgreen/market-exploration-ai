package exploration;

import rescuecore2.worldmodel.EntityID;

public final class Bid implements Comparable<Bid> {
	public final EntityID bidder;
	public final ExplorationTask item;
	public final int value;
	
	public Bid(EntityID bidder, ExplorationTask item, int value) {
		if (bidder == null) throw new IllegalArgumentException("bidder cannot be null");
		if (item == null) throw new IllegalArgumentException("item cannot be null");
		if (value < 0) throw new IllegalArgumentException("Value must be > 0. Was " + value + ".");
		
		this.bidder = bidder;
		this.item = item;
		this.value = value;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Bid) {
			Bid b = (Bid) obj;
			return this.bidder.equals(b.bidder) && this.item.equals(b.item) && this.value == b.value;
		}
		return false;
	}

	@Override
	public int compareTo(Bid o) {
		return this.value - o.value;
	}
	
	
}
