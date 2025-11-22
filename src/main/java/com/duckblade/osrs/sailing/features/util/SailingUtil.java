package com.duckblade.osrs.sailing.features.util;

import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SailingUtil
{

	public static boolean isSailing(Client client)
	{
		return client.getLocalPlayer() != null &&
			!client.getLocalPlayer().getWorldView().isTopLevel();
	}

	// on boats, InteractingChanged fires for the local player but the target is null
	// it DOES fire an event with the expected target for a separate instance of Player with the same ID
	public static boolean isLocalPlayer(Client client, Actor actor)
	{
		return client.getLocalPlayer() != null &&
				actor instanceof Player && ((Player) actor).getId() == client.getLocalPlayer().getId();
	}
}
