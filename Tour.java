package exploration;

import java.util.LinkedList;
import java.util.List;

import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public final class Tour {
	
	private Tour() {} // no instances
	
	/** Construct a Hamilton path visiting all nodes using a greedy heuristic in O(n^2). */
	public static LinkedList<ExplorationTask> greedy(List<ExplorationTask> nodes, EntityID start, StandardWorldModel world) {
		if (nodes.size() == 0) return new LinkedList<ExplorationTask>();
		
		LinkedList<ExplorationTask> tour = new LinkedList<ExplorationTask>();
		LinkedList<ExplorationTask> unvisited = new LinkedList<ExplorationTask>(nodes);
		
		// Find first node -- special case
		greedyAdd(start, tour, unvisited, world);
		// Find remaing nodes
		while (!unvisited.isEmpty()) {
			greedyAdd(tour.getLast().goal, tour, unvisited, world);
		}
		
		return tour;
	}

	/** Greedily adds best node (from lastNode) from unvisited to tour, remove found node from unvisited. */ 
	private static void greedyAdd(EntityID lastNode, LinkedList<ExplorationTask> tour,
			LinkedList<ExplorationTask> unvisited, StandardWorldModel world) {
		ExplorationTask bestTask = null;
		int bestCost = Integer.MAX_VALUE;
		
		// greedily assign shortest addition.
		for (ExplorationTask candidate : unvisited) {
			int cost = world.getDistance(lastNode, candidate.goal);
			if (cost < bestCost) {
				bestTask = candidate;
				bestCost = cost;
			}
		}
		tour.add(bestTask);
		unvisited.remove(bestTask);
	}

}
