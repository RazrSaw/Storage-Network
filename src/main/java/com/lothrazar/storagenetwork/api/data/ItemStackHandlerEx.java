package com.lothrazar.storagenetwork.api.data;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import java.util.ArrayList;
import java.util.List;

public class ItemStackHandlerEx extends ItemStackHandler {

  public ItemStackHandlerEx(int size) {
    super(size);
  }

  public List<ItemStack> getStacks() {
    List<ItemStack> result = new ArrayList<>();
    for (int slot = 0; slot < this.getSlots(); slot++) {
      ItemStack stack = this.getStackInSlot(slot);
      result.add(stack);
    }
    return result;
  }
}
