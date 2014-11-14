package exploration;

import java.util.List;

public interface MarketExploring {
    public List<ExplorationTask> generateRandomTasks(int num);
    public double cost(ExplorationTask goal);
    public void broadcast(Bid bid);
    public void broadcast(Auction auction);
    public void openAuction(ExplorationTask item, int reservePrice);
    public void placeBid(Auction auction, int bid);

}
