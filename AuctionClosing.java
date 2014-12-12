package exploration;

import rescuecore2.worldmodel.EntityID;
import sample.CommunicationEncoding;
import sample.MsgReceiver;
import sample.MsgType;

//Message string: "ac:<item>:<winner>"
public final class AuctionClosing {
	public final ExplorationTask item;
	public final EntityID winner;
	
	public AuctionClosing(ExplorationTask item, EntityID winner) {
		if (item == null) throw new IllegalArgumentException("item cannot be null");
		if (winner == null) throw new IllegalArgumentException("winner cannot be null");
		
		this.item = item;
		this.winner = winner;
	}
	
	public AuctionClosing(Bid bid) {
		if (bid == null) throw new IllegalArgumentException("bid cannot be null");
		
		this.item = bid.item;
		this.winner = bid.bidder;
	}
	
	public String toMessageString() {
		StringBuilder sb = new StringBuilder();
		sb.append(MsgReceiver.Ambulance.getInt());
		sb.append("d");
		sb.append(MsgType.FinishAuction.getInt());
		sb.append("d");
		sb.append(item.goal.toString());
		sb.append("d");
		sb.append(winner.getValue());
		sb.append("d");
		return sb.toString();
	}
	
	public static final AuctionClosing fromMessage(int[] split, EntityID winner) {
		EntityID itemID = new EntityID(split[2]);
		ExplorationTask item = new ExplorationTask(itemID);
		return new AuctionClosing(item, winner);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AuctionClosing) {
			AuctionClosing ac = (AuctionClosing) obj;
			return this.item.equals(ac.item) && this.winner.equals(ac.winner);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "<AuctionClosing: @" + winner.toString() + ", ->" + item.goal.toString() +">";
	}
}
