package exploration;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public final class Tour {
	private final LinkedList<ExplorationTask> nodes;
	private final int[] costs;
	
	private Tour(LinkedList<ExplorationTask> nodes, int[] costs) {
		this.nodes = nodes;
		this.costs = costs;
	}

	public LinkedList<ExplorationTask> getNodes() {
		return nodes;
	}

	public int getCosts(int index) {
		return costs[index];
	}
	
	/** Cost of addition to greedily insert node into this Tour instance. */
	public int costToAdd(EntityID node, StandardWorldModel world) {
		int bestCost = Integer.MAX_VALUE;
		
		for (ExplorationTask task : this.nodes) {
			int cost = world.getDistance(task.goal, node);
			if (cost < bestCost) {
				bestCost = cost;
			}
		}
		return bestCost;
	}
	
	/** Construct a Hamilton path visiting all nodes using a greedy heuristic in O(n^2). */
	public static Tour greedy(List<ExplorationTask> nodes, EntityID start, StandardWorldModel world) {
		if (nodes.size() == 0) {
			return Tour.empty();
		}
		
		LinkedList<ExplorationTask> tasks = new LinkedList<ExplorationTask>();
		LinkedList<ExplorationTask> unvisited = new LinkedList<ExplorationTask>(nodes);
		
		// Find first node -- special case
		Tour.greedyAdd(start, tasks, unvisited, world);
		// Find remaing nodes
		while (!unvisited.isEmpty()) {
			greedyAdd(tasks.getLast().goal, tasks, unvisited, world);
		}
		
		Tour tour = new Tour(tasks, Tour.buildCosts(tasks, start, world)); 
		return tour;
	}

	/** Greedily adds best node (from lastNode) from unvisited to tour, remove found node from unvisited.
	 * @return cost of the addition. */ 
	private static int greedyAdd(EntityID lastNode, LinkedList<ExplorationTask> tour,
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
		return bestCost;
	}
	
	private static int[] buildCosts(LinkedList<ExplorationTask> tasks, EntityID start, 
			StandardWorldModel world) {
		if (tasks.size() <= 0)
			throw new IllegalArgumentException("tasks must have at least one member.");
		int[] costs = new int[tasks.size()];
		
		Iterator<ExplorationTask> iter = tasks.iterator();
		ExplorationTask prev = new ExplorationTask(start); // dummy task for starting position

		costs[0] = world.getDistance(start, iter.next().goal); // node 1
		int counter = 1;
		while (iter.hasNext()) {
			ExplorationTask current = iter.next(); // start iterating at node 2
			int cost = world.getDistance(prev.goal, current.goal);
			costs[counter] = cost; // set cost to enter current node
			costs[counter-1] += cost; // add cost to exit prev node
			counter++;
		}
		
		costs[0] -= world.getDistance(start, tasks.get(0).goal);
		for (int j = 1; j < costs.length-1; j++) {
			int dist = world.getDistance(tasks.get(j-1).goal, tasks.get(j+1).goal);

			if (dist > costs[j]) costs[j] = 0; // prevent negative costs.
			else costs[j] -= dist;
		}
		// costs[costs.length-1] -= 0; // do not change last cost
		
		return costs;
	}

	public static Tour empty() {
		return new Tour(new LinkedList<ExplorationTask>(), new int[0]);
	}

}
