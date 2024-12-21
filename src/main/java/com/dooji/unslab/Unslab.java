package com.dooji.unslab;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Unslab implements ModInitializer {
	public static final String MOD_ID = "unslab";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[Unslab] Initializing Unslab...");

		UnslabMapping.initialize();

		LOGGER.info("[Unslab] Unslab has finished initializing!");
	}
}