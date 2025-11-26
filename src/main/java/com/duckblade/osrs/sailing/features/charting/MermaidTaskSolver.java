package com.duckblade.osrs.sailing.features.charting;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MermaidTaskSolver
	implements PluginLifecycleComponent
{

	@VisibleForTesting
	static final ImmutableSet<Integer> MERMAID_IDS = ImmutableSet.of(
		NpcID.SAILING_CHARTING_MERMAID_GUIDE_1,
		NpcID.SAILING_CHARTING_MERMAID_GUIDE_2,
		NpcID.SAILING_CHARTING_MERMAID_GUIDE_3,
		NpcID.SAILING_CHARTING_MERMAID_GUIDE_4,
		NpcID.SAILING_CHARTING_MERMAID_GUIDE_5
	);

	@VisibleForTesting
	static final Map<SeaChartTask, Multiset<Integer>> SOLUTIONS = ImmutableMap.<SeaChartTask, Multiset<Integer>>builder()
		.put(
			SeaChartTask.TASK_12,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.XBOWS_CROSSBOW_STOCK_WILLOW, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_44,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.PIEDISH, 1)
				.addCopies(ItemID.POT_FLOUR, 1)
				.addCopies(ItemID.COOKING_APPLE, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_45,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.IRON_MED_HELM, 1)
				.addCopies(ItemID.BRONZE_CHAINBODY, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_46,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.CABBAGE_SEED, 5)
				.build()
		)
		.put(
			SeaChartTask.TASK_47,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.WATERMELON, 10)
				.build()
		)
		.put(
			SeaChartTask.TASK_48,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.VIAL_EMPTY, 1)
				.addCopies(ItemID.AVANTOE, 1)
				.addCopies(ItemID.SNAPE_GRASS, 1)
				.addCopies(ItemID.BRUT_CAVIAR, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_49,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.HARRALANDERVIAL, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_50,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.PAPAYA, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_51,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.ASHES, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_52,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.ICS_LITTLE_SAP_BUCKET, 1)
				.addCopies(ItemID.MORT_SLIMEY_EEL, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_53,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.BARLEY, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_125,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.II_CAPTURED_IMPLING_4, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_127,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.CABBAGE, 1)
				.addCopies(ItemID.ONION, 1)
				.addCopies(ItemID.TOMATO, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_126,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.COAL, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_128,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.KWUARM, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_129,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.DWELLBERRIES, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_152,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.FLOWERS_WATERFALL_QUEST_BLACK, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_153,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.BUTTERFLY_JAR, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_154,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.CALQUAT_FRUIT_KEG_EMPTY, 2)
				.addCopies(ItemID.ALE_YEAST, 1)
				.addCopies(ItemID.OAK_ROOTS, 1)
				.addCopies(ItemID.BARLEY_MALT, 2)
				.build()
		)
		.put(
			SeaChartTask.TASK_155,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.VIAL_EMPTY, 1)
				.addCopies(ItemID.COCONUT, 1)
				.addCopies(ItemID.TOADFLAX, 1)
				.addCopies(ItemID.YEW_ROOTS, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_156,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.SOILED_PAGE, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_170,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.THATCHING_SPAR_DENSE, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_171,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.GOLD_ORE, 2)
				.build()
		)
		.put(
			SeaChartTask.TASK_172,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.MALICIOUS_ASHES, 2)
				.build()
		)
		.put(
			SeaChartTask.TASK_192,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.SANDWICH_LADY_BOTTOM, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_241,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.TABLET_KHARYLL, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_242,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.RAW_COD, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_243,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.XBOWS_CROSSBOW_LIMBS_BRONZE, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_244,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.ONION, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_245,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.TORSTOL, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_246,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.NEEDLE, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_247,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.POH_CLOCKWORK_MECHANISM, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_248,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.DRAGONSHIELD_A, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_249,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.VIAL_BLOOD, 1)
				.addCopies(ItemID.CADANTINE, 1)
				.addCopies(ItemID.WINE_OF_ZAMORAK, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_250,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.DRAGON_BITTER, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_251,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.RAIN_BOW, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_252,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.ROYAL_CROWN, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_271,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.SLAYER_NOSEPEG, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_272,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.CHARCOAL, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_273,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.WOADLEAF, 2)
				.addCopies(ItemID.ONION, 2)
				.build()
		)
		.put(
			SeaChartTask.TASK_274,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.DORGESH_SWAMP_WEED, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_307,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.HUNTING_STRIPY_BIRD_FEATHER, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_308,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.EQUA_LEAVES, 1)
				.addCopies(ItemID.BATTA_TIN, 1)
				.addCopies(ItemID.TOMATO, 2)
				.addCopies(ItemID.CHEESE, 1)
				.addCopies(ItemID.DWELLBERRIES, 1)
				.addCopies(ItemID.ONION, 1)
				.addCopies(ItemID.CABBAGE, 1)
				.addCopies(ItemID.GIANNE_DOUGH, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_309,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.LIME, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_310,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.FEDORA, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_311,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.AERIAL_FISHING_COMMON_TENCH, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_312,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.WOODPLANK, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_313,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.GHRAZI_RAPIER, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_321,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.SILVER_ORE, 1)
				.addCopies(ItemID.CHISEL, 1)
				.addCopies(ItemID.UNCUT_JADE, 1)
				.addCopies(ItemID.RING_MOULD, 1)
				.addCopies(ItemID.COSMICRUNE, 1)
				.addCopies(ItemID.AIRRUNE, 3)
				.build()
		)
		.put(
			SeaChartTask.TASK_322,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.POTATO, 1)
				.addCopies(ItemID.CACTUS_POTATO, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_323,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.ENAKH_SANDSTONE_LARGE, 1)
				.addCopies(ItemID.ENAKH_SANDSTONE_SMALL, 1)
				.addCopies(ItemID.ENAKH_SANDSTONE_TINY, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_324,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.TUXEDO_BOWTIE, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_325,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.DOUBLE_EYE_PATCH, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_326,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.BUCKET_HELM_GOLD, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_348,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.RING_MOULD, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_349,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.TRAIL_BOB_SHIRT_BLUE, 1)
				.addCopies(ItemID.TRAIL_BOB_SHIRT_PURPLE, 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_350,
			ImmutableMultiset.<Integer>builder()
				.addCopies(ItemID.TARROMIN, 1)
				.build()
		)
		.build();

	private static final int SCRIPT_ID_SEARCH_LOAD = 750;
	private static final int SCRIPT_ID_SEARCH_SELECT = 754;
	private static final int SCRIPT_ID_MES_NUMBER_INPUT_LOAD = 108;

	private final Client client;
	private final ClientThread clientThread;
	private final SeaChartTaskIndex taskIndex;

	private final Multiset<Integer> inputs = HashMultiset.create();

	private SeaChartTask task;
	private Multiset<Integer> solution;
	private int pendingInput = -1;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.chartingMermaidSolver();
	}

	@Override
	public void shutDown()
	{
		reset();
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (task == null)
		{
			return;
		}

		if (task.isComplete(client))
		{
			log.debug("task {} completed, clearing", task.getTaskId());
			reset();
		}

		if (!SailingUtil.isSailing(client) || task.getLocation().distanceTo(SailingUtil.getTopLevelWorldPoint(client)) > 25)
		{
			log.debug("cancelling in progress task {} due to distance", task.getTaskId());
			reset();
		}
	}

	@Subscribe(priority = 100)
	public void onGrandExchangeSearched(GrandExchangeSearched e)
	{
		if (task == null || !SailingUtil.isSailing(client))
		{
			return;
		}

		if (pendingInput != -1)
		{
			log.debug("Committing input {} with implicit count 1 due to ge prompt reappearing", pendingInput);
			inputs.add(pendingInput);
			pendingInput = -1;
		}

		Multiset<Integer> notYetEntered = Multisets.difference(solution, inputs);
		if (notYetEntered.isEmpty())
		{
			log.warn("no items left to enter for task {} but prompt loaded!", task.getTaskId());
			return;
		}

		e.consume();

		Multiset.Entry<Integer> first = notYetEntered.entrySet().iterator().next();

		client.setVarcStrValue(VarClientID.MESLAYERINPUT, "Sailing Mermaid Solver");
		client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);

		client.setGeSearchResultIndex(0);
		client.setGeSearchResultCount(1);
		client.setGeSearchResultIds(new short[]{first.getElement().shortValue()});
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired e)
	{
		if (task == null || !SailingUtil.isSailing(client))
		{
			return;
		}

		if (e.getScriptId() == SCRIPT_ID_SEARCH_LOAD)
		{
			clientThread.invokeLater(() ->
			{
				// immediately filter on widget load
				// this will invoke onGrandExchangeSearched on its own
				Widget geSearchBox = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
				if (geSearchBox != null && !geSearchBox.isHidden())
				{
					client.setVarcStrValue(VarClientID.MESLAYERINPUT, "Sailing Mermaid Solver");
					client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);
					client.runScript(geSearchBox.getOnKeyListener());
					geSearchBox.setHidden(true);
				}
			});
		}

		if (e.getScriptId() == SCRIPT_ID_MES_NUMBER_INPUT_LOAD)
		{
			clientThread.invokeLater(() ->
			{
				int desiredCount = solution.count(pendingInput);
				if (desiredCount != 0)
				{
					client.setVarcStrValue(VarClientID.MESLAYERINPUT, String.valueOf(desiredCount)); // set the value
					client.runScript(client.getWidget(InterfaceID.Chatbox.MES_TEXT2).getOnKeyListener()); // make ui refresh to pick up the value
				}
			});
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired e)
	{
		if (e.getScriptId() != SCRIPT_ID_SEARCH_SELECT || task == null)
		{
			return;
		}

		pendingInput = (int) e.getScriptEvent().getArguments()[1];
		log.debug("item {} selected for task {}", pendingInput, task.getTaskId());
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged e)
	{
		if (!SailingUtil.isLocalPlayer(client, e.getSource()) || e.getTarget() == null)
		{
			return;
		}

		reset();

		Actor target = e.getTarget();
		if (!(target instanceof NPC) || !MERMAID_IDS.contains(((NPC) target).getId()))
		{
			return;
		}

		WorldPoint playerLoc = SailingUtil.getTopLevelWorldPoint(client);
		SeaChartTask maybeTask = taskIndex.findTask((NPC) target);
		if (maybeTask == null)
		{
			log.warn("no mermaid task found at {}", playerLoc);
			return;
		}
		task = maybeTask;

		solution = SOLUTIONS.get(maybeTask);
		if (solution == null)
		{
			log.warn("no solution found for task {}", task.getTaskId());
			reset();
			return;
		}

		log.debug("solution for task {} is {}", task.getTaskId(), solution);
	}

	private void reset()
	{
		task = null;
		solution = null;
		inputs.clear();
		pendingInput = -1;
	}
}
