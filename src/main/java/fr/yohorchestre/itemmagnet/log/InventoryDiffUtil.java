package fr.yohorchestre.itemmagnet.log;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class InventoryDiffUtil {

    public static List<ItemStack> getAddedItems(ItemStack[] oldContents, ItemStack[] newContents) {
        return getDiff(oldContents, newContents, true);
    }

    public static List<ItemStack> getRemovedItems(ItemStack[] oldContents, ItemStack[] newContents) {
        return getDiff(newContents, oldContents, true);
    }

    private static List<ItemStack> getDiff(ItemStack[] base, ItemStack[] compareTo, boolean findAdded) {
        List<ItemStack> diff = new ArrayList<>();
        
        List<ItemStack> baseList = copyNonNull(base);
        List<ItemStack> compareList = copyNonNull(compareTo);

        for (ItemStack compareItem : compareList) {
            int remaining = compareItem.getAmount();
            
            // Try to find matching items in base to subtract
            for (int i = 0; i < baseList.size(); i++) {
                ItemStack baseItem = baseList.get(i);
                if (baseItem.isSimilar(compareItem)) {
                    if (baseItem.getAmount() >= remaining) {
                        baseItem.setAmount(baseItem.getAmount() - remaining);
                        remaining = 0;
                        if (baseItem.getAmount() == 0) {
                            baseList.remove(i);
                        }
                        break;
                    } else {
                        remaining -= baseItem.getAmount();
                        baseList.remove(i);
                        i--;
                    }
                }
            }
            
            if (remaining > 0) {
                ItemStack diffItem = compareItem.clone();
                diffItem.setAmount(remaining);
                diff.add(diffItem);
            }
        }
        
        return diff;
    }

    private static List<ItemStack> copyNonNull(ItemStack[] arr) {
        List<ItemStack> list = new ArrayList<>();
        if (arr == null) return list;
        for (ItemStack i : arr) {
            if (i != null && !i.getType().isAir()) {
                list.add(i.clone());
            }
        }
        return list;
    }
}
