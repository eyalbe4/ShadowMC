package test;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.shadowfacts.shadowmc.gui.GUIBuilder;
import net.shadowfacts.shadowmc.gui.component.GUIComponent;
import net.shadowfacts.shadowmc.gui.component.GUIComponentTexture;
import net.shadowfacts.shadowmc.structure.StructureManager;

/**
 * @author shadowfacts
 */
@Mod(modid = ModTest.modId, name = ModTest.name, version = ModTest.version)
public class ModTest {

	public static final String modId = "modTest";
	public static final String name = "Mod Test";
	public static final String version = "0.1.0";

	@Mod.Instance("modTest")
	public static ModTest instance;

	private BlockTest blockTest;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {

		NetworkRegistry.INSTANCE.registerGuiHandler(instance, new TestGUIHandler());

		blockTest = new BlockTest();
		GameRegistry.register(blockTest);
		GameRegistry.register(new ItemBlock(blockTest).setRegistryName(blockTest.getRegistryName()));

		GameRegistry.registerTileEntity(TileEntityTest.class, "tileEntity");

		StructureManager.INSTANCE.register(new ResourceLocation(modId, "test"));
		StructureManager.INSTANCE.registerReloadHandler(new ResourceLocation(modId, "test"), StructureManager.INSTANCE::load);
	}

	private static GuiScreen create1() {
		return new GUIBuilder()
				.addComponent(new GUIComponentTexture(0, 0, 256, 256, GUIComponent.widgetTextures))
				.wrap();
	}

	public static class TestGUIHandler implements IGuiHandler {

		@Override
		public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
			switch (ID) {
				case 0:
					return new ContainerTest(player, (TileEntityTest) world.getTileEntity(new BlockPos(x, y, z)));
				default:
					return null;
			}
		}

		@Override
		public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
			switch (ID) {
				case 0:
					return GUITest.create(player, (TileEntityTest)world.getTileEntity(new BlockPos(x, y, z)));
				case 1:
					return create1();
				default:
					return null;
			}
		}
	}

}
