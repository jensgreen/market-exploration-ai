package exploration.test;

import static org.junit.Assert.*;

import org.junit.Test;

import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;
import exploration.AuctionOpening;
import exploration.ExplorationTask;

public class AuctionOpeningTest {

	@Test
	public void testFromMessage() {
		AKSpeak cmd = new AKSpeak(new EntityID(9), 1, 1, "ao:1:2".getBytes());
		AuctionOpening ao = AuctionOpening.fromMessage(cmd);
		
		assertEquals(9, ao.auctioneer.getValue());
		assertEquals(1, ao.item.goal.getValue());
		assertEquals(2, ao.reservePrice);
	}
	
	@Test
	public void toMessage() {
		ExplorationTask item = new ExplorationTask(new EntityID(11));
		EntityID auctioneer = new EntityID(22);
		int reservePrice = 33;
		AuctionOpening ao = new AuctionOpening(auctioneer, item, reservePrice);
		
		assertEquals("ao:11:33", ao.toMessageString());
	}

}
