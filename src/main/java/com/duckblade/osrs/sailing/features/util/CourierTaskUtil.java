package com.duckblade.osrs.sailing.features.util;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.VarbitID;

@Slf4j
public class CourierTaskUtil
{
	public enum TaskType
	{
		DELIVERY,
		BOUNTY
	}

	public static final List<Integer> TASK_SLOT_IDS = ImmutableList.of(
		VarbitID.PORT_TASK_SLOT_0_ID,
		VarbitID.PORT_TASK_SLOT_1_ID,
		VarbitID.PORT_TASK_SLOT_2_ID,
		VarbitID.PORT_TASK_SLOT_3_ID,
		VarbitID.PORT_TASK_SLOT_4_ID
	);

	public static final List<Integer> CARGO_RETRIEVED_VARBITS = ImmutableList.of(
		VarbitID.PORT_TASK_SLOT_0_CARGO_TAKEN,
		VarbitID.PORT_TASK_SLOT_1_CARGO_TAKEN,
		VarbitID.PORT_TASK_SLOT_2_CARGO_TAKEN,
		VarbitID.PORT_TASK_SLOT_3_CARGO_TAKEN,
		VarbitID.PORT_TASK_SLOT_4_CARGO_TAKEN
	);

	public static final List<Integer> CARGO_DELIVERED_VARBITS = ImmutableList.of(
		VarbitID.PORT_TASK_SLOT_0_CARGO_DELIVERED,
		VarbitID.PORT_TASK_SLOT_1_CARGO_DELIVERED,
		VarbitID.PORT_TASK_SLOT_2_CARGO_DELIVERED,
		VarbitID.PORT_TASK_SLOT_3_CARGO_DELIVERED,
		VarbitID.PORT_TASK_SLOT_4_CARGO_DELIVERED
	);

	public static final Set<Integer> ALL_COURIER_TASK_VARBITS = Stream.of(
		TASK_SLOT_IDS,
		CARGO_RETRIEVED_VARBITS,
		CARGO_DELIVERED_VARBITS
	).flatMap(List::stream)
		.collect(Collectors.toUnmodifiableSet());
}
