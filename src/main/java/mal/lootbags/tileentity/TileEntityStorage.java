package mal.lootbags.tileentity;

import java.util.ArrayList;

import mal.lootbags.LootBags;
import mal.lootbags.handler.BagHandler;
import mal.lootbags.item.LootbagItem;
import mal.lootbags.network.LootbagsPacketHandler;
import mal.lootbags.network.message.StorageMessageClient;
import mal.lootbags.network.message.StorageMessageServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

public class TileEntityStorage extends TileEntity implements IInventory, ISidedInventory, ITickable{

	private int stored_value;
	private int outputID, outputindex;
	private ItemStack input_inventory;
	//private ItemStack output_inventory;
	private ArrayList<Integer> outputIDlist;
	private boolean justRemoved = false;//flag for when the value has just been reduced to allow for bag counts to not mess up
	private NetworkRegistry.TargetPoint point;
	
	public TileEntityStorage()
	{
		input_inventory = ItemStack.EMPTY;
		outputIDlist = new ArrayList<Integer>();
		outputIDlist.addAll(BagHandler.getExtractedBagList());
		outputindex = 0;
		outputID = outputIDlist.get(outputindex);
		//output_inventory = new ItemStack(LootBags.lootbagItem,1,outputID)
	}
	
	public void activate(World world, BlockPos pos, EntityPlayer player) {
		player.openGui(LootBags.LootBagsInstance, 3, world, pos.getX(), pos.getY(), pos.getZ());
	}
	

	@Override
	public void update() {
		if(world != null && !this.world.isRemote)
		{
			if(point==null)
				point = new NetworkRegistry.TargetPoint(world.provider.getDimension(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), 16);
			//make sure the output slot has a bag in it
			/*if(output_inventory == null || output_inventory.isEmpty())
				if(stored_value >= BagHandler.getBagValue(outputID)[1])
				{
					stored_value -= BagHandler.getBagValue(outputID)[1];
					output_inventory = new ItemStack(LootBags.lootbagItem, 1, outputID);
				}
			*/
			LootbagsPacketHandler.instance.sendToAllAround(new StorageMessageServer(this, stored_value, outputID, outputindex), point);
		}
		justRemoved = false;
	}
	
	public void setDataClient(int value, int ID, int index)
	{
		stored_value = value;
		outputID = ID;
		outputindex = index;
	}
	
	public void setDataServer(int ID, int index) {
		outputID = ID;
		outputindex = index;
		this.markDirty();
	}
	
	public void setOutputID(int ID)
	{
		if(!BagHandler.isIDFree(ID))
			outputID = ID;
	}
	
	public void cycleOutputID(boolean direction)
	{
		if(direction==true)
		{
			outputindex++;
			if(outputindex == outputIDlist.size())
				outputindex = 0;
		}
		else
		{
			outputindex--;
			if(outputindex < 0)
				outputindex = outputIDlist.size()-1;
		}
		try {
			outputID = outputIDlist.get(outputindex);
		} catch(IndexOutOfBoundsException e)
		{
			outputID = outputIDlist.get(0);
			outputindex = 0;
		}
		if(world.isRemote)
			LootbagsPacketHandler.instance.sendToServer(new StorageMessageClient(this, outputID, outputindex));
	}
	
	public int getStorage()
	{
		return stored_value;
	}
	
