package mal.lootbags.gui;

import mal.lootbags.item.LootbagItem;
import mal.lootbags.tileentity.TileEntityStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class StorageContainer extends Container{

    private static final int OUTPUT_SLOT = 0;
    private static final int INPUT_SLOT = 1;

    TileEntityStorage bench;

    public StorageContainer(InventoryPlayer player, TileEntityStorage te)
    {
        bench = te;

        this.addSlotToContainer(new LootbagSlot(te, OUTPUT_SLOT, 135, 16));

        this.addSlotToContainer(new StorageSlot(te, INPUT_SLOT, 26, 16));

        drawPlayerInventory(player);
    }

    private void drawPlayerInventory(InventoryPlayer player) {
        //main inventory, so 18-44
        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
                this.addSlotToContainer(new Slot(player, j + i * 9+9, 8 + j * 18, 66 + i * 18));
            }
        }

        //hotbar, so 45-53
        for (int i = 0; i < 9; ++i)
        {
            this.addSlotToContainer(new Slot(player, i, 8 + i * 18, 123));
        }
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer playerIn) {
        return bench.isUsableByPlayer(playerIn);
    }

    @Override
    @Nonnull
    public ItemStack slotClick(int slotPos, int pressedKey, @Nonnull ClickType clickTypeIn, @Nonnull EntityPlayer player) {
        if (slotPos < 0 || slotPos >= inventorySlots.size()) {
            return super.slotClick(slotPos, pressedKey, clickTypeIn, player);
        }
        Slot selectedSlot = inventorySlots.get(slotPos);
        if (clickTypeIn == ClickType.SWAP && slotPos == OUTPUT_SLOT && pressedKey >= 0 && pressedKey < 9) {
            if (player.inventory.getStackInSlot(pressedKey).isEmpty() && selectedSlot.getHasStack()) {
                ItemStack lootBagToGive = selectedSlot.getStack();
                lootBagToGive.setCount(1);
                player.inventory.setInventorySlotContents(pressedKey, lootBagToGive);
                bench.decrStorage(lootBagToGive);
                return lootBagToGive;
            }
        }
        return super.slotClick(slotPos, pressedKey, clickTypeIn, player);
    }
    /**
     * Called when a player shift-clicks on a slot. You must override this or you will crash when someone does that.
     */
    @Override
    @Nonnull
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int slotPos)
    {
        // I have no idea what the return value is supposed to be used for.
        // Returning EMPTY every time seems to work at least for the basic use case.
        if (slotPos == INPUT_SLOT) {
            return ItemStack.EMPTY;
        }

        Slot clickedSlot = this.inventorySlots.get(slotPos);
        if (clickedSlot == null || !clickedSlot.getHasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack clickedStack = clickedSlot.getStack();
        if (!(clickedStack.getItem() instanceof LootbagItem)) {
            return ItemStack.EMPTY;
        }

        if (slotPos == OUTPUT_SLOT) {
            clickedStack.setCount(1);
            if (mergeItemStack(clickedStack, 2, 38, true)) {
                clickedStack.setCount(1);
                bench.decrStorage(clickedStack);
            }
            return ItemStack.EMPTY;
        }

        bench.incrementStorage(clickedStack);
        clickedSlot.putStack(ItemStack.EMPTY);

        return ItemStack.EMPTY;
    }
}
/*******************************************************************************
 * Copyright (c) 2018 Malorolam.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the included license.
 *
 *********************************************************************************/