package com.duckblade.osrs.sailing.module;

import com.duckblade.osrs.sailing.features.crewmates.CrewmateOverheadMuter;
import com.duckblade.osrs.sailing.features.charting.SeaChartPanelOverlay;
import com.duckblade.osrs.sailing.features.charting.SeaChartTaskIndex;
import com.duckblade.osrs.sailing.features.charting.WeatherTaskTracker;
import com.duckblade.osrs.sailing.features.barracudatrials.LostCratesOverlay;
import com.duckblade.osrs.sailing.features.mes.DeprioSailsOffHelm;
import com.duckblade.osrs.sailing.features.mes.PrioritizeCargoHold;
import com.duckblade.osrs.sailing.features.navigation.LightningCloudsOverlay;
import com.duckblade.osrs.sailing.features.util.BarracudaTrialTracker;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.facilities.LuffOverlay;
import com.duckblade.osrs.sailing.features.navigation.RapidsOverlay;
import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.charting.SeaChartOverlay;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Set;
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
		BarracudaTrialTracker barracudaTrialTracker,
		LostCratesOverlay lostCratesOverlay,
		BoatTracker boatTracker,
//		CargoHoldTracker cargoHoldTracker,
		CrewmateOverheadMuter crewmateOverheadMuter,
		DeprioSailsOffHelm deprioSailsOffHelm,
		LuffOverlay luffOverlay,
		PrioritizeCargoHold prioritizeCargoHold,
		RapidsOverlay rapidsOverlay,
		LightningCloudsOverlay lightningCloudsOverlay,
		SeaChartOverlay seaChartOverlay,
		SeaChartPanelOverlay seaChartPanelOverlay,
		SeaChartTaskIndex seaChartTaskIndex,
		WeatherTaskTracker weatherTaskTracker
	)
	{
		return ImmutableSet.of(
			barracudaTrialTracker,
			lostCratesOverlay,
			boatTracker,
//			cargoHoldTracker,
			crewmateOverheadMuter,
			deprioSailsOffHelm,
			luffOverlay,
			prioritizeCargoHold,
			rapidsOverlay,
			lightningCloudsOverlay,
			seaChartOverlay,
			seaChartPanelOverlay,
			seaChartTaskIndex,
			weatherTaskTracker
		);
	}

	@Provides
	@Singleton
	SailingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SailingConfig.class);
	}

}
