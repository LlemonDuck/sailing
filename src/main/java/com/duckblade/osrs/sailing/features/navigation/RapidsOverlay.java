package com.duckblade.osrs.sailing.features.navigation;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.model.HelmTier;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class RapidsOverlay
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final Set<Integer> RAPIDS_IDS = ImmutableSet.of(
		ObjectID.SAILING_RAPIDS,
		ObjectID.SAILING_RAPIDS_STRONG,
		ObjectID.SAILING_RAPIDS_POWERFUL,
		ObjectID.SAILING_RAPIDS_DEADLY,
		ObjectID.SAILING_CHARTING_RAPIDS_KHARIDIAN_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_BAY_OF_SARIM,
		ObjectID.SAILING_CHARTING_RAPIDS_GREAT_SOUND,
		ObjectID.SAILING_CHARTING_RAPIDS_LUMBRIDGE_BASIN,
		ObjectID.SAILING_CHARTING_RAPIDS_CRABCLAW_BAY,
		ObjectID.SAILING_CHARTING_RAPIDS_MUDSKIPPER_SOUND,
		ObjectID.SAILING_CHARTING_RAPIDS_RIMMINGTON_STRAIT,
		ObjectID.SAILING_CHARTING_RAPIDS_CATHERBY_BAY,
		ObjectID.SAILING_CHARTING_RAPIDS_BRIMHAVEN_PASSAGE,
		ObjectID.SAILING_CHARTING_RAPIDS_GULF_OF_KOUREND,
		ObjectID.SAILING_CHARTING_RAPIDS_STRAIT_OF_KHAZARD,
		ObjectID.SAILING_CHARTING_RAPIDS_GUTANOTH_BAY,
		ObjectID.SAILING_CHARTING_RAPIDS_HOSIDIAN_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_PILGRIMS_PASSAGE,
		ObjectID.SAILING_CHARTING_RAPIDS_FELDIP_GULF,
		ObjectID.SAILING_CHARTING_RAPIDS_KHARAZI_STRAIT,
		ObjectID.SAILING_CHARTING_RAPIDS_LITUS_LUCIS,
		ObjectID.SAILING_CHARTING_RAPIDS_OOGLOG_CHANNEL,
		ObjectID.SAILING_CHARTING_RAPIDS_FORTIS_BAY,
		ObjectID.SAILING_CHARTING_RAPIDS_ARROW_PASSAGE,
		ObjectID.SAILING_CHARTING_RAPIDS_AUREUM_COAST,
		ObjectID.SAILING_CHARTING_RAPIDS_MENAPHITE_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_TURTLE_BELT,
		ObjectID.SAILING_CHARTING_RAPIDS_WYRMS_WATERS,
		ObjectID.SAILING_CHARTING_RAPIDS_THE_SIMIAN_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_SEA_OF_SHELLS,
		ObjectID.SAILING_CHARTING_RAPIDS_SUNSET_BAY,
		ObjectID.SAILING_CHARTING_RAPIDS_THE_STORM_TEMPOR,
		ObjectID.SAILING_CHARTING_RAPIDS_RED_REEF,
		ObjectID.SAILING_CHARTING_RAPIDS_MISTY_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_MYTHIC_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_ANGLERFISHS_LIGHT,
		ObjectID.SAILING_CHARTING_RAPIDS_BAY_OF_ELIDINIS,
		ObjectID.SAILING_CHARTING_RAPIDS_BREAKBONE_STRAIT,
		ObjectID.SAILING_CHARTING_RAPIDS_TORTUGAN_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_DUSKS_MAW,
		ObjectID.SAILING_CHARTING_RAPIDS_BACKWATER,
		ObjectID.SAILING_CHARTING_RAPIDS_PEARL_BANK,
		ObjectID.SAILING_CHARTING_RAPIDS_THE_LONELY_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_ZUL_EGIL,
		ObjectID.SAILING_CHARTING_RAPIDS_THE_SKULLHORDE,
		ObjectID.SAILING_CHARTING_RAPIDS_SEA_OF_SOULS,
		ObjectID.SAILING_CHARTING_RAPIDS_SOUL_BAY,
		ObjectID.SAILING_CHARTING_RAPIDS_BARRACUDA_BELT,
		ObjectID.SAILING_CHARTING_RAPIDS_THE_EVERDEEP,
		ObjectID.SAILING_CHARTING_RAPIDS_SAPPHIRE_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_WESTERN_GATE,
		ObjectID.SAILING_CHARTING_RAPIDS_RAINBOW_REEF,
		ObjectID.SAILING_CHARTING_RAPIDS_SOUTHERN_EXPANSE,
		ObjectID.SAILING_CHARTING_RAPIDS_PORTH_NEIGWL,
		ObjectID.SAILING_CHARTING_RAPIDS_TIRANNWN_BIGHT,
		ObjectID.SAILING_CHARTING_RAPIDS_CRYSTAL_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_PORTH_GWENITH,
		ObjectID.SAILING_CHARTING_RAPIDS_PISCATORIS_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_VAGABONDS_REST,
		ObjectID.SAILING_CHARTING_RAPIDS_MOONSHADOW,
		ObjectID.SAILING_CHARTING_RAPIDS_FREMENSUND,
		ObjectID.SAILING_CHARTING_RAPIDS_GRANDROOT_BAY,
		ObjectID.SAILING_CHARTING_RAPIDS_VS_BELT,
		ObjectID.SAILING_CHARTING_RAPIDS_FREMENNIK_STRAIT,
		ObjectID.SAILING_CHARTING_RAPIDS_IDESTIA_STRAIT,
		ObjectID.SAILING_CHARTING_RAPIDS_LUNAR_BAY,
		ObjectID.SAILING_CHARTING_RAPIDS_WINTERS_EDGE,
		ObjectID.SAILING_CHARTING_RAPIDS_LUNAR_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_EVERWINTER_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_KANNSKI_TIDES,
		ObjectID.SAILING_CHARTING_RAPIDS_WEISSMERE,
		ObjectID.SAILING_CHARTING_RAPIDS_STONEHEART_SEA,
		ObjectID.SAILING_CHARTING_RAPIDS_SHIVERWAKE_EXPANSE,
		ObjectID.SAILING_CHARTING_RAPIDS_WEISS_MELT
	);

	private final Map<Integer, HelmTier> MIN_HELM_TIER_BY_RAPID_TYPE = ImmutableMap.<Integer, HelmTier>builder()
		.put(ObjectID.SAILING_RAPIDS, HelmTier.IRON)
		.put(ObjectID.SAILING_RAPIDS_STRONG, HelmTier.MITHRIL)
		.put(ObjectID.SAILING_RAPIDS_POWERFUL, HelmTier.RUNE)
		.build();

	private final Client client;
	private final SailingConfig config;
	private final BoatTracker boatTracker;

	private final Set<GameObject> rapids = new HashSet<>();

	private Color safeRapidsColour;
	private Color dangerousRapidsColour;
	private Color unknownRapidsColour;

	@Inject
	public RapidsOverlay(Client client, SailingConfig config, BoatTracker boatTracker)
	{
		this.client = client;
		this.config = config;
		this.boatTracker = boatTracker;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		safeRapidsColour = config.safeRapidsColor();
		dangerousRapidsColour = config.dangerousRapidsColour();
		unknownRapidsColour = config.unknownRapidsColour();
		return config.highlightRapids();
	}

	public void shutDown()
	{
		rapids.clear();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject o = e.getGameObject();
		if (RAPIDS_IDS.contains(o.getId()))
		{
			rapids.add(o);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		rapids.remove(e.getGameObject());
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded e)
	{
		if (e.getWorldView().isTopLevel())
		{
			rapids.clear();
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!SailingUtil.isSailing(client) || !config.highlightRapids())
		{
			return null;
		}

		for (GameObject rapid : rapids)
		{
			ObjectComposition def = SailingUtil.getTransformedObject(client, rapid);
			if (def != null)
			{
				Color colour = getHighlightColour(def.getId());
				OverlayUtil.renderTileOverlay(graphics, rapid, "", colour);
			}
		}

		return null;
	}

	private Color getHighlightColour(int objId)
	{
		HelmTier minTier = MIN_HELM_TIER_BY_RAPID_TYPE.get(objId);
		if (minTier == null)
		{
			return unknownRapidsColour;
		}

		Boat boat = boatTracker.getBoat();
		if (boat == null)
		{
			return unknownRapidsColour;
		}

		HelmTier helmTier = boat.getHelmTier();
		if (helmTier == null)
		{
			return unknownRapidsColour;
		}

		if (helmTier.ordinal() >= minTier.ordinal())
		{
			return safeRapidsColour;
		}

		return dangerousRapidsColour;
	}
}
