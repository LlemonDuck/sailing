package com.duckblade.osrs.sailing.model;

import com.duckblade.osrs.sailing.features.util.SailingUtil;
import lombok.Data;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.WorldEntity;

@Data
public class Boat
{

	@Getter
	private final int worldViewId;
	private final WorldEntity worldEntity;

	GameObject hull;
	GameObject sail;
	GameObject helm;
	GameObject salvagingHook;
	GameObject cargoHold;

	// these are intentionally not cached in case the object is transformed without respawning
	// e.g. helms have a different idle vs in-use id
	public HullTier getHullTier()
	{
		return hull != null ? HullTier.fromGameObjectId(hull.getId()) : null;
	}

	public SailTier getSailTier()
	{
		return sail != null ? SailTier.fromGameObjectId(sail.getId()) : null;
	}

	public HelmTier getHelmTier()
	{
		return helm != null ? HelmTier.fromGameObjectId(helm.getId()) : null;
	}

	public SalvagingHookTier getSalvagingHookTier()
	{
		return salvagingHook != null ? SalvagingHookTier.fromGameObjectId(salvagingHook.getId()) : null;
	}

	public CargoHoldTier getCargoHoldTier()
	{
		return cargoHold != null ? CargoHoldTier.fromGameObjectId(cargoHold.getId()) : null;
	}

	public SizeClass getSizeClass()
	{
		return hull != null ? SizeClass.fromGameObjectId(hull.getId()) : null;
	}

	public int getCargoCapacity(boolean uim)
	{
		CargoHoldTier cargoHoldTier = getCargoHoldTier();
		if (cargoHoldTier == null)
		{
			return 0;
		}

		return cargoHoldTier.getCapacity(getSizeClass(), uim);
	}

	public int getCargoCapacity(Client client)
	{
		return getCargoCapacity(SailingUtil.isUim(client));
	}

	public String getDebugString()
	{
		return String.format(
			"Id: %d, Hull: %s, Sail: %s, Helm: %s, Hook: %s, Cargo: %s",
			worldViewId,
			getHullTier(),
			getSailTier(),
			getHelmTier(),
			getSalvagingHookTier(),
			getCargoHoldTier()
		);
	}
}
