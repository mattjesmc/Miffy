package com.mattmc.nijntje.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.DyeColor;

@Environment(EnvType.CLIENT)
public class NijntjeRenderState extends LivingEntityRenderState {
    // Growth is expressed through the inherited scale field (from entity.getScale()).
    public boolean saddled;
    public boolean sitting;
    public boolean showBarding;
    public DyeColor dressColor = DyeColor.ORANGE;
}
