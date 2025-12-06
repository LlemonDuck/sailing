package com.duckblade.osrs.sailing.debugplugin.module;

import com.duckblade.osrs.sailing.debugplugin.SailingDebugConfig;

public interface DebugLifecycleComponent
{

	default boolean isEnabled(SailingDebugConfig config)
	{
		return true;
	}

	default void startUp()
	{
	}

	default void shutDown()
	{
	}

}
