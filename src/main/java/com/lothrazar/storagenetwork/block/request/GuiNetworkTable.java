package com.lothrazar.storagenetwork.block.request;

import java.util.List;
import com.lothrazar.storagenetwork.StorageNetwork;
import com.lothrazar.storagenetwork.api.EnumSortType;
import com.lothrazar.storagenetwork.api.IGuiNetwork;
import com.lothrazar.storagenetwork.gui.NetworkWidget;
import com.lothrazar.storagenetwork.jei.JeiHooks;
import com.lothrazar.storagenetwork.jei.JeiSettings;
import com.lothrazar.storagenetwork.network.ClearRecipeMessage;
import com.lothrazar.storagenetwork.network.RequestMessage;
import com.lothrazar.storagenetwork.network.SortMessage;
import com.lothrazar.storagenetwork.registry.PacketRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;

/**
 * Base class for Request table inventory and Remote inventory
 */
public class GuiNetworkTable extends ContainerScreen<ContainerNetworkCraftingTable> implements IGuiNetwork {

  private static final int HEIGHT = 256;
  public static final int WIDTH = 176;
  private final ResourceLocation texture = new ResourceLocation(StorageNetwork.MODID, "textures/gui/request.png");
  final NetworkWidget network;
  private TileRequest tile;

  public GuiNetworkTable(ContainerNetworkCraftingTable container, PlayerInventory inv, ITextComponent name) {
    super(container, inv, name);
    tile = container.getTileRequest();
    network = new NetworkWidget(this);
    xSize = WIDTH;
    ySize = HEIGHT;
  }

  @Override
  public void setStacks(List<ItemStack> stacks) {
    network.stacks = stacks;
  }

  @Override
  public void init() {
    super.init();
    int searchLeft = guiLeft + 81, searchTop = guiTop + 96, width = 85;
    try {
      network.searchBar = new TextFieldWidget(font,
          searchLeft, searchTop,
          width, font.FONT_HEIGHT, "search");
      network.searchBar.setMaxStringLength(30);
      network.initSearchbar();
    }
    catch (Exception e) {
      //cannot replicate java.lang.NoClassDefFoundError: mezz/jei/api/recipe/IFocus 
      StorageNetwork.LOGGER.error("Search bar init error", e);
    }
    network.initButtons();
    this.addButton(network.directionBtn);
    this.addButton(network.sortBtn);
    if (JeiSettings.isJeiLoaded()) {
      addButton(network.jeiBtn);
    }
    //    addButton(network.clearTextBtn);
  }

  @Override
  public void render(int mouseX, int mouseY, float partialTicks) {
    renderBackground();
    super.render(mouseX, mouseY, partialTicks);
    renderHoveredToolTip(mouseX, mouseY);
    network.searchBar.render(mouseX, mouseY, partialTicks);
    network.render();
  }

  @Override
  public void syncDataToServer() {
    PacketRegistry.INSTANCE.sendToServer(new SortMessage(getPos(), getDownwards(), getSort()));
  }

  @Override
  public boolean getDownwards() {
    return tile.isDownwards();
  }

  @Override
  public void setDownwards(boolean d) {
    tile.setDownwards(d);
  }

  @Override
  public EnumSortType getSort() {
    return tile.getSort();
  }

  @Override
  public void setSort(EnumSortType s) {
    tile.setSort(s);
  }

  public BlockPos getPos() {
    return tile.getPos();
  }

  @Override
  public void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    minecraft.getTextureManager().bindTexture(texture);
    int xCenter = (width - xSize) / 2;
    int yCenter = (height - ySize) / 2;
    blit(xCenter, yCenter, 0, 0, xSize, ySize);
    //good stuff
    network.applySearchTextToSlots();
    network.renderItemSlots(mouseX, mouseY, font);
  }

  @Override
  public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    network.drawGuiContainerForegroundLayer(mouseX, mouseY);
  }

  boolean isScrollable(double x, double y) {
    int scrollHeight = 135;
    return this.isPointInRegion(0, 0,
        this.width - 8, scrollHeight,
        x, y);
  }

  /**
   * Negative is down; positive is up.
   *
   * @param x
   * @param y
   * @param mouseButton
   * @return
   */
  @Override
  public boolean mouseScrolled(double x, double y, double mouseButton) {
    super.mouseScrolled(x, y, mouseButton);
    if (isScrollable(x, y) && mouseButton != 0) {
      network.mouseScrolled(mouseButton);
    }
    return true;
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    network.mouseClicked(mouseX, mouseY, mouseButton);
    //recipe clear thingy
    //TODO: network needs isCrafting and isPointInRegion access to refactor
    // OR make real button lol
    int rectX = 63;
    int rectY = 110;
    if (isPointInRegion(rectX, rectY, 7, 7, mouseX, mouseY)) {
      PacketRegistry.INSTANCE.sendToServer(new ClearRecipeMessage());
      PacketRegistry.INSTANCE.sendToServer(new RequestMessage(0, ItemStack.EMPTY, false, false));
      return true;
    }
    return true;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int b) {
    InputMappings.Input mouseKey = InputMappings.getInputByCode(keyCode, scanCode);
    if (keyCode == 256) {//ESCAPE
      minecraft.player.closeScreen();
      return true; // Forge MC-146650: Needs to return true when the key is handled.
    }
    if (network.searchBar.isFocused()) {
      network.searchBar.keyPressed(keyCode, scanCode, b);
      if (keyCode == 259) {// BACKSPACE
        network.syncTextToJei();
      }
      return true;
    }
    else if (network.stackUnderMouse.isEmpty()) {
      try {
        JeiHooks.testJeiKeybind(mouseKey, network.stackUnderMouse);
      }
      catch (Throwable e) {
        StorageNetwork.LOGGER.error("JEI compat issue ", e);
        //its ok JEI not installed for maybe an addon mod is ok
      }
    }
    //regardles of above branch, also check this
    if (minecraft.gameSettings.keyBindInventory.isActiveAndMatches(mouseKey)) {
      minecraft.player.closeScreen();
      return true; // Forge MC-146650: Needs to return true when the key is handled.
    }
    return super.keyPressed(keyCode, scanCode, b);
  }

  @Override
  public boolean charTyped(char typedChar, int keyCode) {
    if (network.charTyped(typedChar, keyCode)) {
      return true;
    }
    return false;
  }

  @Override
  public void renderStackToolTip(ItemStack stack, int x, int y) {
    super.renderTooltip(stack, x, y);
  }

  @Override
  public void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
    super.fillGradient(left, top, right, bottom, startColor, endColor);
  }

  @Override
  public boolean isInRegion(int x, int y, int width, int height, double mouseX, double mouseY) {
    // because its protected and apparently sometimes abstract when compiled
    return super.isPointInRegion(x, y, width, height, mouseX, mouseY);
  }
}
