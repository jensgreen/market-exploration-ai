package exploration;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.AbstractSampleAgent;
import sample.SampleSearch;
import sun.util.logging.resources.logging;

public class MarketExplorerAmbulanceTeam extends AbstractSampleAgent<AmbulanceTeam> {
	public static enum Behavior { EXPLORING, RESCUEING };
	
	private MarketComponent market;
	private Behavior behavior;

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
		market.updateWorld(changed);
//    	log("thinking...");
    	
    	if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            sendSubscribe(time, MarketComponent.MARKET_CHANNEL);
        }
    	
    	// Send ONE market message if there are any waiting.
    	while (market.hasMessage()) {
    		String msg = market.nextMessage();
    		market.log("sending: \"" + msg + "\"");
			sendSpeak(time, MarketComponent.MARKET_CHANNEL, msg.getBytes());
		}
    	
		reportCivilian(changed, time);

    	if (market.reachedGoal(market.getCurrentTask().goal)) {
    		market.onGoal();
    		String position = "c:" + me().getPosition().getValue();
    		sendSpeak(time, 1, position.getBytes());
    	}
    	else {
            EntityID pos = me().getPosition();
            List<EntityID> path = search.
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
			
			if (msg.startsWith("ci:"))
				market.log("heard:" + msg);
			
            if (market.isBid(msg)) {
            	market.handleBid(cmd);
            } else if (market.isAuctionOpening(msg)) {
            	market.handleAuctionOpening(cmd);
            } else if (market.isAuctionClosing(msg)) {
            	market.handleAuctionClosing(cmd);
            }
        }
    }


	private void reportCivilian(ChangeSet changed, int time) {
		for (EntityID id : changed.getChangedEntities()) {
			StandardEntity entity = model.getEntity(id);
			if (entity instanceof Civilian) {
				Civilian civ = (Civilian) entity;
				
				if (!civilianInRefuge(civ) && (civ.getBuriedness() > 0 || civ.getDamage() > 0)) {
					String msg = "ci:" + civ.getPosition().getValue();
					sendSpeak(time, 1, msg.getBytes());
				}
			}
		}
	}

	private boolean civilianInRefuge(Civilian civ) {
		for (Refuge refuge : getRefuges()) {
			if (civ.getPosition().equals(refuge.getID())) {
				return true;
			}
		}
		return false;
	}

	@Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
    }

}