	public ItemStack getOutputStack()
	{
		return new ItemStack(LootBags.lootbagItem, 1, outputID);//output_inventory;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		
		outputID = nbt.getInteger("outputID");
		stored_value = nbt.getInteger("totalValue");
		outputindex = nbt.getInteger("outputindex");
		
		NBTTagList input = nbt.getTagList("inputItems", 10);
		NBTTagCompound var4 = input.getCompoundTagAt(0);
		this.input_inventory = new ItemStack(var4);
		
		//NBTTagList olootbag = nbt.getTagList("outputItem", 10);
		//NBTTagCompound var3 = (NBTTagCompound)olootbag.getCompoundTagAt(0);
		//output_inventory = new ItemStack(var3);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		
		nbt.setInteger("outputID", outputID);
		nbt.setInteger("totalValue", stored_value);
		nbt.setInteger("outputindex", outputindex);
		
		NBTTagList input = new NBTTagList();
		NBTTagCompound var4 = new NBTTagCompound();
		this.input_inventory.writeToNBT(var4);
		input.appendTag(var4);
		nbt.setTag("inputItems", input);
		
		/*NBTTagList olootbag = new NBTTagList();
		if(output_inventory != null && !output_inventory.isEmpty())
		{
			NBTTagCompound var = new NBTTagCompound();
			var.setByte("Slot", (byte)0);
			output_inventory.writeToNBT(var);
			olootbag.appendTag(var);
		}
		nbt.setTag("outputItem", olootbag);*/
		
		return nbt;
	}
	
	public NBTTagCompound getDropNBT()
	{
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("outputID", outputID);
		tag.setInteger("stored_value", stored_value);
		tag.setInteger("outputindex", outputindex);
		
		return tag;
	}

	//Just removes a single bag of whatever is selected, or returns false if it can't
	public boolean removeBag()
	{
		int value = BagHandler.getBagValue(outputID)[1];
		if(stored_value >= value)
		{
			stored_value -= value;
			justRemoved = true;
			return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return "storage";
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		return new int[] {0, 1};
	}

	@Override
	public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
		if(!(itemStackIn.getItem() instanceof LootbagItem))
			return false;
		if(LootBags.PREVENTMERGEDBAGS)
		{
			if (!BagHandler.isBagOpened(itemStackIn) && BagHandler.isBagInsertable(itemStackIn.getMetadata()))
			{
				if(stored_value+BagHandler.getBagValue(itemStackIn.getMetadata())[0] == Integer.MAX_VALUE || stored_value+BagHandler.getBagValue(itemStackIn.getMetadata())[0] < 0)
					return false;
				return true;
			}
		}
		else
		{
			if (BagHandler.isBagInsertable(itemStackIn.getMetadata()))
			{
				if(stored_value+BagHandler.getBagValue(itemStackIn.getMetadata())[0] == Integer.MAX_VALUE || stored_value+BagHandler.getBagValue(itemStackIn.getMetadata())[0] < 0)
					return false;
				return true;
			}
		}
		return false;
			
	}

