package com.duckblade.osrs.sailing.features.util;

import com.duckblade.osrs.sailing.model.BarracudaTrials.Course;
import com.duckblade.osrs.sailing.model.BarracudaTrials.TrialRank;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BarracudaTrialTracker implements PluginLifecycleComponent
{
    // Tempor Tantrum Course Components
    private static final int TT_RUM_WIDGET_ID = WidgetUtil.packComponentId(931, 9);
    private static final int TT_RUM_COLLECTED_SPRITE_ID = 7022;
    private static final int TT_RUM_EMPTY_SPRITE_ID = 7023;

    // TODO: Gather and implement Jubbly Jive and Gwenith Glide Course Components that may be needed.

    private final Client client;

    @Getter
    private boolean inTrial;

    @Getter
    private Course course;

    @Getter
    private TrialRank trialRank;

    @Getter
    private boolean rumCollected;

    public void shutDown()
    {
        reset();
    }

    private void reset()
    {
        inTrial = false;
        course = null;
        trialRank = null;
        rumCollected = false;
        log.debug("reset trial tracker");
    }

    @Subscribe
    private void onVarbitChanged(VarbitChanged e)
    {
        switch (e.getVarbitId())
        {
            case VarbitID.SAILING_BT_IN_TRIAL:
            {
                if (e.getValue() > 0) // In Trial, regardless of difficulty
                {
                    inTrial = true;
                    trialRank = TrialRank.fromId(e.getValue());
                }
                else
                {
                    // When `SAILING_BT_IN_TRIAL` is fired and set to `0`, consider a course exit and reset.
                    log.debug("exited course: " + getDebugString());
                    inTrial = false;
                    reset();
                }
                break;
            }

            // Master states are fired AFTER `SAILING_BT_IN_TRIAL` is fired.
            case VarbitID.SAILING_BT_TEMPOR_TANTRUM_MASTER_STATE:
            {
                determineState(Course.TEMPOR_TANTRUM, e.getValue());
                break;
            }

            case VarbitID.SAILING_BT_JUBBLY_JIVE_MASTER_STATE:
            {
                determineState(Course.JUBBLY_JIVE, e.getValue());
                break;
            }

            case VarbitID.SAILING_BT_GWENITH_GLIDE_MASTER_STATE:
            {
                determineState(Course.GWENITH_GLIDE, e.getValue());
                break;
            }
        }
    }

    @Subscribe
    private void onGameTick(GameTick e)
    {
        if (!inTrial || course != Course.TEMPOR_TANTRUM)
        {
            return;
        }

        Widget rumWidget = client.getWidget(TT_RUM_WIDGET_ID);
        if (rumWidget != null)
        {
            switch (rumWidget.getSpriteId())
            {
                case TT_RUM_COLLECTED_SPRITE_ID:
                {
                    if (!rumCollected)
                    {
                        log.debug("rum collected set to: true");
                    }

                    rumCollected = true;
                    break;
                }

                case TT_RUM_EMPTY_SPRITE_ID:
                {
                    if (rumCollected)
                    {
                        log.debug("rum collected set to: false");
                    }

                    rumCollected = false;
                    break;
                }
            }
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged e)
    {
        if (e.getGameState() == GameState.LOGIN_SCREEN || e.getGameState() == GameState.HOPPING)
        {
            reset();
        }
    }

    private void determineState(Course course, int value)
    {
        if (value == 2)
        {
            this.course = course;
            log.debug("entered course: " + getDebugString());
        }
    }

    private String getDebugString()
    {
        return String.format(
                "inTrial: %s, Course: %s, TrialRank: %s, RumCollected: %s",
                inTrial,
                course,
                trialRank,
                rumCollected
        );
    }
}
