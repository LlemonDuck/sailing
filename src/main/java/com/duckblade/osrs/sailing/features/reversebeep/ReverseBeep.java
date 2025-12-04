package com.duckblade.osrs.sailing.features.reversebeep;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.inject.Inject;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.eventbus.Subscribe;

public class ReverseBeep implements PluginLifecycleComponent
{
	Client client;
	BoatTracker boatTracker;
	AudioPlayer audioPlayer;
	boolean reversing;
	boolean beeping;
	Thread audioThread;
	Clip audioClip;

	@Inject
	public ReverseBeep(Client client)
	{
		this.client = client;
		this.boatTracker = new BoatTracker(client);
		this.audioPlayer = new AudioPlayer();
		this.reversing = false;
		this.beeping = false;
		AudioInputStream stream;
		try
		{
			stream = generateBeep();
			this.audioClip = AudioSystem.getClip();
			this.audioClip.open(stream);
		}
		catch (Exception ex)
		{
			System.err.println(ex.getMessage());
		}
	}

	public boolean isEnabled(SailingConfig config)
	{
		return config.reverseBeep();
	}

	// Generates a beep for the reverse
	public AudioInputStream generateBeep()
	{
		float sampleRate = 44100;
		double frequency = 1200;
		double durationSeconds = 1;
		int samples = (int) (durationSeconds * sampleRate);
		int sampleSize = 16;

		AudioFormat format = new AudioFormat(sampleRate, sampleSize, 1, true, false);
		byte[] beepData = new byte[samples * (sampleSize / 8)];

		for (int i = 0; i < samples; i++)
		{
			double angle = 2 * Math.PI * frequency * i / sampleRate;
			short sample = (short) (Short.MAX_VALUE * Math.sin(angle));
			beepData[i * 2] = (byte) (sample & 0xFF);
			beepData[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
		}
		InputStream byteArrayInputStream = new ByteArrayInputStream(beepData);
		return new AudioInputStream(byteArrayInputStream, format, samples);
	}

	// Beeps for specified time
	public void beepForDuration(long milliseconds)
	{
		audioClip.start();
		try
		{
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException ex)
		{
			System.err.println(ex.getMessage());
		}
	}

	// Stops beeping for duration
	public void pauseForDuration(long milliseconds)
	{
		stopBeeps();
		try
		{
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException ex)
		{
			System.err.println(ex.getMessage());
		}
	}

	// Become a truck
	public void doBeeps()
	{
		this.audioThread = new Thread(() ->
		{
			if (audioClip != null)
			{
				while (this.reversing)
				{
					// See I enjoy that it's .6 seconds and sounds perfect isn't that beautiful?
					beepForDuration(600);
					pauseForDuration(600);
				}
			}
		});
		this.audioThread.start();
	}

	// Cease being a truck
	public void stopBeeps()
	{
		if (this.audioClip != null)
		{
			audioClip.stop();
			audioClip.setMicrosecondPosition(0);
		}
	}

	// Keep an eye on the PRNDL
	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarbitId() == VarbitID.SAILING_SIDEPANEL_BOAT_MOVE_MODE)
		{
			if (e.getValue() == 3)
			{
				this.reversing = true;
				doBeeps();
			}
			else
			{
				this.reversing = false;
				stopBeeps();
			}
		}
	}
}
