package com.duckblade.osrs.sailing.model;

import lombok.Value;

@Value
public class Crewmate
{

	int uniqueId;
	String name;

	int helmsmanship;
	int privateering;
	int deckhandiness;

}
