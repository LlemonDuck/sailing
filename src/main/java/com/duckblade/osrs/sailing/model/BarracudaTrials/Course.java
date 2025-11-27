package com.duckblade.osrs.sailing.model.BarracudaTrials;

import lombok.Getter;

@Getter
public enum Course
{
	TEMPOR_TANTRUM(1),
	JUBBLY_JIVE(2),
	GWENITH_GLIDE(3);

	private final int course;

	Course(int course)
	{
		this.course = course;
	}
}