package exploration;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.AbstractSampleAgent;
import exploration.Auction;
import exploration.AuctionOpening;
import exploration.Bid;
import exploration.ExplorationTask;
import exploration.MarketExploring;

public class MarketExplorerAmbulanceTeam extends AbstractSampleAgent<AmbulanceTeam> implements MarketExploring {
	private static final int MAX_TASK_QUEUE_LENGTH = 4;
	private static final int MARKET_CHANNEL = 2;
	
	private final List<ExplorationTask> tasks = new LinkedList<ExplorationTask>();
	private final List<Auction> ownAuctions = new LinkedList<Auction>();
	private final Queue<String> marketMessages = new LinkedList<String>();
	private LinkedList<ExplorationTask> tour;
	private int time; // save time so we can log it in log()
	
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

    @Override
    public String toString() {
        return "Market explorer ambulance team";
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        log("Connected");
        model.indexClass(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.REFUGE,StandardEntityURN.HYDRANT,StandardEntityURN.GAS_STATION, StandardEntityURN.BUILDING);
        init();
    }

	private void init() {
		tasks.addAll(generateRandomTasks(3)); //TODO set number
		removeUnwantedTasks(tasks);
		createAndBroadcastAuctions(tasks);
		tour = Tour.greedy(tasks, getID(), model);
	}

	private void createAndBroadcastAuctions(List<ExplorationTask> tasks) {
		for (ExplorationTask task : tasks) {
			int cost = cost(task);
			broadcast(openAuction(new Auction(getID(), task, cost, 99999))); // TODO limit num.
		}
	}

    private void removeUnwantedTasks(Collection<ExplorationTask> tasks) {
    	// TODO waiting for ambulance center impl. 
	}

	@Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		this.time = time;
    	log("thinking...");
    	
    	if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            sendSubscribe(time, MARKET_CHANNEL);
        }
    	
    	// Send ONE market message if there are any waiting.
    	if (!marketMessages.isEmpty()) {
    		String msg = marketMessages.remove();
    		log("sending + \"" + msg + "\"");
			sendSpeak(time, MARKET_CHANNEL, msg.getBytes());
		}

    	if (reachedGoal(tour.peek().goal)) {
    		log("reached goal " + tour.peek());
    		tour.poll();
    		updateTasksAndTour(tasks, tour);
    	}
    	
        for (Command next : heard) {
			AKSpeak cmd = parseCommand(next);
			String msg = null;
			try {
				msg = new String(cmd.getContent(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// Rethrow to detect when and if this fails.
				throw new RuntimeException("Cannot parse message content",e);
			}
        	log("heard: " + msg);
			
            if (isBid(msg)) {
            	handleBid(cmd);
            } else if (isAuctionOpening(msg)) {
            	handleAuctionOpening(cmd);
            }
        }
        
        
        
        
    }

	private AKSpeak parseCommand(Command next) {
		return ((AKSpeak)next);
	}

    private boolean isAuctionOpening(String msg) {
    	return msg.startsWith("ao:");
	}

	private void handleAuctionOpening(AKSpeak cmd) {
		AuctionOpening ao = AuctionOpening.fromMessage(cmd);
		int cost = cost(ao.item);
		if (cost < ao.reservePrice) {
			placeBid(ao, cost);
		} else {
			log("Did not place bid. Cost=" + cost + " reserve=" + ao.reservePrice);
		}
		
	}

	private void updateTasksAndTour(List<ExplorationTask> tasks, LinkedList<ExplorationTask> tour) {
		int numNewTasks = MAX_TASK_QUEUE_LENGTH - tasks.size();
		List<ExplorationTask> newTasks = generateRandomTasks(numNewTasks);
		createAndBroadcastAuctions(newTasks);
		this.tasks.addAll(newTasks);
		this.tour = Tour.greedy(tasks, getID(), model);
	}

	private boolean reachedGoal(EntityID goal) {
		return (model.getDistance(getID(), goal) < 200);
	}

	private void handleBid(AKSpeak cmd) {
    	Bid bid = Bid.fromMessage(cmd);
    	for (Auction a : ownAuctions) {

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

	private boolean isBid(String msg) {
		return msg.startsWith("bi:");
	}

	@Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
    }

	@Override
	public List<ExplorationTask> generateRandomTasks(int num) {
		if (num == 0) return new ArrayList<ExplorationTask>();
		
		// Get all buidlings and roads
		final List<StandardEntity> candidates = new LinkedList<StandardEntity>
		(model.getEntitiesOfType(StandardEntityURN.BUILDING, StandardEntityURN.ROAD));
		
		final List<ExplorationTask> list = new LinkedList<ExplorationTask>();
		final Random rnd = new Random(this.getID().getValue()); // TODO remove seed
		
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

	@Override
	public int cost(ExplorationTask task) {
		return model.getDistance(me().getPosition(), task.goal);
	}

	@Override
	public AuctionOpening openAuction(Auction au) {
		ownAuctions.add(au);
		log("Opening auction: " + au.toString());
		AuctionOpening opening = au.open();
		broadcast(opening);
		return opening;
	}

	@Override
	public void placeBid(AuctionOpening ao, int price) {
		Bid bid = new Bid(getID(), ao.item, price);
		log("Bidded on "+ ao.toString() + ". Bid=" + price + " reserve=" + ao.reservePrice);
		broadcast(bid);
	}

	@Override
	public void broadcast(Bid bid) {
		marketMessages.add(bid.toMessageString());
	}

	@Override
	public void broadcast(AuctionOpening opening) {
		marketMessages.add(opening.toMessageString());
	}

}
