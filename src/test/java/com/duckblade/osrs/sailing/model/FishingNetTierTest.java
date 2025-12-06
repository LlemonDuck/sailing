package com.duckblade.osrs.sailing.model;

import net.runelite.api.gameval.ObjectID;
import org.junit.Assert;
import org.junit.Test;

public class FishingNetTierTest {

	@Test
	public void testFromGameObjectId_ropeNet() {
		Assert.assertEquals(FishingNetTier.Rope, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_ROPE_TRAWLING_NET));
	}

	@Test
	public void testFromGameObjectId_ropeNetPort() {
		Assert.assertEquals(FishingNetTier.Rope, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_ROPE_TRAWLING_NET_3X8_PORT));
	}

	@Test
	public void testFromGameObjectId_ropeNetStarboard() {
		Assert.assertEquals(FishingNetTier.Rope, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_ROPE_TRAWLING_NET_3X8_STARBOARD));
	}

	@Test
	public void testFromGameObjectId_linenNet() {
		Assert.assertEquals(FishingNetTier.Linen, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_LINEN_TRAWLING_NET));
	}

	@Test
	public void testFromGameObjectId_linenNetPort() {
		Assert.assertEquals(FishingNetTier.Linen, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_LINEN_TRAWLING_NET_3X8_PORT));
	}

	@Test
	public void testFromGameObjectId_linenNetStarboard() {
		Assert.assertEquals(FishingNetTier.Linen, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_LINEN_TRAWLING_NET_3X8_STARBOARD));
	}

	@Test
	public void testFromGameObjectId_hempNet() {
		Assert.assertEquals(FishingNetTier.Hemp, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_HEMP_TRAWLING_NET));
	}

	@Test
	public void testFromGameObjectId_hempNetPort() {
		Assert.assertEquals(FishingNetTier.Hemp, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_HEMP_TRAWLING_NET_3X8_PORT));
	}

	@Test
	public void testFromGameObjectId_hempNetStarboard() {
		Assert.assertEquals(FishingNetTier.Hemp, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_HEMP_TRAWLING_NET_3X8_STARBOARD));
	}

	@Test
	public void testFromGameObjectId_cottonNet() {
		Assert.assertEquals(FishingNetTier.Cotton, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_COTTON_TRAWLING_NET));
	}

	@Test
	public void testFromGameObjectId_cottonNetPort() {
		Assert.assertEquals(FishingNetTier.Cotton, 
			FishingNetTier.fromGameObjectId(ObjectID.SAILING_COTTON_TRAWLING_NET_3X8_PORT));
	}

	@Test
	public void testFromGameObjectId_cottonNetStarboard() {
		Assert.assertEquals(FishingNetTier.Cotton, 
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
		Assert.assertEquals(125, FishingNetTier.Rope.getCapacity());
		Assert.assertEquals(125, FishingNetTier.Linen.getCapacity());
		Assert.assertEquals(125, FishingNetTier.Hemp.getCapacity());
		Assert.assertEquals(125, FishingNetTier.Cotton.getCapacity());
	}

	@Test
	public void testGetGameObjectIds_ropeHasThreeIds() {
		Assert.assertEquals(3, FishingNetTier.Rope.getGameObjectIds().length);
	}

	@Test
	public void testGetGameObjectIds_linenHasThreeIds() {
		Assert.assertEquals(3, FishingNetTier.Linen.getGameObjectIds().length);
	}

	@Test
	public void testGetGameObjectIds_hempHasThreeIds() {
		Assert.assertEquals(3, FishingNetTier.Hemp.getGameObjectIds().length);
	}

	@Test
	public void testGetGameObjectIds_cottonHasThreeIds() {
		Assert.assertEquals(3, FishingNetTier.Cotton.getGameObjectIds().length);
	}

	@Test
	public void testAllTiersExist() {
		FishingNetTier[] tiers = FishingNetTier.values();
		Assert.assertEquals(4, tiers.length);
		Assert.assertEquals(FishingNetTier.Rope, tiers[0]);
		Assert.assertEquals(FishingNetTier.Linen, tiers[1]);
		Assert.assertEquals(FishingNetTier.Hemp, tiers[2]);
		Assert.assertEquals(FishingNetTier.Cotton, tiers[3]);
	}
}
