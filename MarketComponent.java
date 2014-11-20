package exploration;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MarketComponent {
	public static final int MARKET_CHANNEL = 2;
	private final Queue<ExplorationTask> tasks = new LinkedList<ExplorationTask>();
	private final List<Auction> ownAuctions = new LinkedList<Auction>();
	private final Queue<String> marketMessages = new LinkedList<String>();
}
