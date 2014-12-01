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
		log("generating " + numNewTasks + "new tasks");
		List<ExplorationTask> tasks = new LinkedList<ExplorationTask>(tour);
		tasks.addAll(generateRandomTasks(numNewTasks));
		removeUnwantedTasks(tasks);
		// order tasks into tour
		this.tour = Tour.greedy(tasks, getID(), model);
		// set current goal, then auction the rest
		this.currentTask = tour.poll();
		createAuctions(tour);
	}
	
	public Queue<String> getMarketMessages() {
		return marketMessages;
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
		sb.append(idstr.substring(idstr.length()-1-2, idstr.length()-1));
		// ...and time...
		sb.append(" (t="); sb.append(this.time); sb.append(")");
		sb.append(": ");
		// ...then message:
		sb.append(s);
		System.out.println(sb.toString());
	}
	
	// Keep track of the time
	public int tick(int time) {
		this.time = time;
		return this.time;
	}

	public void handleBid(AKSpeak cmd) {
    	Bid bid = Bid.fromMessage(cmd);
    	for (Auction a : auctions) {

			if (a.item.equals(bid.item)) {
				log("Received bid on " + a.toString() + ". Bidder: " + bid.bidder.toString());
				a.addBid(bid);
				if(a.numBids() >= a.expectedNumBids()) {
					a.close();
				}
				break;
			}
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
		
		log("generated tasks: " + list.toString());
		return list;
	}

	public int cost(ExplorationTask task) {
		return model.getDistance(me().getPosition(), task.goal);
	}

	public AuctionOpening openAuction(Auction au) {
		auctions.add(au);
		log("Opening auction: " + au.toString());
		AuctionOpening opening = au.open();
		broadcast(opening);
		return opening;
	}

	public void placeBid(AuctionOpening ao, int price) {
		Bid bid = new Bid(getID(), ao.item, price);
		log("Bidded on "+ ao.toString() + ". Bid=" + price + " reserve=" + ao.reservePrice);
		broadcast(bid);
	}

	public void broadcast(Bid bid) {
		marketMessages.add(bid.toMessageString());
	}

	public void broadcast(AuctionOpening opening) {
		marketMessages.add(opening.toMessageString());
	}
	
	public AKSpeak parseCommand(Command next) {
		return ((AKSpeak)next);
	}

    public boolean isAuctionOpening(String msg) {
    	return msg.startsWith("ao:");
	}

	public void handleAuctionOpening(AKSpeak cmd) {
		AuctionOpening ao = AuctionOpening.fromMessage(cmd);
		int cost = cost(ao.item);
		if (cost < ao.reservePrice) {
			placeBid(ao, cost);
		} else {
			log("Did not place bid. Cost=" + cost + " reserve=" + ao.reservePrice);
		}
		
	}

	public boolean reachedGoal(EntityID goal) {
		// TODO  what condition to use?
//		return me().getPosition().equals(goal);
		return (model.getDistance(getID(), goal) < 1000);
	}

	public boolean isBid(String msg) {
		return msg.startsWith("bi:");
	}
	
	private void createAuctions(List<ExplorationTask> tasks) {
		for (ExplorationTask task : tasks) {
			int cost = cost(task);
			openAuction(new Auction(getID(), task, cost, 99999)); // TODO limit num.
		}
	}

    private void removeUnwantedTasks(Collection<ExplorationTask> tasks) {
    	// TODO waiting for ambulance center impl. 
	}
	
	
	
}