package exploration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;

public class MarketComponent {
	public static final int MARKET_CHANNEL = 2;
	private static final int MAX_TASKS = 4;
	private static final boolean ALWAYS_PLACE_BID = true;
	private static final int EXPECTED_NUM_BIDS = 3;
	private List<Auction> auctions = new LinkedList<Auction>();
	private Queue<String> marketMessages = new LinkedList<String>();
	private LinkedList<ExplorationTask> tour = new LinkedList<ExplorationTask>();
	private ExplorationTask currentTask;
	private StandardWorldModel model;
	private AmbulanceTeam ambulanceTeam;
	private int time;

	public MarketComponent(AmbulanceTeam ambulanceTeam, StandardWorldModel model) {
		this.ambulanceTeam = ambulanceTeam;
		this.model = model;
	}

	public void init() {
		onGoal(); // do whatever we would have done at a subgoal
	}
	
	private AmbulanceTeam me() {
		return ambulanceTeam;
	}
	
	private EntityID getID() {
		return me().getID();
	}

	public void onGoal() {
		// generate new tasks, add current tasks
		int numNewTasks = MAX_TASKS - tour.size();
		String onGoalString =
				(currentTask == null ? "" : "On goal " + currentTask.toString() + ". ");
		log(onGoalString + "Generating " + numNewTasks + " new tasks");
		List<ExplorationTask> tasks = new LinkedList<ExplorationTask>(tour);
		tasks.addAll(generateRandomTasks(numNewTasks));
		removeUnwantedTasks(tasks);
		// order tasks into tour
		addToTour(tasks);
		// set current goal, then auction the rest
		this.currentTask = tour.poll();
		createAuctions(tour);
	}
	
	private void addToTour(ExplorationTask task) {
		List<ExplorationTask> list = new ArrayList<ExplorationTask>();
		list.add(task);
		this.tour = Tour.greedy(list , getID(), model);
	}
	
	private void addToTour(List<ExplorationTask> tasks) {
		this.tour = Tour.greedy(tasks , getID(), model);
	}
	
	public String nextMessage() {
		return marketMessages.poll();
	}
	
	public boolean hasMessage() {
		return !marketMessages.isEmpty();
	}

	public ExplorationTask getCurrentTask() {
		return currentTask;
	}

	public void log(String s) {
		StringBuilder sb = new StringBuilder();
		// Prefix with agent id...
		sb.append("ME_");
		final String idstr = this.getID().toString();
		sb.append(idstr.substring(0, 2));
		sb.append("-");
		sb.append(idstr.substring(idstr.length()-2, idstr.length()));
		// ...and time...
		sb.append(" (t="); sb.append(this.time); sb.append(")");
		sb.append(": ");
		// ...then message:
		sb.append(s);
		System.out.println(sb.toString());
	}
	
	public void tick(int time) {
		this.time = time;
	}
	
	private void sell(AuctionClosing closing) {
		if (closing.winner.equals(this.getID())) {
			// TODO do nothing?
			log("Selling to self: " + closing.toString());
		}
		else {
			log("Selling to other: " + closing.toString());
			broadcast(closing);
		}
	}

	public List<ExplorationTask> generateRandomTasks(int num) {
		if (num == 0) return new ArrayList<ExplorationTask>();

		// Get all buidlings and roads
		final List<StandardEntity> candidates = new LinkedList<StandardEntity>
		(model.getEntitiesOfType(StandardEntityURN.BUILDING, StandardEntityURN.ROAD));
		
		final List<ExplorationTask> list = new LinkedList<ExplorationTask>();
		final int seed = this.getID().getValue() + Integer.valueOf(this.time).hashCode();
		final Random rnd = new Random(seed); // TODO remove seed
		
		// choose entities randomly
		for (int i = 0; i < num; i++) {
			int r = rnd.nextInt(candidates.size());
			EntityID goal = candidates.get(r).getID();
			list.add(new ExplorationTask(goal));
			candidates.remove(r);
		}
		
//		log("Generated tasks: " + list.toString());
		return list;
	}

	public int cost(ExplorationTask task) {
		return model.getDistance(me().getPosition(), task.goal);
	}

	public AuctionOpening openAuction(Auction au) {
		auctions.add(au);
//		log("Opening auction: " + au.toString());
		AuctionOpening opening = au.open();
		broadcast(opening);
		return opening;
	}

	public void placeBid(AuctionOpening ao, int price) {
		Bid bid = new Bid(getID(), ao.item, price);
//		log("Bidded on "+ ao.toString() + ". Bid=" + price + " reserve=" + ao.reservePrice);
		broadcast(bid);
	}

	private void broadcast(Bid bid) {
		marketMessages.add(bid.toMessageString());
	}

	private void broadcast(AuctionOpening opening) {
		marketMessages.add(opening.toMessageString());
	}


	private void broadcast(AuctionClosing closing) {
		marketMessages.add(closing.toMessageString());
	}
	
	public AKSpeak parseCommand(Command next) {
		return ((AKSpeak)next);
	}

	public void handleBid(AKSpeak cmd) {
    	Bid bid = Bid.fromMessage(cmd);
    	for (Auction a : auctions) {
			if (a.item.equals(bid.item)) {
				// Found auction.
				// Add bid, (close auction), then return
				
//				log("Received bid on " + a.toString() + ". Bidder: " + bid.bidder.toString());
				a.addBid(bid);
				if(a.numBids() >= a.expectedNumBids()) {
					AuctionClosing closing = a.close();
					sell(closing);
					auctions.remove(a);
				}
				return;
			}
		}
	}

	public void handleAuctionOpening(AKSpeak cmd) {
		AuctionOpening ao = AuctionOpening.fromMessage(cmd);
		int cost = cost(ao.item);
		if (ALWAYS_PLACE_BID || cost < ao.reservePrice) {
			placeBid(ao, cost);
		} else {
//			log("Did not place bid. Cost=" + cost + " reserve=" + ao.reservePrice);
		}
	}
	
	public void handleAuctionClosing(AKSpeak cmd) {
		AuctionClosing ac = AuctionClosing.fromMessage(cmd);
		log("Check: " + ac.toString() + " ==? " + getID());
		if (ac.winner.equals(this.getID())) return; // someone elses bid
		addToTour(ac.item);
		log("Won auction ->" + ac.item.goal.toString() + ". Added to tour.");
	}

	public boolean reachedGoal(EntityID goal) {
		// TODO  what condition to use?
//		return me().getPosition().equals(goal);
		return (model.getDistance(getID(), goal) < 1000);
	}

	public boolean isBid(String msg) {
		return msg.startsWith("bi:");
	}

    public boolean isAuctionOpening(String msg) {
    	return msg.startsWith("ao:");
    }

	public boolean isAuctionClosing(String msg) {
		return msg.startsWith("ac:");
	}
	
	
	private void createAuctions(List<ExplorationTask> tasks) {
		for (ExplorationTask task : tasks) {
			int cost = cost(task);
			openAuction(new Auction(getID(), task, cost, EXPECTED_NUM_BIDS)); // TODO limit num.
		}
	}

    private void removeUnwantedTasks(Collection<ExplorationTask> tasks) {
    	// TODO waiting for ambulance center impl. 
	}
	
	
}