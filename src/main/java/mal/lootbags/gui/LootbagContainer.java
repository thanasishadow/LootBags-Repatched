package mal.lootbags.gui;

import mal.lootbags.LootBags;
import mal.lootbags.item.LootbagItem;
import mal.lootbags.network.LootbagWrapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class LootbagContainer extends Container{

	public LootbagWrapper wrapper;
	public InventoryPlayer player;
	private int islot;
	
	public LootbagContainer(InventoryPlayer player, LootbagWrapper wrap)
	{
		wrapper=wrap;
		this.player=player;
		islot = player.currentItem;
		
		int hOffset = 8;
		int vOffset = 8;
		int hSpacing = 18;
		int vSpacing = 18;
		
		for(int i = 0; i < wrap.getSizeInventory(); i++)
		{
			if(i < 10)
				this.addSlotToContainer(new LootbagSlot(wrapper,i,hOffset+i*hSpacing, vOffset));
			else
				this.addSlotToContainer(new LootbagSlot(wrapper,i,(hOffset+i*hSpacing)-((wrap.getSizeInventory()/2)*hSpacing), vOffset+vSpacing));
		}
		
		//main inventory
		
		int mainInvOffsetX = 9;
		int mainInvOffsetY = 16;
		
        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
            	if(LootBags.areItemStacksEqualItem(player.getStackInSlot(j+i*9+9), wrapper.getStack(), true, false))
            		this.addSlotToContainer(new FixedSlot(player, j + i * 9 + 9, mainInvOffsetX + (8 + j * 18), mainInvOffsetY + (46 + i*18)));
            	else
            		this.addSlotToContainer(new Slot(player, j + i * 9 + 9, mainInvOffsetX + (8 + j * 18), mainInvOffsetY +(46 + i * 18)));
            }
        }

        //hotbar, so 45-53
        for (int i = 0; i < 9; ++i)
        {
        	if(LootBags.areItemStacksEqualItem(player.getStackInSlot(i), wrapper.getStack(), true, false))
        		this.addSlotToContainer(new FixedSlot(player, i, mainInvOffsetX + (8 + i * 18), mainInvOffsetY + 103));
        	else
        		this.addSlotToContainer(new Slot(player, i, mainInvOffsetX + (8 + i * 18), mainInvOffsetY + 103));
        }
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer p_75145_1_) {
		if(player.getItemStack()!=null && !player.getItemStack().isEmpty())
			return true;//LootBags.areItemStacksEqualItem(player.mainInventory[islot], wrapper.getStack(), true, false);
		return wrapper.isUsableByPlayer(p_75145_1_); //&& LootBags.areItemStacksEqualItem(player.mainInventory[islot], wrapper.getStack(), true, false);
	}

	@Override
	public void detectAndSendChanges()
    {	
		super.detectAndSendChanges();
		if(LootBags.areItemStacksEqualItem(player.mainInventory.get(islot), wrapper.getStack(), true, false))
		{
			if(LootbagItem.checkInventory(wrapper.getStack()))
			{
				player.mainInventory.set(islot, ItemStack.EMPTY);
			}
			else
			{
				player.mainInventory.set(islot, wrapper.getStack());
			}
		}
    }
	
	@Override
	public void onContainerClosed(EntityPlayer player)
	{
		if(!player.world.isRemote)
		{
			if(LootBags.areItemStacksEqualItem(player.inventory.mainInventory.get(islot), wrapper.getStack(), true, false))
			{
				if(LootbagItem.checkInventory(wrapper.getStack()))
				{
					player.inventory.mainInventory.set(islot, ItemStack.EMPTY);
				}
				else
				{
					player.inventory.mainInventory.set(islot, wrapper.getStack());
				}
			}
		}
		super.onContainerClosed(player);
	}
	
	/**
     * Called when a player shift-clicks on a slot. You must override this or you will crash when someone does that.
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int slot)
    {
    	ItemStack var3 = ItemStack.EMPTY;
    	Slot var4 = null;
    	if(this.inventorySlots.get(slot) instanceof FixedSlot)
    		var4 = this.inventorySlots.get(slot);
    	else
    		var4 = this.inventorySlots.get(slot);

        if (var4 != null && var4.getHasStack())
        {
            ItemStack var5 = var4.getStack();
            var3 = var5.copy();

            if(slot>=0 && slot <5)//inventory
            {
            	if (!this.mergeItemStack(var5, 5, 41, true))
                {
                    return ItemStack.EMPTY;
                }

                var4.onSlotChange(var5, var3);

            }
            
            if (var5.getCount() == 0)
            {
                var4.putStack(ItemStack.EMPTY);
            }
            else
            {
                var4.onSlotChanged();
            }

            if (var5.getCount() == var3.getCount())
            {
                return ItemStack.EMPTY;
            }

            var4.onTake(par1EntityPlayer, var5);
        }
        
        return var3;
    }
}
/*******************************************************************************
 * Copyright (c) 2018 Malorolam.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the included license.
 * 
 *********************************************************************************/