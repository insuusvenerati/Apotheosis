package shadows.apotheosis.adventure.affix.effect;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import shadows.apotheosis.adventure.affix.Affix;
import shadows.apotheosis.adventure.affix.AffixType;
import shadows.apotheosis.adventure.loot.LootRarity;

public class DurableAffix extends Affix {

	public DurableAffix() {
		super(AffixType.DURABILITY);
	}

	@Override
	public boolean canApplyTo(ItemStack stack, LootRarity rarity) {
		return stack.isDamageableItem();
	}

	@Override
	public void addInformation(ItemStack stack, LootRarity rarity, float level, Consumer<Component> list) {
		list.accept(Component.translatable("affix." + this.getId() + ".desc", fmt(100 * level)).withStyle(Style.EMPTY.withColor(rarity.color())));
	}

}
