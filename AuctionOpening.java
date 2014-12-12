package exploration;

import rescuecore2.worldmodel.EntityID;
import sample.CommunicationEncoding;
import sample.MsgReceiver;
import sample.MsgType;

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
		StringBuilder sb = new StringBuilder();
		sb.append(MsgReceiver.Ambulance.getInt());
		sb.append("d");
		sb.append(MsgType.StartAuction.getInt());
		sb.append("d");
		sb.append(item.goal.toString());
		sb.append("d");
		sb.append(reservePrice);
		sb.append("d");
		return sb.toString();
	}
	
	public static final AuctionOpening fromMessage(int[] split, EntityID auctioneer) {
		EntityID itemID = new EntityID(split[2]);
		ExplorationTask item = new ExplorationTask(itemID);
		int reservePrice = split[3];
		
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
