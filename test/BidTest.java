package exploration.test;

import static org.junit.Assert.*;

import org.junit.Test;

import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;
import exploration.AuctionOpening;
import exploration.Bid;
import exploration.ExplorationTask;

public class BidTest {

	@Test
	public void testFromMessage() {
		AKSpeak cmd = new AKSpeak(new EntityID(9), 1, 1, "bi:11:22".getBytes());
		Bid bid = Bid.fromMessage(cmd);
		
		assertEquals(9, bid.bidder.getValue());
		assertEquals(11, bid.item.goal.getValue());
		assertEquals(22, bid.value);
	}
	
	@Test
	public void toMessage() {
		ExplorationTask item = new ExplorationTask(new EntityID(11));
		EntityID bidder = new EntityID(22);
		int value = 33;
		Bid bid = new Bid(bidder, item, value);
		
		assertEquals("bi:11:33", bid.toMessageString());
	}

}
