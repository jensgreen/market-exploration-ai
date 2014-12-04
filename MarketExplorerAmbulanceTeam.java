package exploration;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.AbstractSampleAgent;
import sample.SampleSearch;

public class MarketExplorerAmbulanceTeam extends AbstractSampleAgent<AmbulanceTeam> {
	private MarketComponent market;

    @Override
    public String toString() {
        return "Market explorer ambulance team";
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        //TODO look over this list
        model.indexClass(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.REFUGE,StandardEntityURN.HYDRANT,StandardEntityURN.GAS_STATION, StandardEntityURN.BUILDING, StandardEntityURN.ROAD);
        market = new MarketComponent(me(), model);
        market.log("Connected. At pos " + me().getPosition());
        market.init();
    }

	@Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		market.tick(time);
//    	log("thinking...");
    	
    	if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            sendSubscribe(time, MarketComponent.MARKET_CHANNEL);
        }
    	
    	// Send ONE market message if there are any waiting.
    	if (market.hasMessage()) {
    		String msg = market.nextMessage();
    		market.log("sending: \"" + msg + "\"");
			sendSpeak(time, MarketComponent.MARKET_CHANNEL, msg.getBytes());
		}

    	if (market.reachedGoal(market.getCurrentTask().goal)) {
    		market.onGoal();
    	}
    	else {
            EntityID pos = me().getPosition();
            List<EntityID> path = new SampleSearch(model).
            		breadthFirstSearch(pos, market.getCurrentTask().goal);
			sendMove(time, path);
    	}
    	
        for (Command next : heard) {
			AKSpeak cmd = market.parseCommand(next);
			if (cmd.getAgentID().equals(getID())) continue; // skip own messages
			String msg = null;
			try {
				msg = new String(cmd.getContent(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// Rethrow to detect when and if this fails.
				throw new RuntimeException("Cannot parse message content",e);
			}
			
            if (market.isBid(msg)) {
            	market.handleBid(cmd);
            } else if (market.isAuctionOpening(msg)) {
            	market.handleAuctionOpening(cmd);
            } else if (market.isAuctionClosing(msg)) {
            	market.log("Won auction!");
            	market.handleAuctionClosing(cmd);
            }
        }
    }


	@Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
    }

}
