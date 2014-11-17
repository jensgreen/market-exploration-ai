package exploration;

import java.util.List;

public interface MarketExploring {
    public List<ExplorationTask> generateRandomTasks(int num);
    public int cost(ExplorationTask goal);
    public void broadcast(Bid bid);
    public void broadcast(AuctionOpening opening);
	public AuctionOpening openAuction(ExplorationTask item, int reservePrice, int expectedNumBids);
	public void placeBid(AuctionOpening ao, int bid);

}
