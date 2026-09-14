package com.mattmc.nijntje.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mattmc.nijntje.GameBootstrap;
import com.mattmc.nijntje.entity.NijntjeEntity.GrowthStage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The growth ladder, the one piece of the bunny's progression that needs no world. The stage is
 * saved as an int and read back through {@link GrowthStage#byId}, so what that does with a value
 * outside the ladder (an older save, a corrupted one) decides whether the bunny loads at all.
 */
class GrowthStageTest {
    @BeforeAll
    static void boot() {
        // NijntjeEntity's static initialiser defines synched-data ids, which needs the game's
        // registries even though no entity is ever constructed here.
        GameBootstrap.content();
    }

    @Test
    void ladderIsBabyYoungAdultGrown() {
        assertEquals(4, GrowthStage.values().length);
        assertSame(GrowthStage.BABY, GrowthStage.values()[0]);
        assertSame(GrowthStage.GROWN, GrowthStage.values()[GrowthStage.values().length - 1]);
    }

    @Test
    void byIdClampsInsteadOfThrowing() {
        assertSame(GrowthStage.BABY, GrowthStage.byId(-1), "a negative id must not crash a world load");
        assertSame(GrowthStage.GROWN, GrowthStage.byId(99), "an id past the ladder must not crash a world load");
        for (final GrowthStage stage : GrowthStage.values()) {
            assertSame(stage, GrowthStage.byId(stage.ordinal()), "byId must be the inverse of ordinal()");
        }
    }

    @Test
    void eachStageIsBiggerThanTheLast() {
        float previous = 0.0F;
        for (final GrowthStage stage : GrowthStage.values()) {
            assertTrue(stage.scale() > previous, stage + " is not larger than the stage before it");
            previous = stage.scale();
        }
        // The barding, the rider's seat and the hitbox are all sized for an adult of 1.0 or so;
        // a baby must be clearly smaller and a grown one clearly larger.
        assertTrue(GrowthStage.BABY.scale() < 1.0F);
        assertTrue(GrowthStage.GROWN.scale() > 1.0F);
    }

    @Test
    void isAtLeastFollowsTheLadder() {
        assertTrue(GrowthStage.GROWN.isAtLeast(GrowthStage.BABY));
        assertTrue(GrowthStage.ADULT.isAtLeast(GrowthStage.ADULT));
        assertFalse(GrowthStage.YOUNG.isAtLeast(GrowthStage.ADULT));
        assertFalse(GrowthStage.BABY.isAtLeast(GrowthStage.YOUNG));
    }
}
