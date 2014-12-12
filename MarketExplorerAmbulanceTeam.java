package exploration;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import navigation.NavigationModule;
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
import sample.CommunicationEncoding;
import sample.MsgReceiver;
import sample.MsgType;
import sample.ObservedType;

public class MarketExplorerAmbulanceTeam extends AbstractSampleAgent<AmbulanceTeam> {
	public static enum Behavior { EXPLORING, RESCUEING };
	
	private MarketComponent market;
	private Behavior behavior;
	private NavigationModule nav;

    @Override
    public String toString() {
        return "Market explorer ambulance team";
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        //TODO look over this list
        model.indexClass(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.REFUGE,StandardEntityURN.HYDRANT,StandardEntityURN.GAS_STATION, StandardEntityURN.BUILDING, StandardEntityURN.ROAD);
        
        new CommunicationEncoding(); // init communication codebook
        nav = new NavigationModule(model);
        market = new MarketComponent(me(), model, nav);
        market.log("Connected. At pos " + me().getPosition());
        market.init();
        nav.planPathTo(me().getPosition(), market.getCurrentTask().goal);
        behavior = Behavior.EXPLORING;
    }

	@Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		market.tick(time);
		market.updateWorld(changed);
//    	log("thinking...");
    	
    	if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            sendSubscribe(time, MarketComponent.MARKET_CHANNEL);
        }
    	
    	// Send ALL market messages
    	while (market.hasMessage()) {
    		String msg = market.nextMessage();
    		msg = new String(msg.getBytes(), StandardCharsets.ISO_8859_1);
    		String clear = CommunicationEncoding.addSecurity(msg);
    		String code =  CommunicationEncoding.clearToCode(clear);
    		market.log("sending: \"" + code + "\"");
			sendSpeak(time, MarketComponent.MARKET_CHANNEL, code.getBytes(StandardCharsets.ISO_8859_1));
		}
    	
		reportCivilian(changed, time);

        EntityID pos = me().getPosition();
		if (behavior == Behavior.EXPLORING) {
	    	if (market.reachedGoal(market.getCurrentTask().goal) && !nav.isPlanningPath()) {
	    		market.onGoal();
	    		String position = "c:" + me().getPosition().getValue();
	    		sendSpeak(time, 1, position.getBytes());
	    		nav.planPathTo(pos, market.getCurrentTask().goal);
	    	}
	    	else if (nav.isPlanReady()) {
				nav.uppdatePath(pos);
//				List<EntityID> path = nav.getPlan();
				List<EntityID> path = search.
						breadthFirstSearch(pos, market.getCurrentTask().goal);
				sendMove(time, path);
	    	}
		}
    	
        for (Command next : heard) {
			AKSpeak cmd = market.parseCommand(next);
			if (cmd.getAgentID().equals(getID())) continue; // skip own messages
			
			String encodedMsg = new String(cmd.getContent(), StandardCharsets.ISO_8859_1);
			String readableMsg = CommunicationEncoding.codeToClear(encodedMsg);
			market.log("enc:" + encodedMsg);
			market.log("dec:" + readableMsg + ";");
			
			if (readableMsg == null || "".equals(readableMsg) || readableMsg.length() == 0) {
				System.err.println("Cannot parse message content.");
				continue;
			}
			
			if (!CommunicationEncoding.isMsgCorrect(readableMsg)) {
				System.err.println("Message security bit error.");
				continue;
			}
			
			String[] msgParts = readableMsg.substring(0, readableMsg.length() - 2).split("d");
			// Message is correct here. Split into msgParts array.
			
			System.err.println("MSGPARTS" + msgParts);
			int[] msgInts = new int[msgParts.length];
			for (int i = 0; i < msgParts.length; i++) {
				msgInts[i] = Integer.parseInt(msgParts[i]);
			}
			
			// TODO !
//			if (readableMsg.startsWith("ci:"))
//				market.log("heard:" + readableMsg);
			
			// handle at all?
			if (market.handleMessage(msgInts[0])) {
				
				EntityID sender = cmd.getAgentID();
				
				// handle corrent msg type
	            if (market.isBid(msgInts[1])) {
	            	market.handleBid(msgInts, sender);
	            } else if (market.isAuctionOpening(msgInts[1])) {
	            	market.handleAuctionOpening(msgInts, sender);
	            } else if (market.isAuctionClosing(msgInts[1])) {
	            	market.handleAuctionClosing(msgInts, sender);
	            }
			}
        }
    }

	private void reportCivilian(ChangeSet changed, int time) {
		for (EntityID id : changed.getChangedEntities()) {
			StandardEntity entity = model.getEntity(id);
			if (entity instanceof Civilian) {
				Civilian civ = (Civilian) entity;
				
				if (!civilianInRefuge(civ) && (civ.getBuriedness() > 0 || civ.getDamage() > 0)) {
					StringBuilder sb = new StringBuilder();
					sb.append(MsgReceiver.Ambulance.getInt());
					sb.append("d");
					sb.append(MsgType.Observation.getInt());
					sb.append("d");
					sb.append(ObservedType.Civilian);
					sb.append("d");
					sb.append(civ.getPosition().getValue());
					sb.append("d");
					String s = CommunicationEncoding.addSecurity(sb.toString());
					String msg =  CommunicationEncoding.clearToCode(s);
					
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
