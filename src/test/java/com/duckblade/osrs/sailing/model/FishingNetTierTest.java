package com.duckblade.osrs.sailing.model;

import net.runelite.api.gameval.ObjectID;
import org.junit.Assert;
import org.junit.Test;

public class FishingNetTierTest {

	@Test
	public void testFromGameObjectId_ropeNet() {
		Assert.assertEquals(FishingNetTier.ROPE, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_ROPE_TRAWLING_NET));
	}

	@Test
	public void testFromGameObjectId_ropeNetPort() {
		Assert.assertEquals(FishingNetTier.ROPE, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_ROPE_TRAWLING_NET_3X8_PORT));
	}

	@Test
	public void testFromGameObjectId_ropeNetStarboard() {
		Assert.assertEquals(FishingNetTier.ROPE, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_ROPE_TRAWLING_NET_3X8_STARBOARD));
	}

	@Test
	public void testFromGameObjectId_linenNet() {
		Assert.assertEquals(FishingNetTier.LINEN, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_LINEN_TRAWLING_NET));
	}

	@Test
	public void testFromGameObjectId_linenNetPort() {
		Assert.assertEquals(FishingNetTier.LINEN, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_LINEN_TRAWLING_NET_3X8_PORT));
	}

	@Test
	public void testFromGameObjectId_linenNetStarboard() {
		Assert.assertEquals(FishingNetTier.LINEN, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_LINEN_TRAWLING_NET_3X8_STARBOARD));
	}

	@Test
	public void testFromGameObjectId_hempNet() {
		Assert.assertEquals(FishingNetTier.HEMP, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_HEMP_TRAWLING_NET));
	}

	@Test
	public void testFromGameObjectId_hempNetPort() {
		Assert.assertEquals(FishingNetTier.HEMP, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_HEMP_TRAWLING_NET_3X8_PORT));
	}

	@Test
	public void testFromGameObjectId_hempNetStarboard() {
		Assert.assertEquals(FishingNetTier.HEMP, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_HEMP_TRAWLING_NET_3X8_STARBOARD));
	}

	@Test
	public void testFromGameObjectId_cottonNet() {
		Assert.assertEquals(FishingNetTier.COTTON, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_COTTON_TRAWLING_NET));
	}

	@Test
	public void testFromGameObjectId_cottonNetPort() {
		Assert.assertEquals(FishingNetTier.COTTON, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_COTTON_TRAWLING_NET_3X8_PORT));
	}

	@Test
	public void testFromGameObjectId_cottonNetStarboard() {
		Assert.assertEquals(FishingNetTier.COTTON, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_COTTON_TRAWLING_NET_3X8_STARBOARD));
	}

	@Test
	public void testFromGameObjectId_invalidId_returnsNull() {
		Assert.assertNull(FishingNetTier.fromGameObjectId(12345));
	}

	@Test
	public void testFromGameObjectId_allTiers() {
		// Test that all tiers can be found
		Assert.assertNotNull(FishingNetTier.fromGameObjectId(ObjectID.SAILING_ROPE_TRAWLING_NET));
		Assert.assertNotNull(FishingNetTier.fromGameObjectId(ObjectID.SAILING_LINEN_TRAWLING_NET));
		Assert.assertNotNull(FishingNetTier.fromGameObjectId(ObjectID.SAILING_HEMP_TRAWLING_NET));
		Assert.assertNotNull(FishingNetTier.fromGameObjectId(ObjectID.SAILING_COTTON_TRAWLING_NET));
	}

	@Test
	public void testGetCapacity_allTiers() {
		// Currently all tiers return 125
		// This test documents the current behavior
		Assert.assertEquals(125, FishingNetTier.ROPE.getCapacity());
		Assert.assertEquals(125, FishingNetTier.LINEN.getCapacity());
		Assert.assertEquals(125, FishingNetTier.HEMP.getCapacity());
		Assert.assertEquals(125, FishingNetTier.COTTON.getCapacity());
	}

	@Test
	public void testGetGameObjectIds_ropeHasThreeIds() {
		Assert.assertEquals(3, FishingNetTier.ROPE.getGameObjectIds().length);
	}

	@Test
	public void testGetGameObjectIds_linenHasThreeIds() {
		Assert.assertEquals(3, FishingNetTier.LINEN.getGameObjectIds().length);
	}

	@Test
	public void testGetGameObjectIds_hempHasThreeIds() {
		Assert.assertEquals(3, FishingNetTier.HEMP.getGameObjectIds().length);
	}

	@Test
	public void testGetGameObjectIds_cottonHasThreeIds() {
		Assert.assertEquals(3, FishingNetTier.COTTON.getGameObjectIds().length);
	}

	@Test
	public void testAllTiersExist() {
		FishingNetTier[] tiers = FishingNetTier.values();
		Assert.assertEquals(4, tiers.length);
		Assert.assertEquals(FishingNetTier.ROPE, tiers[0]);
		Assert.assertEquals(FishingNetTier.LINEN, tiers[1]);
		Assert.assertEquals(FishingNetTier.HEMP, tiers[2]);
		Assert.assertEquals(FishingNetTier.COTTON, tiers[3]);
	}
}
