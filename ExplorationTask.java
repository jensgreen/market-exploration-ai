package exploration;

import rescuecore2.worldmodel.EntityID;

public final class ExplorationTask {
	
	public final EntityID goal;
	
	public ExplorationTask(EntityID goal) {
		if (goal == null) throw new IllegalArgumentException("goal cannot be null");
		this.goal = goal;
		
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ExplorationTask) {
			ExplorationTask t = (ExplorationTask) obj;
			return this.goal.equals(t.goal);
		}
		return false;
	}
}
