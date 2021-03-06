package org.bukkit.craftbukkit.block;

import net.minecraft.server.BlockFurnace;
import net.minecraft.server.TileEntityFurnace;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.craftbukkit.inventory.CraftInventoryFurnace;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.inventory.FurnaceInventory;

public class CraftFurnace extends CraftContainer<TileEntityFurnace> implements Furnace {

    public CraftFurnace(final Block block) {
        super(block, TileEntityFurnace.class);
    }

    public CraftFurnace(final Material material, final TileEntityFurnace te) {
        super(material, te);
    }

    @Override
    public FurnaceInventory getSnapshotInventory() {
        return new CraftInventoryFurnace(this.getSnapshot());
    }

    @Override
    public FurnaceInventory getInventory() {
        if (!this.isPlaced()) {
            return this.getSnapshotInventory();
        }

        return new CraftInventoryFurnace(this.getTileEntity());
    }

    @Override
    public short getBurnTime() {
        return (short) this.getSnapshot().getProperty(0);
    }

    @Override
    public void setBurnTime(short burnTime) {
        this.getSnapshot().setProperty(0, burnTime);
        // SPIGOT-844: Allow lighting and relighting using this API
        this.data = this.data.set(BlockFurnace.LIT, burnTime > 0);
    }

    @Override
    public short getCookTime() {
        return (short) this.getSnapshot().getProperty(2);
    }

    @Override
    public void setCookTime(short cookTime) {
        this.getSnapshot().setProperty(2, cookTime);
    }

    @Override
    public int getCookTimeTotal() {
        return this.getSnapshot().getProperty(3);
    }

    @Override
    public void setCookTimeTotal(int cookTimeTotal) {
        this.getSnapshot().setProperty(3, cookTimeTotal);
    }

    @Override
    public String getCustomName() {
        TileEntityFurnace furnace = this.getSnapshot();
        return furnace.hasCustomName() ? CraftChatMessage.fromComponent(furnace.getCustomName()) : null;
    }

    @Override
    public void setCustomName(String name) {
        this.getSnapshot().setCustomName(CraftChatMessage.fromStringOrNull(name));
    }

    @Override
    public void applyTo(TileEntityFurnace furnace) {
        super.applyTo(furnace);

        if (!this.getSnapshot().hasCustomName()) {
            furnace.setCustomName(null);
        }
    }

    // Paper start - cook speed multiplier API
    @Override
    public double getCookSpeedMultiplier() {
        return this.getSnapshot().cookSpeedMultiplier;
    }

    @Override
    public void setCookSpeedMultiplier(double multiplier) {
        com.google.common.base.Preconditions.checkArgument(multiplier >= 0, "Furnace speed multiplier cannot be negative");
        com.google.common.base.Preconditions.checkArgument(multiplier <= 200, "Furnace speed multiplier cannot more than 200");
        this.getSnapshot().cookSpeedMultiplier = multiplier;
    }
    // Paper end
}
