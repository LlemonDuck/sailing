package com.duckblade.osrs.sailing.features;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.Scene;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LowDetail
        implements PluginLifecycleComponent, RenderCallback
{
    private static final Set<Integer> GAME_OBJECTS_TO_HIDE = ImmutableSet.of(
            ObjectID.SAILING_HAZARD_FETID_WATERS_AMBIENT_3X3_FAST,
            ObjectID.SAILING_HAZARD_FETID_WATERS_AMBIENT_3X3_SLOW,
            ObjectID.SAILING_HAZARD_FETID_WATERS_AMBIENT_1X1,
            ObjectID.OCEAN_OUTCROP_SEAWEED01,
            ObjectID.OCEAN_OUTCROP_SEAWEED02,
            ObjectID.OCEAN_OUTCROP_SEAWEED03,

            // Visible from Jubbly Jive Course but on land
            // Attempt to mitigate as much as possible from rendering while sailing
            ObjectID.SWAMPBUBBLES,
            ObjectID.REGICIDE_SWAMP_BUBBLE1,
            ObjectID.WATERFALL_FOAM,
            ObjectID.WATERFALL_FOAM_BIG,
            ObjectID.WATERFALL_FOAM_SMALL,

            35912, // ObjectID.FIRE_35912 - not resolving
            35913 // ObjectID.FIRE_35913 - not resolving
    );

    private final SailingConfig config;
    private final RenderCallbackManager renderCallbackManager;

    @Override
    public void startUp()
    {
        renderCallbackManager.register(this);
    }

    @Override
    public void shutDown()
    {
        renderCallbackManager.unregister(this);
    }

    @Override
    public boolean isEnabled(SailingConfig config)
    {
        return config.enableLowDetail();
    }

    @Override
    public boolean drawObject(Scene scene, TileObject object)
    {
        if (!config.enableLowDetail())
        {
            return true;
        }

        if (object instanceof GameObject)
        {
            GameObject gameObject = (GameObject) object;
            return !GAME_OBJECTS_TO_HIDE.contains(gameObject.getId());
        }

        return true;
    }
}
