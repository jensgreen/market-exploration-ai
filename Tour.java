package exploration;

import java.util.LinkedList;
import java.util.List;

import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public final class Tour {
	
	private Tour() {} // no instances
	
	/** Construct a Hamilton path visiting all nodes using a greedy heuristic in O(n^2). */
	public static LinkedList<ExplorationTask> greedy(List<ExplorationTask> nodes, EntityID start, StandardWorldModel world) {
		LinkedList<ExplorationTask> tour = new LinkedList<ExplorationTask>();
		if (nodes.size() == 0) return tour;
		LinkedList<ExplorationTask> unvisited = new LinkedList<ExplorationTask>(nodes);
		
		// Find first node -- special case
		ExplorationTask bestTask = null;
		int bestCost = Integer.MAX_VALUE;
		for (ExplorationTask candidate : unvisited) {
			int cost = world.getDistance(start, candidate.goal);
			if (cost < bestCost) {
				bestTask = candidate;
				bestCost = cost;
			}
		}
		tour.add(bestTask);
		unvisited.remove(bestTask);
		
		
		// Find remaining nodes
		while (!unvisited.isEmpty()) {
			
			// greedily assign shortest addition.
			for (ExplorationTask candidate : unvisited) {
				int cost = world.getDistance(tour.getLast().goal, candidate.goal);
				if (cost < bestCost) {
					bestTask = candidate;
					bestCost = cost;
				}
			}
			tour.add(bestTask);
			unvisited.remove(bestTask);
		}
		
		return tour;
	}

}
