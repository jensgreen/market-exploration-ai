package exploration;

import java.io.UnsupportedEncodingException;

import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;

// Message string: "bi:<item>:<value>"
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
	
	@Override
	public String toString() {
		return "<Bid: @" + bidder.toString() + ", ->" + item.goal.toString() + ", $" + value + ">";
	}
	
	public String toMessageString() {
		StringBuilder sb = new StringBuilder("bi:");
		sb.append(item.goal.toString());
		sb.append(":");
		sb.append(value);
		return sb.toString();
	}
	
	public static final Bid fromMessage(AKSpeak msg) {
		String str = null;
		try {
			str = new String(msg.getContent(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Rethrow to detect when and if this fails.
			throw new RuntimeException("Cannot parse message content",e);
		}
		String[] split = str.split(":");
		// split == [String commandID, EntityID int item, int value]
		
		EntityID bidder = msg.getAgentID();
		EntityID itemID = new EntityID(Integer.parseInt(split[1]));
		ExplorationTask item = new ExplorationTask(itemID);
		int value = Integer.parseInt(split[2]);
		return new Bid(bidder, item, value);
	}
	
	
}
