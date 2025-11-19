package com.duckblade.osrs.sailing.util;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SailingUtil
{

	public static final Set<Integer> SAIL_IDS = ImmutableSet.of(
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_WOOD,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_OAK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_TEAK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_MAHOGANY,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_CAMPHOR,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_IRONWOOD,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_ROSEWOOD,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_WOOD,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_OAK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_TEAK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_MAHOGANY,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_CAMPHOR,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_IRONWOOD,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_ROSEWOOD,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_WOOD,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_OAK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_TEAK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_MAHOGANY,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_CAMPHOR,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_IRONWOOD,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_ROSEWOOD
	);

	public static final Set<Integer> RAPIDS_IDS = ImmutableSet.of(
		ObjectID.SAILING_RAPIDS,
		ObjectID.SAILING_RAPIDS_STRONG,
		ObjectID.SAILING_RAPIDS_POWERFUL,
		ObjectID.SAILING_RAPIDS_DEADLY
	);

	public static boolean isSailing(Client client)
	{
		return client.getLocalPlayer() != null &&
			!client.getLocalPlayer().getWorldView().isTopLevel();
	}

}
