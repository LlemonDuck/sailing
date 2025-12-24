package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.ComponentManager;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.inject.Provider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class ShoalPathTrackerCommand implements PluginLifecycleComponent {

	private static final String COMMAND_NAME = "trackroutes";
	
	private final ChatMessageManager chatMessageManager;
	private final Provider<ComponentManager> componentManagerProvider;
	private final boolean developerMode;
	@Getter
    private boolean tracingEnabled = false;

	@Inject
	public ShoalPathTrackerCommand(ChatMessageManager chatMessageManager, Provider<ComponentManager> componentManagerProvider, @Named("developerMode") boolean developerMode) {
		this.chatMessageManager = chatMessageManager;
		this.componentManagerProvider = componentManagerProvider;
		this.developerMode = developerMode;
	}

	@Override
	public boolean isEnabled(SailingConfig config) {
		// Only available in developer mode
		return developerMode;
	}

	@Override
	public void startUp() {
		log.debug("Route tracing command available: ::" + COMMAND_NAME);
	}

	@Override
	public void shutDown() {
		tracingEnabled = false;
		log.debug("Route tracing command disabled");
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted commandExecuted) {
		if (!COMMAND_NAME.equalsIgnoreCase(commandExecuted.getCommand())) {
			return;
		}

		String[] arguments = commandExecuted.getArguments();
		
		if (arguments.length == 0) {
			// Toggle
			tracingEnabled = !tracingEnabled;
		} else {
			// Explicit on/off
			String arg = arguments[0].trim().toLowerCase();
			if (arg.equals("on") || arg.equals("true") || arg.equals("1")) {
				tracingEnabled = true;
			} else if (arg.equals("off") || arg.equals("false") || arg.equals("0")) {
				tracingEnabled = false;
			} else {
				sendChatMessage("Usage: ::trackroutes [on|off] - Current status: " + (tracingEnabled ? "ON" : "OFF"));
				return;
			}
		}
		
		sendChatMessage("Shoal route tracing is now " + (tracingEnabled ? "ENABLED" : "DISABLED"));
		log.debug("Shoal route tracing is now {}", tracingEnabled ? "ENABLED" : "DISABLED");
		
		// Trigger component manager to re-check all component states
		componentManagerProvider.get().revalidateComponentStates();
	}

	private void sendChatMessage(String message) {
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.value(message)
			.build());
	}

}
