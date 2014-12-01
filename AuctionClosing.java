package exploration;

import java.io.UnsupportedEncodingException;

import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;

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
		StringBuilder sb = new StringBuilder("ac:");
		sb.append(item.goal.toString());
		sb.append(":");
		sb.append(winner.getValue());
		return sb.toString();
	}
	
	public static final AuctionClosing fromMessage(AKSpeak msg) {
		String str = null;
		try {
			str = new String(msg.getContent(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Rethrow to detect when and if this fails.
			throw new RuntimeException("Cannot parse message content",e);
		}
		String[] split = str.split(":");
		// split == [String commandID, EntityID int item, EntityID in winner]		

		EntityID itemID = new EntityID(Integer.parseInt(split[1]));
		ExplorationTask item = new ExplorationTask(itemID);
		
		EntityID winnerID = new EntityID(Integer.parseInt(split[2]));		
		
		return new AuctionClosing(item, winnerID);
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
