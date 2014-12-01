package exploration;

import java.io.UnsupportedEncodingException;

import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;

//Message string: "ao:<item>:<reserve>"
public final class AuctionOpening {
	public final ExplorationTask item;
	public final EntityID auctioneer;
	public final int reservePrice;
	
	public AuctionOpening(EntityID auctioneer, ExplorationTask item, int reservePrice) {
		if (item == null) throw new IllegalArgumentException("item cannot be null");
		if (auctioneer == null) throw new IllegalArgumentException("auctioneer cannot be null");
		if (reservePrice < 0) throw new IllegalArgumentException("reservePrive cannot be < 0");

		this.item = item;
		this.reservePrice = reservePrice;
		this.auctioneer = auctioneer;
	}
	
	public String toMessageString() {
		StringBuilder sb = new StringBuilder("ao:");
		sb.append(item.goal.toString());
		sb.append(":");
		sb.append(reservePrice);
		return sb.toString();
	}
	
	public static final AuctionOpening fromMessage(AKSpeak msg) {
		String str = null;
		try {
			str = new String(msg.getContent(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Rethrow to detect when and if this fails.
			throw new RuntimeException("Cannot parse message content",e);
		}
		String[] split = str.split(":");
		// split == [String commandID, EntityID int item, int value]		

		EntityID auctioneer = msg.getAgentID();
		EntityID itemID = new EntityID(Integer.parseInt(split[1]));
		ExplorationTask item = new ExplorationTask(itemID);
		int reservePrice = Integer.parseInt(split[2]);
		
		return new AuctionOpening(auctioneer, item, reservePrice);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AuctionOpening) {
			AuctionOpening ao = (AuctionOpening) obj;
			return this.item.equals(ao.item) && this.reservePrice == ao.reservePrice && this.auctioneer.equals(ao.auctioneer);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "<AuctionOpening: @" + auctioneer.toString() + ", ->" + item.goal.toString() + ", $" + reservePrice + ">";
	}
}
