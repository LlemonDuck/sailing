package com.duckblade.osrs.sailing.features.barracudatrials;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.SailingPanel;
import com.duckblade.osrs.sailing.SailingPlugin;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class GwenithGlideSplits implements PluginLifecycleComponent
{
    // Time script
    private static final int GT_UI_SCRIPT_ID = 8605; // Sailing BT UI time script
    // Portal overlay script
    private static final int CREATE_PORTAL_OVERLAY_SCRIPT_ID = 5984;

    // parent dir for all Barracuda trial times
    private static final File TRIALS_DIR =
            new File(RuneLite.RUNELITE_DIR, "barracuda-trials-times");

    // subdir for Gwenith Glide
    private static final File TIMES_DIR =
            new File(TRIALS_DIR, "gwenith-glide");

    private static final int MAX_RECENT_RUNS = 20;

    private final Client client;
    private final SailingConfig config;
    private final ClientToolbar clientToolbar;

    private SailingPanel panel;
    private NavigationButton navButton;

    private boolean historyLoaded = false;
    private boolean started;
    private int portalIndex = 1;       // which portal we are on (for split labels)
    private int currentTime = -1;      // total ticks since start (from script 8605)
    private int lastSplitTime = 0;     // total ticks at previous portal
    private final List<String> splits = new ArrayList<>();

    public static class RunRecord
    {
        public final int kc;
        public final int totalTicks;
        @Getter
        final List<String> splits;

        RunRecord(int kc, int totalTicks, List<String> splits)
        {
            this.kc = kc;
            this.totalTicks = totalTicks;
            this.splits = splits;
        }
    }

    private final List<RunRecord> recentRuns = new ArrayList<>();
    private int personalBestTicks = -1;

    @Inject
    public GwenithGlideSplits(Client client, SailingConfig config, ClientToolbar clientToolbar, ItemManager itemManager)
    {
        this.client = client;
        this.config = config;
        this.clientToolbar = clientToolbar;
    }

    // ComponentManager integration

    @Override
    public boolean isEnabled(SailingConfig config)
    {
        return config.barracudaGwenithGlideSplits();
    }

    @Override
    public void startUp()
    {
        if (!TIMES_DIR.exists())
        {
            TIMES_DIR.mkdirs(); // creates barracuda-trials-times/gwenith-glide
        }

        reset();

        panel = new SailingPanel();

        BufferedImage icon = ImageUtil.loadImageResource(
                SailingPlugin.class,
                "gwenith_flag.png"
        );

        navButton = NavigationButton.builder()
                .tooltip("Gwenith Glide Times")
                .icon(icon)
                .priority(10)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        panel.updateData(recentRuns, personalBestTicks);
        historyLoaded = false;
    }

    @Override
    public void shutDown()
    {
        reset();
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        panel = null;
        historyLoaded = false;
    }

    // Core logic: start / end / splits

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        // lazy-load history once name is known
        if (!historyLoaded && client.getLocalPlayer() != null)
        {
            loadHistoryFromFile();
            historyLoaded = true;
        }

        int startTime = client.getVarpValue(VarPlayerID.SAILING_BT_TIME_START);

        // run starts
        if (!started && startTime != 0)
        {
            reset();
            started = true;
            log.debug("Gwenith Glide: run started");
            return;
        }

        // run ends or is reset (teleport back, overlay removed, etc)
        if (started && startTime == 0)
        {
            int total = currentTime >= 0 ? currentTime + 1 : -1;

            if (portalIndex > 1)
            {
                log.debug("Run finished, total time = " + total + " ticks");
            }
            else
            {
                log.debug("Run reset before completing any portals.");
            }

//            for (String s : splits)
//            {
//                log.debug(s);
//            }
            reset();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }

        String msg = Text.removeTags(event.getMessage());
        // Gwenith Glide KC message
        if (msg.startsWith("Your Gwenith Glide completion count is:"))
        {
            if (!started)
            {
                return;
            }
            int kc = parseKc(msg);
            handleCourseCompleted(kc);
        }
    }

    private int parseKc(String msg)
    {
        try
        {
            int colonIdx = msg.indexOf(':');
            if (colonIdx == -1)
            {
                return -1;
            }
            String afterColon = msg.substring(colonIdx + 1).trim();
            return Integer.parseInt(afterColon);
        }
        catch (Exception e)
        {
            return -1;
        }
    }

    private void handleCourseCompleted(int kc)
    {
        int totalTicks = currentTime >= 0 ? currentTime + 1 : -1;

        addRunToHistory(kc, totalTicks, splits);
        exportTimes(kc, totalTicks, splits);

        reset();
    }

    private void addRunToHistory(int kc, int totalTicks, List<String> splits)
    {
        RunRecord rec = new RunRecord(kc, totalTicks, new ArrayList<>(splits));
        recentRuns.add(0, rec); // newest first

        if (recentRuns.size() > MAX_RECENT_RUNS)
        {
            recentRuns.remove(recentRuns.size() - 1);
        }

        if (totalTicks > 0 && (personalBestTicks == -1 || totalTicks < personalBestTicks))
        {
            personalBestTicks = totalTicks;
        }

        if (panel != null)
        {
            panel.updateData(recentRuns, personalBestTicks);
        }
    }

    private void exportTimes(int kc, int totalTicks, List<String> splitsToWrite)
    {
        String playerName = client.getLocalPlayer() != null
                ? client.getLocalPlayer().getName()
                : "unknown";

        File file = new File(TIMES_DIR, playerName + "_times.txt");

        try (FileWriter writer = new FileWriter(file, true))
        {
            writer.write("KC " + (kc >= 0 ? kc : "?") + " - total: " + totalTicks + " ticks\n");
            for (String line : splitsToWrite)
            {
                writer.write(line);
                writer.write('\n');
            }
            writer.write("----------------------------------------\n");
        }
        catch (IOException ignored)
        {
        }
    }

    // Script hook: time + portal splits

    @Subscribe
    private void onScriptPreFired(ScriptPreFired event)
    {
        int scriptId = event.getScriptId();

        if (scriptId == GT_UI_SCRIPT_ID)
        {
            updateCurrentTimeFromScript(event);
            return;
        }

        // Portal overlay created -> log split
        if (scriptId == CREATE_PORTAL_OVERLAY_SCRIPT_ID)
        {
            handlePortalSplit();
        }
    }

    private void updateCurrentTimeFromScript(ScriptPreFired event)
    {
        if (!started)
        {
            return;
        }

        int startTime = client.getVarpValue(VarPlayerID.SAILING_BT_TIME_START);
        if (startTime == 0)
        {
            return;
        }

        Object[] args = event.getScriptEvent().getArguments();
        if (args == null || args.length == 0)
        {
            return;
        }

        int now = (int) args[args.length - 1];
        currentTime = now - startTime;
    }

    private void handlePortalSplit()
    {
        if (!started || currentTime < 0)
        {
            return;
        }

        int delta = currentTime - lastSplitTime;
        String msg = "Portal " + portalIndex + " time: " + currentTime + " ticks (" + delta + ")";

        splits.add(msg);
        infoPrint(msg);
        // we can also add this to an overlay to live update on screen if needed,
        // splits contains a list of all portal splits

        lastSplitTime = currentTime;
        portalIndex++;
    }

    // ---------------------------------------------------------------------
    // Reset / history load / printing
    // ---------------------------------------------------------------------

    private void reset()
    {
        started = false;
        portalIndex = 1;
        currentTime = -1;
        lastSplitTime = 0;
        splits.clear();
    }

    private void infoPrint(String message)
    {
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
    }

    private void loadHistoryFromFile()
    {
        String playerName = client.getLocalPlayer() != null
                ? client.getLocalPlayer().getName()
                : null;

        if (playerName == null)
        {
            return;
        }

        File file = new File(TIMES_DIR, playerName + "_times.txt");
        if (!file.exists())
        {
            return;
        }

        List<RunRecord> allRuns = new ArrayList<>();

        RunRecord currentRun = null;
        List<String> currentSplits = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (line.isEmpty())
                {
                    continue;
                }

                if (line.startsWith("KC "))
                {
                    if (currentRun != null)
                    {
                        allRuns.add(currentRun);
                    }

                    try
                    {
                        int dashIdx = line.indexOf(" - total:");
                        if (dashIdx == -1)
                        {
                            currentRun = null;
                            currentSplits = null;
                            continue;
                        }

                        String kcPart = line.substring(3, dashIdx).trim();
                        int kc = kcPart.equals("?") ? -1 : Integer.parseInt(kcPart);

                        int ticksIdx = line.indexOf("total:");
                        int ticksEndIdx = line.indexOf("ticks", ticksIdx);
                        if (ticksIdx == -1 || ticksEndIdx == -1)
                        {
                            currentRun = null;
                            currentSplits = null;
                            continue;
                        }

                        String ticksPart = line.substring(ticksIdx + "total:".length(), ticksEndIdx).trim();
                        int totalTicks = Integer.parseInt(ticksPart);

                        currentSplits = new ArrayList<>();
                        currentRun = new RunRecord(kc, totalTicks, currentSplits);
                    }
                    catch (Exception ignored)
                    {
                        currentRun = null;
                        currentSplits = null;
                    }

                    continue;
                }

                if (line.startsWith("---"))
                {
                    if (currentRun != null)
                    {
                        allRuns.add(currentRun);
                        currentRun = null;
                        currentSplits = null;
                    }
                    continue;
                }

                if (currentRun != null && currentSplits != null)
                {
                    currentSplits.add(line);
                }
            }

            if (currentRun != null)
            {
                allRuns.add(currentRun);
            }
        }
        catch (IOException e)
        {
            log.warn("Failed to load Gwenith Glide history", e);
        }

        recentRuns.clear();

        List<RunRecord> target =
                allRuns.size() > MAX_RECENT_RUNS
                        ? allRuns.subList(allRuns.size() - MAX_RECENT_RUNS, allRuns.size())
                        : allRuns;

        // newest at index 0
        for (int i = target.size() - 1; i >= 0; i--)
        {
            recentRuns.add(target.get(i));
        }

        personalBestTicks = -1;
        for (RunRecord r : allRuns)
        {
            if (r.totalTicks > 0 && (personalBestTicks == -1 || r.totalTicks < personalBestTicks))
            {
                personalBestTicks = r.totalTicks;
            }
        }

        if (panel != null)
        {
            panel.updateData(recentRuns, personalBestTicks);
        }

        log.debug("Loaded {} Gwenith Glide runs; PB={}", allRuns.size(), personalBestTicks);
    }
}
