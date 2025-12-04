package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
public enum BarracudaTrial
{

	TEMPOR_TANTRUM(
		new int[]{
			ObjectID.SAILING_BT_SCOREBOARD_TEMPOR_TANTRUM,
			ObjectID.SAILING_BT_TEMPOR_TANTRUM_NORTH_LOC_PARENT,
			ObjectID.SAILING_BT_TEMPOR_TANTRUM_SOUTH_LOC_PARENT,
		}
	),
	JUBBLY_JIVE(
		new int[]{
			ObjectID.SAILING_BT_SCOREBOARD_JUBBLY_JIVE,
			ObjectID.SAILING_BT_JUBBLY_JIVE_TOAD_SUPPLIES_PARENT,
			ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_0_PARENT,
			ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_1_PARENT,
			ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_2_PARENT,
			ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_3_PARENT,
			ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_4_PARENT,
			ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_5_PARENT,
			ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_6_PARENT,
			ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_7_PARENT,
		}
	),
	GWENITH_GLIDE(
		new int[]{
			NpcID.SAILING_BT_GWENITH_GLIDE_CRYSTAL_STEERING_HEADBAR_NPC,
			ObjectID.SAILING_BT_SCOREBOARD_GWENITH_GLIDE,
		}
	),
	;

	private final int[] gameObjectIds;

	public static BarracudaTrial fromGameObjectId(int id)
	{
		for (BarracudaTrial objects : values())
		{
			for (int objectId : objects.getGameObjectIds())
			{
				if (objectId == id)
				{
					return objects;
				}
			}
		}

		return null;
	}
}
