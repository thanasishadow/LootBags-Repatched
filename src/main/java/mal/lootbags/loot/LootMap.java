package mal.lootbags.loot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;

import mal.lootbags.Bag;
import mal.lootbags.LootBags;
import mal.lootbags.LootbagsUtil;
import mal.lootbags.handler.BagHandler;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootEntryItemAccess;
import net.minecraft.world.storage.loot.LootEntryTable;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class LootMap {

	private HashMap<String, LootItem> generalMap;
	private ArrayList<ResourceLocation> generalLootSources;
	private ArrayList<String> generalModBlacklist;
	private ArrayList<LootItem> generalBlacklist;
	private ArrayList<LootItem> generalWhitelist;
	
	public HashMap<String, LootItem> totalList;
	public ArrayList<LootItem> recyclerBlacklist;
	public ArrayList<LootItem> recyclerWhitelist;
	public HashMap<String, LootItem> recyclerMap;
	
	private LootContext context;
	
	private int generalTotalWeight;
	
	public LootMap()
	{
		clearMapData();
	}
	
	public void clearMapData()
	{
		generalMap = new HashMap<>();
		generalLootSources = new ArrayList<>();
		generalModBlacklist = new ArrayList<>();
		generalBlacklist = new ArrayList<>();
		generalWhitelist = new ArrayList<>();
		recyclerBlacklist = new ArrayList<>();
		recyclerWhitelist = new ArrayList<>();
		
		totalList = new HashMap<>();
		recyclerMap = new HashMap<>();
		
		generalTotalWeight = 0;
	}
	
	/*
	 * Populate the recycler blacklist
	 */
	public void populateRecyclerBlacklist(ArrayList<String> configRecyclerBlacklist)
	{
		for(String s:configRecyclerBlacklist)
		{
			String trim = s.trim();
			if(!trim.isEmpty())
			{
				String[] tempwords = trim.split("(?<!$):");
				if(tempwords.length==3)//correct length for a standard blacklist item
				{
					try {
						String modid = tempwords[0];
						String itemname = tempwords[1];
						ArrayList<Integer> itemdamage = LootbagsUtil.constructDamageRange(tempwords[2]);
						
						for(Integer dam: itemdamage)
						{
							LootItem item = new LootItem(null, modid, itemname, dam, 1, 1, 1, false);
							recyclerBlacklist.add(item);
							LootbagsUtil.LogInfo("Added General Recycler Blacklist Item: " + item.toString());
						}
					} catch(Exception e) {
						LootbagsUtil.LogError("General Recycler Blacklist Error: Line: " + s + " Improperly formed Blacklisted item causing exception.");
						e.printStackTrace();
					}
				}
				else
				{
					LootbagsUtil.LogError("General Recycler Blacklist Error: Line: " + s + " Improperly formed Blacklisted item.");
				}
			}
		}
		
		for(Bag b: BagHandler.getBagList().values())
		{
			if(b.getRecyclerBlacklist())
			{
				for(LootItem item:b.getMap().values())
				{
					if(!recyclerBlacklist.contains(item))
						recyclerBlacklist.add(item);
				}
			}
		}
	}
	
	/*
	 * Populate the recycler whitelist from the general config
	 */
	public void populateRecyclerWhitelist(ArrayList<String> configRecyclerWhitelist)
	{
		for(String s:configRecyclerWhitelist)
		{
			String trim = s.trim();
			if(!trim.isEmpty())
			{
				String[] tempwords = trim.split("(?<!$):");
				if(tempwords.length==4)//correct length for a standard whitelist item
				{
					try {
					String modid = tempwords[0];
					String itemname = tempwords[1];
					ArrayList<Integer> itemdamage = LootbagsUtil.constructDamageRange(tempwords[2]);
					int weight = Integer.parseInt(tempwords[3]);
					
						for(Integer dam: itemdamage)
						{
							LootItem item = new LootItem(null, modid, itemname, dam, 1, 1, weight, false);
							recyclerWhitelist.add(item);
							LootbagsUtil.LogInfo("Added General Recycler Whitelist Item: " + item.toString());
						}
					} catch(Exception e) {
						LootbagsUtil.LogError("General Recycler Whitelist Error: Line: " + s + " Improperly formed Whitelisted item causing exception.");
						e.printStackTrace();
					}
				}
				else if(tempwords.length==5)//length including NBT data
				{
					try {
					String modid = tempwords[0];
					String itemname = tempwords[1];
					ArrayList<Integer> itemdamage = LootbagsUtil.constructDamageRange(tempwords[2]);
					int weight = Integer.parseInt(tempwords[3]);
					byte[] nbt = LootbagsUtil.parseNBTArray(tempwords[4]);
					
					for(Integer dam: itemdamage)
					{
						LootItem item = new LootItem(null, modid, itemname, dam, 1, 1, weight, nbt, false);
						generalWhitelist.add(item);
						LootbagsUtil.LogInfo("Added General Recycler Whitelist Item with NBT: " + item.toString());
					}
					} catch(Exception e) {
						LootbagsUtil.LogError("General Recycler Whitelist Error: Line: " + s + " Improperly formed NBT Whitelisted item causing exception.");
						e.printStackTrace();
					}
				}
				else
				{
					LootbagsUtil.LogError("General Recycler Whitelist Error: Line: " + s + " Improperly formed Whitelisted item.");
				}
			}
		}
	}

	public void populateRecyclerMap()
	{
		for(Bag b:BagHandler.getBagList().values())
		{
			for(LootItem i:b.getMap().values())
			{
				if(LootBags.isItemRecyclable(i.getContentItem()))
					recyclerMap.put(i.getItemModID()+i.getItemName()+i.getContentItem().getItemDamage(),i);
			}
		}
	}

	public void populateLootValues()
	{
		for(Bag b:BagHandler.getBagList().values())
		{
			for(LootItem i:b.getMap().values())
			{
				i.setRecyclerValue();
			}
		}
	}

	/*
	 * Populates the general black list from the general config
	 */
	public void populateGeneralBlacklist(ArrayList<String> configBlacklist)
	{
		for(String s:configBlacklist)
		{
			String trim = s.trim();
			if(!trim.isEmpty())
			{
				String[] tempwords = trim.split("(?<!$):");
				if(tempwords.length==1)//correct length for a mod blacklist
				{
					if(!generalModBlacklist.contains(tempwords[0]))
						generalModBlacklist.add(tempwords[0]);
				}
				else if(tempwords.length==3)//correct length for a standard blacklist item
				{
					try {
						String modid = tempwords[0];
						String itemname = tempwords[1];
						if(tempwords[2].equalsIgnoreCase("*")) 
						{
							LootItem item = new LootItem(null, modid, itemname, -1, 1, 1, 1, false);
							generalBlacklist.add(item);
							LootbagsUtil.LogDebug("Added General Blacklist Item: " + item.toString());
						}
						else
						{
							ArrayList<Integer> itemdamage = LootbagsUtil.constructDamageRange(tempwords[2]);
						
							for(Integer dam: itemdamage)
							{
								LootItem item = new LootItem(null, modid, itemname, dam, 1, 1, 1, false);
								generalBlacklist.add(item);
								LootbagsUtil.LogDebug("Added General Blacklist Item: " + item.toString());
							}
						}
					} catch(Exception e) {
						LootbagsUtil.LogError("General Blacklist Error: Line: " + s + " Improperly formed Blacklisted item causing exception.");
						e.printStackTrace();
					}
				}
				else
				{
					LootbagsUtil.LogError("General Blacklist Error: Line: " + s + " Improperly formed Blacklisted item.");
				}
			}
		}
	}
	
	/*
	 * Populate the general whitelist
	 */
	public void populateGeneralWhitelist(ArrayList<String> configWhitelist)
	{
		for(String s:configWhitelist)
		{
			String trim = s.trim();
			if(!trim.isEmpty())
			{
				String[] tempwords = trim.split("(?<!$):");
				if(tempwords.length==6)//correct length for a standard whitelist item
				{
					try {
					String modid = tempwords[0];
					String itemname = tempwords[1];
					ArrayList<Integer> itemdamage = LootbagsUtil.constructDamageRange(tempwords[2]);
					int minstack = Integer.parseInt(tempwords[3]);
					int maxstack = Integer.parseInt(tempwords[4]);
					int weight = Integer.parseInt(tempwords[5]);
					
						for(Integer dam: itemdamage)
						{
							LootItem item = new LootItem(null, modid, itemname, dam, minstack, maxstack, weight, false);
							generalWhitelist.add(item);
							LootbagsUtil.LogDebug("Added General Whitelist Item: " + item.toString());
						}
					} catch(Exception e) {
						LootbagsUtil.LogError("General Whitelist Error: Line: " + s + " Improperly formed Whitelisted item causing exception.");
						e.printStackTrace();
					}
				}
				else if(tempwords.length==7)//length including NBT data
				{
					try {
					String modid = tempwords[0];
					String itemname = tempwords[1];
					ArrayList<Integer> itemdamage = LootbagsUtil.constructDamageRange(tempwords[2]);
					int minstack = Integer.parseInt(tempwords[3]);
					int maxstack = Integer.parseInt(tempwords[4]);
					int weight = Integer.parseInt(tempwords[5]);
					byte[] nbt = LootbagsUtil.parseNBTArray(tempwords[6]);
					
					for(Integer dam: itemdamage)
					{
						LootItem item = new LootItem(null, modid, itemname, dam, minstack, maxstack, weight, nbt, false);
						generalWhitelist.add(item);
						LootbagsUtil.LogDebug("Added General Whitelist Item with NBT: " + item.toString());
					}
					} catch(Exception e) {
						LootbagsUtil.LogError("General Whitelist Error: Line: " + s + " Improperly formed NBT Whitelisted item causing exception.");
						e.printStackTrace();
					}
				}
				else
				{
					LootbagsUtil.LogError("General Whitelist Error: Line: " + s + " Improperly formed Whitelisted item.");
				}
			}
		}
	}
	
	/*
	 * Populates the general map from the global loot sources, whitelist, and blacklist
	 */
	public void populateGeneralMap(World world)
	{
		//loot sources
		for(ResourceLocation source: generalLootSources)
		{
			try {
				addLootCategory(source, world);
			} catch (NoSuchFieldException | SecurityException
					| IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		//whitelist
		for(LootItem item: generalWhitelist)
		{
			if(item.getContentItem()==null)
				item.reinitializeLootItem();
			if(item.getContentItem()==null)
				LootbagsUtil.LogError("Loot Item with information: " + item.getItemModID() + ":" + item.getItemName() + " does not exist, even after reinitilizing it. This typically means the whitelist entry is wrong. This item will be skipped.");
			else
			{
				String key = item.getItemModID()+item.getItemName()+item.getContentItem().getItemDamage();
				if(item.getContentItem().getItem() instanceof ItemEnchantedBook && item.getContentItem().hasTagCompound())//a specific enchanted book
					key += item.getContentItem().getTagCompound().toString();
				generalMap.remove(key);//remove the existing entry to overwrite it with the whitelisted version
				
				generalMap.put(key, item);
				
				if(!totalList.containsKey(key))
					totalList.put(key, item);
			}
		}
	}

	/**
	 * Add every item in a category to the map calculating the drop chance
	 */
	public void addLootCategory(ResourceLocation categoryName, World world) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		LootbagsUtil.LogInfo("Starting adding items from loot table: " + categoryName + ".");
		LootTable table = LootbagsUtil.getLootManager(world).getLootTableFromLocation(categoryName);
		
		//reflect the lists in the table and pool so that I can actually access them
		String poolname;
		String entryname;
		if((boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment"))
		{
			poolname="pools";
			entryname="lootEntries";
		}
		else
		{
			poolname="field_186466_c";
			entryname="field_186453_a";
		}
		Field poolListField = LootTable.class.getDeclaredField(poolname);
		poolListField.setAccessible(true);
		Field lootListField = LootPool.class.getDeclaredField(entryname);
		lootListField.setAccessible(true);
		
		processLootTable(table, 0, poolListField, lootListField);
	}
	
	private void processLootTable(LootTable table, int count, Field poolListField, Field lootListField) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		//since this is recursive, counting the depth to keep things functional
		if(count > 10)
			return;
		
		List<LootPool> poolList = (List<LootPool>)poolListField.get(table);
		for(LootPool pool:poolList)
		{
			List<LootEntry> lootList = (List<LootEntry>)lootListField.get(pool);
			RandomValueRange prange = pool.getRolls();
			float average = (prange.getMin()+prange.getMax())/2;
			for(LootEntry loot:lootList)
			{
				if(loot instanceof LootEntryItem)
				{
					LootEntryItem lloot = (LootEntryItem) loot;
					ItemStack stack = LootEntryItemAccess.getLootEntryItemStack(lloot);
					int weight = (int)Math.floor(LootEntryItemAccess.getLootEntryItemWeight(lloot)*average);
					RandomValueRange range = LootEntryItemAccess.getStackSizes(lloot);
					int minstack;
					int maxstack;
					if(range != null)
					{
						minstack = (int) range.getMin();
						maxstack = (int) range.getMax();
					}
					else
					{
						minstack = 1;
						maxstack = 1;
					}
					
					LootItem item=null;
					boolean skip = false;
					if(stack==null || stack.getItem()==null)
					{
						skip = true;
						LootbagsUtil.LogInfo("Found a null item in the loot table, skipping it.");
					}
					else
						item = new LootItem(lloot, stack, minstack, maxstack, weight, true);
					//Do some fixing to prevent <1 errors in the bags
					if(item != null && item.getItemWeight() < 1)
					{
						LootbagsUtil.LogError("Item " + item.getContentItem().toString() + " has a weighting of " + item.getItemWeight() + ".  This is not a Lootbags error but an error in a different mod!  "
								+ "This item will be excluded from the Lootbags loot table.");
					}
					else if(!skip)
					{
					
						for(String modid:generalModBlacklist)
						{
							if(ForgeRegistries.ITEMS.getKey(item.getContentItem().getItem()).getClass().equals(modid))
							{
								skip = true;
								LootbagsUtil.LogInfo("Found item to skip from Blacklisted mod: " + item.toString());
							}
						}
						for(LootItem entry:generalBlacklist)
						{
							if(item.getContentItem().getItem().equals(entry.getContentItem().getItem()))
								if(entry.getMetadata() == -1 || item.getContentItem().getMetadata() == entry.getMetadata())
								{
									skip = true;
									LootbagsUtil.LogInfo("Found Blacklisted item to skip: " + item.toString());
								}
						}
						if(!skip)
						{
							
							String key = item.getItemModID()+item.getItemName()+item.getContentItem().getItemDamage();
							
							if(!generalMap.containsKey(key))
							{
								generalMap.put(key, item);
								LootbagsUtil.LogDebug("Added new General Item: " + item.toString());
								if(!totalList.containsKey(key))
									totalList.put(key, item);
							}
							else
							{
								LootItem it = generalMap.get(key);
								int wweight = it.getItemWeight();
								wweight = (wweight+item.getItemWeight())/2;
								it.setItemWeight(wweight);
								generalMap.put(key, it);
								LootbagsUtil.LogDebug("Merged new General Item: " + item.toString());
								if(!totalList.containsKey(key))
									totalList.put(key, it);
							}
						}
					}
				}
				if(loot instanceof LootEntryTable)
				{
					LootEntryTable tloot = (LootEntryTable)loot;
					LootTable ltable = LootEntryItemAccess.getLootTable(tloot, context);
					
					processLootTable(ltable, count+1, poolListField, lootListField);//repeat the process with the new table
				}
			}
		}
	}
	
	/*
	 * Get a subset of the map sorted by weight
	 */
	public ArrayList<LootItem> getMapByWeight(int minWeight, int maxWeight)
	{
		ArrayList<LootItem> ret = new ArrayList<LootItem>();
		
		//populate the list
		for(LootItem item:generalMap.values())
		{
			if((minWeight == -1 || item.getItemWeight() >= minWeight) && (maxWeight == -1 || item.getItemWeight() <= maxWeight))
			{
				ret.add(item);
			}
		}
		
		//sort the list
		Collections.sort(ret);
		
		return ret;
	}

	public ArrayList<String> getGeneralModBlacklist() {
		return generalModBlacklist;
	}

	public ArrayList<LootItem> getGeneralBlacklist() {
		return generalBlacklist;
	}

	public HashMap<String, LootItem> getMap()
	{
		return generalMap;
	}
	
	public void setTotalListWeight()
	{
		generalTotalWeight = 0;
		for(LootItem item: totalList.values())
		{
			//LootbagsUtil.LogInfo(item.toString());
			generalTotalWeight += item.getItemWeight();
		}
		for(LootItem item: recyclerWhitelist)
			generalTotalWeight += item.getItemWeight();
	}
	
	public int getTotalListWeight()
	{
		//LootbagsUtil.LogInfo("weight: " + generalTotalWeight);
		return generalTotalWeight;
	}
	
	public void setLootSources(String[] sources)
	{
		for(int i = 0; i < sources.length; i++)
			generalLootSources.add(new ResourceLocation(sources[i]));
	}
	
	public LootContext getContext()
	{
		return context;
	}
	
	public void setContext(@Nullable WorldServer world)
	{
		context = new LootContext(0, world, LootbagsUtil.getLootManager(world), null, null, null);
	}
}
/*******************************************************************************
 * Copyright (c) 2018 Malorolam.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the included license.
 * 
 *********************************************************************************/