	@Override
	public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
		return true;
	}

	@Override
	public int getSizeInventory() {
		return 2;
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		if(index==0)
		{
			if(stored_value >= BagHandler.getBagValue(outputID)[1] || justRemoved)
			{
				//stored_value -= BagHandler.getBagValue(outputID)[1];
				if(LootBags.STOREDCOUNT) {
					if(LootBags.MEKOVERRIDE) {
						String ss = (new Throwable()).getStackTrace()[1].getClassName();
						if (ss.startsWith("mekanism"))//make it so that Mekanism literally cannot pull out more than one bag
							return new ItemStack(LootBags.lootbagItem, 1, outputID);
					}
					return new ItemStack(LootBags.lootbagItem, (int) Math.floor(stored_value / BagHandler.getBagValue(outputID)[1]), outputID);
				}
				else
					return new ItemStack(LootBags.lootbagItem, 1, outputID);
			}
			else
				return ItemStack.EMPTY;
		}
		else
			return input_inventory;
	}

	@Override
	public ItemStack decrStackSize(int slot, int dec) {
		if(slot == 0) // Output
		{
			int bagValue = BagHandler.getBagValue(outputID)[1];
			int bagsToRemove = Math.min(dec, stored_value / bagValue);
			if (bagsToRemove == 0) {
				return ItemStack.EMPTY;
			}
			stored_value -= bagValue * bagsToRemove;
			return new ItemStack(LootBags.lootbagItem, bagsToRemove, outputID);
		}
		else
		{
			if(input_inventory != null)
			{
				ItemStack is;
				if(input_inventory.getCount() <= dec)
				{
					is = input_inventory;
					input_inventory = ItemStack.EMPTY;
					return is;
				}
				else
				{
					is = input_inventory.splitStack(dec);
					if(input_inventory.getCount() == 0)
						input_inventory = ItemStack.EMPTY;
					return is;
				}
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		if(index==0)
		{
			int value = BagHandler.getBagValue(outputID)[1];
			if(stored_value >= value)
			{
				stored_value -= value;
				return new ItemStack(LootBags.lootbagItem, 1, outputID);
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		//if(world != null && !this.world.isRemote)
		if(stack == null || stack.isEmpty() || !(stack.getItem() instanceof LootbagItem)) {//edited to try and fix dupe issue with Mekanism
			if(stored_value < BagHandler.getBagValue(outputID)[1])//there can't be a bag to replace in this case
				return;
			else {
				stored_value -= BagHandler.getBagValue(outputID)[1];//there is a bag and it got sucked out
				return;
			}
		}

	/*	if(stack.getItem() instanceof LootbagItem && stack.getCount()>1)//The only instance where this could happen is because of a NAUGHTY ITEM TRANSPORT MOD
		{																//so, do some logic to assume what caused it
			if(LootBags.STOREDCOUNT) {//looks that the returned stack is the bag count that remains from the stack
				int cnt = (int)Math.floor(stored_value/BagHandler.getBagValue(stack)[1]);
				if(stack.getCount() <= cnt) {//Overflow return?
					int cc = cnt - stack.getCount();
					stored_value -= cc*BagHandler.getBagValue(stack)[1];
				}
				else if(stack.getCount() >= cnt && stack.getCount() <= 64) {//adding a stack of bags probably?
					stored_value += stack.getCount()*BagHandler.getBagValue(stack)[1];
				}
				//Anything else is probably some odd corner case that sucks
			}
			return;
		}*/
		int value = BagHandler.getBagValue(stack)[0];
		if(value < 1)
			return;
		stored_value += value;
	
		this.markDirty();
	}

	@Override
	public int getInventoryStackLimit() {
		return 1;
	}

	@Override
	public void openInventory(EntityPlayer player) {
	}

	@Override
	public void closeInventory(EntityPlayer player) {
	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		if(!(stack.getItem() instanceof LootbagItem))
			return false;
		if(LootBags.PREVENTMERGEDBAGS)
		{
			if (!BagHandler.isBagOpened(stack) && BagHandler.isBagInsertable(stack.getMetadata()))
			{
				if(stored_value+BagHandler.getBagValue(stack.getMetadata())[0] >= Integer.MAX_VALUE || stored_value+BagHandler.getBagValue(stack.getMetadata())[0] < 0)
					return false;
				return true;
			}
		}
		else
		{
			if (BagHandler.isBagInsertable(stack.getMetadata()))
			{
				if(stored_value+BagHandler.getBagValue(stack.getMetadata())[0] >= Integer.MAX_VALUE || stored_value+BagHandler.getBagValue(stack.getMetadata())[0] < 0)
					return false;
				return true;
			}
		}
		return false;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {
	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return true;
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return (T) new SidedInvWrapper(this, facing);
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		if (this.world.getTileEntity(this.pos) != this) {
			return false;
		}
		return player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
	}

	public int getID() {
		return outputID;
	}

	public void decrStorage(ItemStack itemStack) {
		if (itemStack == null || itemStack.isEmpty() || !(itemStack.getItem() instanceof LootbagItem)) {
			return; //TODO: err here
		}
		int value = BagHandler.getBagValue(itemStack.getMetadata())[1];
		stored_value = Math.max(0, stored_value - value);
	}

	public void incrementStorage(ItemStack itemStack) {
		if (itemStack == null || itemStack.isEmpty() || !(itemStack.getItem() instanceof LootbagItem)) {
			return; //TODO: err here
		}
		int value = BagHandler.getBagValue(itemStack.getMetadata())[1];
		stored_value += value;
	}
}
/*******************************************************************************
 * Copyright (c) 2018 Malorolam.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the included license.
 * 
 *********************************************************************************/