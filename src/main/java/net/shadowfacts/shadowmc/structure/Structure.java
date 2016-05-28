package net.shadowfacts.shadowmc.structure;

import com.google.gson.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.lang.reflect.Type;
import java.util.*;

/**
 * @author shadowfacts
 */
@NoArgsConstructor
@AllArgsConstructor
public class Structure implements IForgeRegistryEntry<Structure> {

	private ResourceLocation registryName;

	private BlockInfo[][][] blocks;

	public Structure(World world, AxisAlignedBB box) {
		int xSize = (int)(box.maxX - box.minX);
		int ySize = (int)(box.maxY - box.minY);
		int zSize = (int)(box.maxZ - box.minZ);

		BlockInfo[][][] blocks = new BlockInfo[ySize][xSize][zSize];

		for (int x = 0; x < xSize; x++) {
			for (int y = 0; y < ySize; y++) {
				for (int z = 0; z < zSize; z++) {

					BlockPos pos = new BlockPos(x + (int)box.minX, y + (int)box.minY, z + (int)box.minZ);
					IBlockState state = world.getBlockState(pos);
					blocks[y][x][z] = new BlockInfo(world, pos, state);

				}
			}
		}
	}

	public int xSize() {
		return blocks[0].length;
	}

	public int ySize() {
		return blocks.length;
	}

	public int zSize() {
		return blocks[0][0].length;
	}

	public BlockInfo get(int x, int y, int z) {
		return blocks[y][x][z];
	}

	public void generate(World world, BlockPos basePos) {
		for (int x = 0; x < xSize(); x++) {
			for (int y = 0; y < ySize(); y++) {
				for (int z = 0; z < zSize(); z++) {
					BlockInfo block = get(x, y, z);
					block.load(world, basePos.add(x, y, z));
				}
			}
		}
	}

	@Override
	public Structure setRegistryName(ResourceLocation name) {
		registryName = name;
		return this;
	}

	@Override
	public ResourceLocation getRegistryName() {
		return registryName;
	}

	@Override
	public Class<? super Structure> getRegistryType() {
		return Structure.class;
	}

	@NoArgsConstructor
	public static class BlockInfo {
		private String id;
		private Map<String, String> properties;

		private String lootId = "";
		private InventoryEntry[] inventory = new InventoryEntry[0];

		public BlockInfo(World world, BlockPos pos, IBlockState state) {
			id = Block.REGISTRY.getNameForObject(state.getBlock()).toString();

			properties = new HashMap<>();
			for (IProperty prop : state.getPropertyNames()) {
				properties.put(prop.getName(), prop.getName(state.getValue(prop)));
			}

			lootId = "";

			List<InventoryEntry> inventory = new ArrayList<>();
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IInventory) {
				IInventory inv = (IInventory)te;
				for (int i = 0; i < inv.getSizeInventory(); i++) {
					ItemStack stack = inv.getStackInSlot(i);
					if (stack != null) {
						inventory.add(new InventoryEntry(stack, i));
					}
				}
			} else if (te instanceof IItemHandlerModifiable) {
				IItemHandlerModifiable handler = (IItemHandlerModifiable)te;
				for (int i = 0; i < handler.getSlots(); i++) {
					ItemStack stack = handler.getStackInSlot(i);
					if (stack != null) {
						inventory.add(new InventoryEntry(stack, i));
					}
				}
			}
			this.inventory = inventory.toArray(new InventoryEntry[inventory.size()]);
		}

		private void load(World world, BlockPos pos) {
//			Block/State
			IBlockState state = id.isEmpty() ? Blocks.AIR.getDefaultState() : createState();
			world.setBlockState(pos, state);


			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IInventory) {
				IInventory inv = (IInventory)te;
//				Loot
				LootContext context = new LootContext.Builder((WorldServer)world).build();
				LootTable table = world.getLootTableManager().getLootTableFromLocation(new ResourceLocation(lootId));
				table.fillInventory(inv, world.rand, context);

//				Inventory
				for (InventoryEntry e : inventory) {
					inv.setInventorySlotContents(e.slot, e.getStack());
				}
			} else if (te instanceof IItemHandlerModifiable) {
//				Inventory
				IItemHandlerModifiable handler = (IItemHandlerModifiable)te;
				for (InventoryEntry e : inventory) {
					handler.setStackInSlot(e.slot, e.getStack());
				}
			}
		}

		private IBlockState createState() {
			Block block = Block.REGISTRY.getObject(new ResourceLocation(id));
			IBlockState state = block.getDefaultState();

			for (Map.Entry<String, String> e : properties.entrySet()) {
				IProperty prop = block.getBlockState().getProperties().stream()
						.filter(p -> p.getName().equals(e.getKey()))
						.findFirst()
						.get();

				state = state.withProperty(prop, getVal(e.getValue(), prop));
			}

			return state;
		}

		private <T extends Comparable<T>> T getVal(String name, IProperty<T> prop) {
			Map<String, T> valueNameMap = new HashMap<>();
			prop.getAllowedValues().forEach(t -> {
				valueNameMap.put(prop.getName(t), t);
			});
			return valueNameMap.get(name);
		}

		public static class Serializer implements JsonDeserializer<BlockInfo> {

			@Override
			public BlockInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				JsonObject obj =  json.getAsJsonObject();
				BlockInfo info = new BlockInfo();

				info.id = obj.get("id").getAsString();
				info.properties = new HashMap<>();
				JsonObject properties = obj.get("properties").getAsJsonObject();
				for (Map.Entry<String, JsonElement> e : properties.entrySet()) {
					info.properties.put(e.getKey(), e.getValue().getAsString());
				}

				if (obj.has("lootId")) {
					info.lootId = obj.get("lootId").getAsString();
				}

				if (obj.has("inventory")) {
					JsonArray array = obj.get("inventory").getAsJsonArray();
					info.inventory = new InventoryEntry[array.size()];
					for (int i = 0; i < array.size(); i++) {
						info.inventory[i] = context.deserialize(array.get(i), InventoryEntry.class);
					}
				}

				return info;
			}

		}

	}

	@AllArgsConstructor
	public static class InventoryEntry {
		private String item;
		private int amount;
		private int slot;

		public InventoryEntry(ItemStack stack, int slot) {
			this.slot = slot;

			String id = Item.REGISTRY.getNameForObject(stack.getItem()).toString();
			int meta = stack.getMetadata();
			if (meta != 0) {
				id += ":" + meta;
			}
			item = id;
			amount = stack.stackSize;
		}

		private ItemStack getStack() {
			String[] bits = item.split(":");
			Item item = Item.REGISTRY.getObject(new ResourceLocation(bits[0], bits[1]));
			int meta = bits.length > 2 ? Integer.parseInt(bits[2]) : 0;
			return new ItemStack(item, amount, meta);
		}
	}

}
