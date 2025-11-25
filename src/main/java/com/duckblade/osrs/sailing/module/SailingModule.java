package com.duckblade.osrs.sailing.module;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.facilities.CargoHoldTracker;
import com.duckblade.osrs.sailing.features.facilities.LuffOverlay;
import com.duckblade.osrs.sailing.features.navigation.RapidsOverlay;
import com.duckblade.osrs.sailing.features.barracudatrials.BarracudaTrialHelper;
import com.duckblade.osrs.sailing.features.charting.CurrentDuckTaskTracker;
import com.duckblade.osrs.sailing.features.charting.SeaChartOverlay;
import com.duckblade.osrs.sailing.features.charting.SeaChartPanelOverlay;
import com.duckblade.osrs.sailing.features.charting.SeaChartTaskIndex;
import com.duckblade.osrs.sailing.features.charting.WeatherTaskTracker;
import com.duckblade.osrs.sailing.features.courier.CourierDestinationOverlay;
import com.duckblade.osrs.sailing.features.crewmates.CrewmateOverheadMuter;
import com.duckblade.osrs.sailing.features.mes.DeprioSailsOffHelm;
import com.duckblade.osrs.sailing.features.mes.PrioritizeCargoHold;
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

		BarracudaTrialHelper barracudaTrialHelper,
		BoatTracker boatTracker,
		CargoHoldTracker cargoHoldTracker,
		CourierDestinationOverlay courierDestinationOverlay,
		CrewmateOverheadMuter crewmateOverheadMuter,
		CurrentDuckTaskTracker currentDuckTaskTracker,
		DeprioSailsOffHelm deprioSailsOffHelm,
		LuffOverlay luffOverlay,
		PrioritizeCargoHold prioritizeCargoHold,
		RapidsOverlay rapidsOverlay,
		SeaChartOverlay seaChartOverlay,
		SeaChartPanelOverlay seaChartPanelOverlay,
		SeaChartTaskIndex seaChartTaskIndex,
		WeatherTaskTracker weatherTaskTracker
	)
	{
		var builder = ImmutableSet.<PluginLifecycleComponent>builder()
			.add(barracudaTrialHelper)
			.add(boatTracker)
			.add(courierDestinationOverlay)
			.add(crewmateOverheadMuter)
			.add(currentDuckTaskTracker)
			.add(deprioSailsOffHelm)
			.add(luffOverlay)
			.add(prioritizeCargoHold)
			.add(rapidsOverlay)
			.add(seaChartOverlay)
			.add(seaChartPanelOverlay)
			.add(seaChartTaskIndex)
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
