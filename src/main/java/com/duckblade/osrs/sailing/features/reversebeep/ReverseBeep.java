package com.duckblade.osrs.sailing.features.reversebeep;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ReverseBeep implements PluginLifecycleComponent
{

	private static final int VARB_VALUE_REVERSING = 3;

	private final AudioPlayer audioPlayer;

	private boolean reversing;

	private ScheduledExecutorService es;
	private ScheduledFuture<?> beepTask;

	private float gain;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		gain = (float) config.reverseBeepVolume() - 50.0f;
		return config.reverseBeep();
	}

	@Override
	public void startUp()
	{
		reversing = false;
		es = Executors.newScheduledThreadPool(1);
	}

	@Override
	public void shutDown()
	{
		es.shutdown();
		beepTask = null;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (reversing && beepTask == null)
		{
			// become a truck
			beepTask = es.scheduleAtFixedRate(this::beepOnce, 0, 1200, java.util.concurrent.TimeUnit.MILLISECONDS);
		}
		else if (!reversing && beepTask != null)
		{
			// become a boat again
			beepTask.cancel(false);
			beepTask = null;
		}
	}

    @Subscribe
    public void onGameStateChanged(GameStateChanged e) {
        if (e.getGameState() == GameState.HOPPING
                || e.getGameState() == GameState.CONNECTION_LOST
                || e.getGameState() == GameState.LOGIN_SCREEN
                || e.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR
                || e.getGameState() == GameState.UNKNOWN)
        {
            beepTask.cancel(false);
            beepTask = null;
        }
    }

	// Keep an eye on the PRNDL
	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarbitId() == VarbitID.SAILING_SIDEPANEL_BOAT_MOVE_MODE)
		{
			reversing = e.getValue() == VARB_VALUE_REVERSING;
		}
	}

	private void beepOnce()
	{
		try
		{
			audioPlayer.play(ReverseBeep.class, "beep.wav", gain);
		}
		catch (Exception e)
		{
			log.warn("Failed to play beep", e);
			beepTask.cancel(false);
		}
	}

}
