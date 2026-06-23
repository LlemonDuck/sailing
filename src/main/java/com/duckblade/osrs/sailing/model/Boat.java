package com.duckblade.osrs.sailing.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.GameObject;
import net.runelite.api.WorldEntity;
import net.runelite.api.gameval.ObjectID;

@Data
public class Boat
{
	public static final ImmutableSet<Integer> SAIL_PATTERN_IDS = ImmutableSet.of(
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_LINEN,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_CANVAS,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_COTTON,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_BLACK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_YELLOW,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_PURPLE,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_GREEN,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_BLUE,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_PINK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_RED,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_LINEN,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_CANVAS,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_COTTON,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_BLACK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_YELLOW,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_PURPLE,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_GREEN,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_BLUE,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_PINK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_RED,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_LINEN,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_CANVAS,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_COTTON,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_BLACK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_YELLOW,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_PURPLE,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_GREEN,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_BLUE,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_PINK,
		ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_RED
	);

	@Getter
	private final int worldViewId;
	private final WorldEntity worldEntity;

	GameObject hull;
	GameObject sailPattern;
	GameObject sailMast;
	GameObject helm;
	GameObject cargoHold;

	@Setter(AccessLevel.NONE)
	Set<GameObject> salvagingHooks = new HashSet<>();

	// these are intentionally not cached in case the object is transformed without respawning
	// e.g. helms have a different idle vs in-use id
	public HullTier getHullTier()
	{
		return hull != null ? HullTier.fromGameObjectId(hull.getId()) : null;
	}

	public Integer getSailPatternObject()
	{
		if (sailPattern == null)
		{
			return null;
		}

		int id = sailPattern.getId();
		return SAIL_PATTERN_IDS.contains(id) ? id : null;
	}
	public SailMast getSailMastObject()
	{
		return sailMast != null ? SailMast.fromGameObjectId(sailMast.getId()) : null;
	}

	public HelmTier getHelmTier()
	{
		return helm != null ? HelmTier.fromGameObjectId(helm.getId()) : null;
	}

	public List<SalvagingHookTier> getSalvagingHookTiers()
	{
		return salvagingHooks.stream()
			.mapToInt(GameObject::getId)
			.mapToObj(SalvagingHookTier::fromGameObjectId)
			.collect(Collectors.toList());
	}

	public CargoHoldTier getCargoHoldTier()
	{
		return cargoHold != null ? CargoHoldTier.fromGameObjectId(cargoHold.getId()) : null;
	}

	public SizeClass getSizeClass()
	{
		return hull != null ? SizeClass.fromGameObjectId(hull.getId()) : null;
	}

	public Set<GameObject> getAllFacilities()
	{
		Set<GameObject> facilities = new HashSet<>();
		facilities.add(hull);
		facilities.add(sailPattern);
		facilities.add(sailMast);
		facilities.add(helm);
		facilities.addAll(salvagingHooks);
		facilities.add(cargoHold);
		return facilities;
	}

	public int getCargoCapacity()
	{
		CargoHoldTier cargoHoldTier = getCargoHoldTier();
		if (cargoHoldTier == null)
		{
			return 0;
		}

		return cargoHoldTier.getCapacity(getSizeClass());
	}

	public int getSpeedBoostDuration()
	{
		SailMast sailMast = getSailMastObject();
		if (sailMast == null)
		{
			return -1;
		}

		return sailMast.getSpeedBoostDuration(getSizeClass());
	}

	public String getDebugString()
	{
		return String.format(
			"Id: %d, Hull: %s, Sail Pattern: %s, Sail Mast: %s, Helm: %s, Hook: %s, Cargo: %s",
			worldViewId,
			getHullTier(),
			getSailPatternObject(),
			getSailMastObject(),
			getHelmTier(),
			getSalvagingHookTiers()
				.stream()
				.map(SalvagingHookTier::toString)
				.collect(Collectors.joining(", ", "[", "]")),
			getCargoHoldTier()
		);
	}
}
