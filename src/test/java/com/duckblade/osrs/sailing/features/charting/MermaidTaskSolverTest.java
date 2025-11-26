package com.duckblade.osrs.sailing.features.charting;

import org.junit.Assert;
import org.junit.Test;

public class MermaidTaskSolverTest
{

	@Test
	public void ensureAllTasksHaveMermaidSolutions()
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (!MermaidTaskSolver.MERMAID_IDS.contains(task.getNpcId()))
			{
				continue;
			}

			Assert.assertNotNull(
				String.format("task %d missing solution", task.getTaskId()),
				MermaidTaskSolver.SOLUTIONS.get(task)
			);
		}
	}

	@Test
	public void noNonMermaidsHaveSolutions()
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (MermaidTaskSolver.MERMAID_IDS.contains(task.getNpcId()))
			{
				continue;
			}

			Assert.assertNull(
				String.format("task %d has solution but is not a mermaid task", task.getTaskId()),
				MermaidTaskSolver.SOLUTIONS.get(task)
			);
		}
	}

}
