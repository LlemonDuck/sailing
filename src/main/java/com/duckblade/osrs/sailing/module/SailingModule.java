package com.duckblade.osrs.sailing.module;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.barracudatrials.HidePortalTransitions;
import com.duckblade.osrs.sailing.features.barracudatrials.JubblyJiveHelper;
import com.duckblade.osrs.sailing.features.courier.CourierTaskLedgerOverlay;
import com.duckblade.osrs.sailing.features.courier.CourierTaskTracker;
import com.duckblade.osrs.sailing.features.facilities.CargoHoldTracker;
import com.duckblade.osrs.sailing.features.facilities.LuffOverlay;
import com.duckblade.osrs.sailing.features.facilities.CrystalExtractorHighlight;
import com.duckblade.osrs.sailing.features.facilities.SpeedBoostInfoBox;
import com.duckblade.osrs.sailing.features.navigation.LightningCloudsOverlay;
import com.duckblade.osrs.sailing.features.navigation.RapidsOverlay;
import com.duckblade.osrs.sailing.features.barracudatrials.LostCargoHighlighter;
import com.duckblade.osrs.sailing.features.barracudatrials.TemporTantrumHelper;
import com.duckblade.osrs.sailing.features.charting.CurrentDuckTaskTracker;
import com.duckblade.osrs.sailing.features.charting.SeaChartOverlay;
import com.duckblade.osrs.sailing.features.charting.SeaChartPanelOverlay;
import com.duckblade.osrs.sailing.features.charting.SeaChartTaskIndex;
import com.duckblade.osrs.sailing.features.charting.WeatherTaskTracker;
import com.duckblade.osrs.sailing.features.charting.MermaidTaskSolver;
import com.duckblade.osrs.sailing.features.courier.CourierDestinationOverlay;
import com.duckblade.osrs.sailing.features.crewmates.CrewmateOverheadMuter;
import com.duckblade.osrs.sailing.features.mes.DeprioSailsOffHelm;
import com.duckblade.osrs.sailing.features.mes.PrioritizeCargoHold;
import com.duckblade.osrs.sailing.features.mes.HideStopNavigatingDuringTrials;
import com.duckblade.osrs.sailing.features.oceanencounters.Castaway;
import com.duckblade.osrs.sailing.features.oceanencounters.ClueCasket;
import com.duckblade.osrs.sailing.features.oceanencounters.ClueTurtle;
import com.duckblade.osrs.sailing.features.oceanencounters.GiantClam;
import com.duckblade.osrs.sailing.features.oceanencounters.LostShipment;
import com.duckblade.osrs.sailing.features.oceanencounters.MysteriousGlow;
import com.duckblade.osrs.sailing.features.oceanencounters.OceanMan;
import com.duckblade.osrs.sailing.features.salvaging.SalvagingHighlight;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Set;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
public class SailingModule extends AbstractModule
{

	@Override
	protected void configure()
	{
		bind(ComponentManager.class);
	}

	@Provides
	Set<PluginLifecycleComponent> lifecycleComponents(
		@Named("developerMode") boolean developerMode,

		BoatTracker boatTracker,
		CargoHoldTracker cargoHoldTracker,
		Castaway castaway,
		ClueCasket clueCasket,
		ClueTurtle clueTurtle,
		CourierTaskLedgerOverlay courierTaskLedgerOverlay,
		CourierTaskTracker courierTaskTracker,
		CourierDestinationOverlay courierDestinationOverlay,
		CrewmateOverheadMuter crewmateOverheadMuter,
		CurrentDuckTaskTracker currentDuckTaskTracker,
		DeprioSailsOffHelm deprioSailsOffHelm,
		HideStopNavigatingDuringTrials hideStopNavigatingDuringTrials,
		GiantClam giantClam,
		HidePortalTransitions hidePortalTransitions,
		JubblyJiveHelper jubblyJiveHelper,
		TemporTantrumHelper temporTantrumHelper,
		LightningCloudsOverlay lightningCloudsOverlay,
		LostCargoHighlighter lostCargoHighlighter,
		LostShipment lostShipment,
		LuffOverlay luffOverlay,
		CrystalExtractorHighlight crystalExtractorHighlight,
		MermaidTaskSolver mermaidTaskSolver,
		MysteriousGlow mysteriousGlow,
		OceanMan oceanMan,
		PrioritizeCargoHold prioritizeCargoHold,
		RapidsOverlay rapidsOverlay,
		SalvagingHighlight salvagingHighlight,
		SeaChartOverlay seaChartOverlay,
		SeaChartPanelOverlay seaChartPanelOverlay,
		SeaChartTaskIndex seaChartTaskIndex,
		SpeedBoostInfoBox speedBoostInfoBox,
		WeatherTaskTracker weatherTaskTracker
	)
	{
		var builder = ImmutableSet.<PluginLifecycleComponent>builder()
			.add(boatTracker)
			.add(castaway)
			.add(clueCasket)
			.add(clueTurtle)
			.add(courierTaskLedgerOverlay)
			.add(courierTaskTracker)
			.add(courierDestinationOverlay)
			.add(crewmateOverheadMuter)
			.add(currentDuckTaskTracker)
			.add(deprioSailsOffHelm)
			.add(hideStopNavigatingDuringTrials)
			.add(giantClam)
			.add(hidePortalTransitions)
			.add(jubblyJiveHelper)
			.add(temporTantrumHelper)
			.add(lightningCloudsOverlay)
			.add(lostCargoHighlighter)
			.add(lostShipment)
			.add(luffOverlay)
			.add(crystalExtractorHighlight)
			.add(mermaidTaskSolver)
			.add(mysteriousGlow)
			.add(oceanMan)
			.add(prioritizeCargoHold)
			.add(rapidsOverlay)
			.add(salvagingHighlight)
			.add(seaChartOverlay)
			.add(seaChartPanelOverlay)
			.add(seaChartTaskIndex)
			.add(speedBoostInfoBox)
			.add(weatherTaskTracker);

		// features still in development
		if (developerMode)
		{
			builder
				.add(cargoHoldTracker);
		}

		return builder.build();
	}

	@Provides
	@Singleton
	SailingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SailingConfig.class);
	}

}
