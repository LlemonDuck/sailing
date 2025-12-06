package com.duckblade.osrs.sailing.debugplugin.features;

import com.duckblade.osrs.sailing.debugplugin.SailingDebugConfig;
import com.duckblade.osrs.sailing.debugplugin.module.DebugLifecycleComponent;
import com.duckblade.osrs.sailing.features.util.CrewmateIndex;
import com.duckblade.osrs.sailing.features.util.CrewAssignment;
import com.duckblade.osrs.sailing.model.Crewmate;
import com.google.common.collect.ImmutableMap;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Slf4j
@Singleton
public class CrewmateInfoOverlay
	extends OverlayPanel
	implements DebugLifecycleComponent
{

	private static final Map<Integer, Integer> CREWMATE_VARBS = ImmutableMap.<Integer, Integer>builder()
		.put(VarbitID.SAILING_CREW_SLOT_1, VarbitID.SAILING_CREW_SLOT_1_POSITION)
		.put(VarbitID.SAILING_CREW_SLOT_2, VarbitID.SAILING_CREW_SLOT_2_POSITION)
		.put(VarbitID.SAILING_CREW_SLOT_3, VarbitID.SAILING_CREW_SLOT_3_POSITION)
		.put(VarbitID.SAILING_CREW_SLOT_4, VarbitID.SAILING_CREW_SLOT_4_POSITION)
		.put(VarbitID.SAILING_CREW_SLOT_5, VarbitID.SAILING_CREW_SLOT_5_POSITION)
		.build();

	private final Client client;
	private final CrewmateIndex crewmateIndex;

	@Inject
	public CrewmateInfoOverlay(Client client, CrewmateIndex crewmateIndex)
	{
		this.client = client;
		this.crewmateIndex = crewmateIndex;

		setPreferredPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	@Override
	public boolean isEnabled(SailingDebugConfig config)
	{
		return config.crewmates();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		getPanelComponent().getChildren().add(TitleComponent.builder()
			.text("Crewmates")
			.build());

		CREWMATE_VARBS.forEach((crewmateVarb, positionVarb) ->
		{
			Crewmate crewmate = crewmateIndex.getCrewmate(client.getVarbitValue(crewmateVarb));
			if (crewmate != null)
			{
				int facilityIx = client.getVarbitValue(positionVarb);
				CrewAssignment facility = CrewAssignment.fromCrewAssignmentVarb(facilityIx);

				if (facility != null)
				{
					addLine(crewmate.getName(), facility.toString(), Color.GREEN);
				}
				else if (facilityIx != 0)
				{
					addLine(crewmate.getName(), String.valueOf(facilityIx), Color.RED);
				}
				else
				{
					addLine(crewmate.getName(), "None", Color.WHITE);
				}
			}
		});

		return super.render(graphics);
	}

	private void addLine(String left, String right, Color color)
	{
		getPanelComponent().getChildren().add(LineComponent.builder()
			.left(left)
			.right(right)
			.rightColor(color)
			.build());
	}
}
