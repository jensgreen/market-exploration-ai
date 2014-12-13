package exploration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import navigation.NavigationModule;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import sample.SampleSearch;

public final class Tour {
	private static final boolean USE_BFS_PATH_COST = true;
	private final LinkedList<ExplorationTask> nodes;
	private final int[] costs;
	private NavigationModule nav;
	
	private Tour(LinkedList<ExplorationTask> nodes, int[] costs, NavigationModule nav) {
		this.nodes = nodes;
		this.costs = costs;
		this.nav = nav;
	}

	public LinkedList<ExplorationTask> getNodes() {
		return nodes;
	}

	public int getCosts(int index) {
		return costs[index];
	}
	
	/** Cost of addition to greedily insert node into this Tour instance. */
	public int costToAdd(EntityID node, StandardWorldModel world, SampleSearch bfs) {
		int bestCost = Integer.MAX_VALUE;
		
		for (int i = 0; i < nodes.size(); i++) {
			int cost = Tour.cost(nodes.get(i).goal, node, nav, world, bfs);
			if (i < nodes.size() - 1) cost += Tour.cost(node, nodes.get(i+1).goal, nav, world, bfs);
			if (cost < bestCost) {
				bestCost = cost;
			}
		}
		
		for (ExplorationTask task : this.nodes) {
			int cost = Tour.cost(task.goal, node, nav, world, bfs); // TODO should be cost of (old) --> (new) --> (old+1)
			if (cost < bestCost) {
				bestCost = cost;
			}
		}
		return bestCost;
	}
	
	/** Construct a Hamilton path visiting all nodes using a greedy heuristic in O(n^2). */
	public static Tour greedy(List<ExplorationTask> nodes, EntityID start, StandardWorldModel world, NavigationModule nav, SampleSearch bfs) {
		if (nodes.size() == 0) {
			return Tour.empty(nav);
		}
		
		LinkedList<ExplorationTask> tasks = new LinkedList<ExplorationTask>();
		LinkedList<ExplorationTask> unvisited = new LinkedList<ExplorationTask>(nodes);
		
		// Find first node -- special case
		Tour.greedyAdd(start, tasks, unvisited, world, nav, bfs);
		// Find remaining nodes
		while (!unvisited.isEmpty()) {
			greedyAdd(tasks.getLast().goal, tasks, unvisited, world, nav, bfs);
		}
		
		Tour tour = new Tour(tasks, Tour.buildCosts(tasks, start, world, nav, bfs), nav); 
		return tour;
	}

	/** Greedily adds best node (from lastNode) from unvisited to tour, remove found node from unvisited.
	 * @return cost of the addition. */ 
	private static int greedyAdd(EntityID lastNode, LinkedList<ExplorationTask> tour,
			LinkedList<ExplorationTask> unvisited, StandardWorldModel world, NavigationModule nav, SampleSearch bfs) {
		ExplorationTask bestTask = null;
		int bestCost = Integer.MAX_VALUE;
		
		// greedily assign shortest addition.
		for (ExplorationTask candidate : unvisited) {
			int cost = Tour.cost(lastNode, candidate.goal, nav, world, bfs);
			if (cost < bestCost) {
				bestTask = candidate;
				bestCost = cost;
			}
		}
		tour.add(bestTask);
		unvisited.remove(bestTask);
		return bestCost;
	}

	private static int cost(EntityID from, EntityID to, NavigationModule nav, StandardWorldModel world, SampleSearch bfs) {
		if (MarketExplorerAmbulanceTeam.USE_CUSTOM_NAV) {
			nav.planPathInSerialMode(from, to);
			return nav.getCost();
			
		}
		else {
			if (USE_BFS_PATH_COST) return bfsCost(from, to, world, bfs);
			else return world.getDistance(from, to);
		}
	}
	
	private static int bfsCost(EntityID from, EntityID to, StandardWorldModel world, SampleSearch bfs) {
		if (from.equals(to)) return 0;
		
		List<EntityID> goalList = new ArrayList<EntityID>();
		goalList.add(to);
		List<EntityID> path = bfs.
				breadthFirstSearch(from, goalList);
		
		if (path == null) return Integer.MAX_VALUE;
		if (path.size() <= 1) return 0;
		
		int sum = 0;
		Iterator<EntityID> iter = path.iterator();

		EntityID prev = iter.next();
		EntityID current = iter.next();
		sum += world.getDistance(prev, current);
		
		while (iter.hasNext()) {
			prev = current;
			current = iter.next();
			sum += world.getDistance(prev, current);
		}
		
		return sum;
	}
	
	private static int[] buildCosts(LinkedList<ExplorationTask> tasks, EntityID start, 
			StandardWorldModel world, NavigationModule nav, SampleSearch bfs) {
		if (tasks.size() <= 0)
			throw new IllegalArgumentException("tasks must have at least one member.");
		int[] costs = new int[tasks.size()];
		
		Iterator<ExplorationTask> iter = tasks.iterator();
		ExplorationTask prev = new ExplorationTask(start); // dummy task for starting position

		costs[0] = Tour.cost(start, iter.next().goal, nav, world, bfs);
		int counter = 1;
		while (iter.hasNext()) {
			ExplorationTask current = iter.next(); // start iterating at node 2
			int cost = Tour.cost(prev.goal, current.goal, nav, world, bfs);
			costs[counter] = cost; // set cost to enter current node
			costs[counter-1] += cost; // add cost to exit prev node
			counter++;
		}
		
		costs[0] -= Tour.cost(start, tasks.get(0).goal, nav, world, bfs);
		for (int j = 1; j < costs.length-1; j++) {
			int dist = Tour.cost(tasks.get(j-1).goal, tasks.get(j+1).goal, nav, world, bfs);

			if (dist > costs[j]) costs[j] = 0; // prevent negative costs.
			else costs[j] -= dist;
		}
		// costs[costs.length-1] -= 0; // do not change last cost
		
		return costs;
	}

	public static Tour empty(NavigationModule nav) {
		return new Tour(new LinkedList<ExplorationTask>(), new int[0], nav);
	}

}
