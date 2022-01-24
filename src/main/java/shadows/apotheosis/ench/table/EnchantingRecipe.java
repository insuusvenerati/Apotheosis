package shadows.apotheosis.ench.table;

import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistryEntry;
import shadows.apotheosis.ench.table.EnchantingStatManager.Stats;

public class EnchantingRecipe implements Recipe<Container> {

	public static final RecipeType<EnchantingRecipe> TYPE = RecipeType.register("apotheosis:enchanting");
	public static final Serializer SERIALIZER = new Serializer();
	public static final Stats NO_MAX = new Stats(-1, -1, -1, -1, -1, -1);

	protected final ResourceLocation id;
	protected final ItemStack output;
	protected final Ingredient input;
	protected final Stats requirements, maxRequirements;

	/**
	 * Defines an Enchanting Recipe.
	 * @param id The Recipe ID
	 * @param output The output ItemStack
	 * @param input The input Ingredient
	 * @param requirements The Level, Quanta, and Arcana requirements respectively.
	 * @param displayLevel The level to show on the fake "Infusion" Enchantment that will show up.
	 */
	public EnchantingRecipe(ResourceLocation id, ItemStack output, Ingredient input, Stats requirements, Stats maxRequirements) {
		this.id = id;
		this.output = output;
		this.input = input;
		this.requirements = requirements;
		this.maxRequirements = maxRequirements;
	}

	public boolean matches(ItemStack input, float eterna, float quanta, float arcana) {
		if (this.maxRequirements.eterna > -1 && eterna > this.maxRequirements.eterna || this.maxRequirements.quanta > -1 && quanta > this.maxRequirements.quanta || this.maxRequirements.arcana > -1 && arcana > this.maxRequirements.arcana) return false;
		return this.input.test(input) && eterna >= this.requirements.eterna && quanta >= this.requirements.quanta && arcana >= this.requirements.arcana;
	}

	public Stats getRequirements() {
		return this.requirements;
	}

	public Stats getMaxRequirements() {
		return this.maxRequirements;
	}

	public Ingredient getInput() {
		return this.input;
	}

	@Override
	public boolean matches(Container pContainer, Level pLevel) {
		return false;
	}

	@Override
	public ItemStack assemble(Container pContainer) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int pWidth, int pHeight) {
		return false;
	}

	@Override
	public ItemStack getResultItem() {
		return this.output;
	}

	@Override
	public ResourceLocation getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public RecipeType<?> getType() {
		return TYPE;
	}

	public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<EnchantingRecipe> {

		private static final Gson GSON = new GsonBuilder().create();

		@Override
		public EnchantingRecipe fromJson(ResourceLocation id, JsonObject obj) {
			ItemStack output = CraftingHelper.getItemStack(obj.get("result").getAsJsonObject(), true, true);
			Ingredient input = Ingredient.fromJson(obj.get("input"));
			Stats stats = GSON.fromJson(obj.get("requirements"), Stats.class);
			Stats maxStats = obj.has("max_requirements") ? GSON.fromJson(obj.get("max_requirements"), Stats.class) : NO_MAX;
			if (maxStats.eterna != -1 && stats.eterna > maxStats.eterna) throw new JsonParseException("An enchanting recipe (" + id + ") has invalid min/max eterna bounds (min > max).");
			if (maxStats.quanta != -1 && stats.quanta > maxStats.quanta) throw new JsonParseException("An enchanting recipe (" + id + ") has invalid min/max quanta bounds (min > max).");
			if (maxStats.arcana != -1 && stats.arcana > maxStats.arcana) throw new JsonParseException("An enchanting recipe (" + id + ") has invalid min/max arcana bounds (min > max).");
			return new EnchantingRecipe(id, output, input, stats, maxStats);
		}

		@Override
		public EnchantingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
			ItemStack output = buf.readItem();
			Ingredient input = Ingredient.fromNetwork(buf);
			Stats stats = Stats.read(buf);
			Stats maxStats = buf.readBoolean() ? Stats.read(buf) : NO_MAX;
			return new EnchantingRecipe(id, output, input, stats, maxStats);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, EnchantingRecipe recipe) {
			buf.writeItem(recipe.output);
			recipe.input.toNetwork(buf);
			recipe.requirements.write(buf);
			buf.writeBoolean(recipe.maxRequirements != NO_MAX);
			if (recipe.maxRequirements != NO_MAX) {
				recipe.maxRequirements.write(buf);
			}
		}

	}

	@Nullable
	public static EnchantingRecipe findMatch(Level level, ItemStack input, float eterna, float quanta, float arcana) {
		List<EnchantingRecipe> recipes = level.getRecipeManager().getAllRecipesFor(EnchantingRecipe.TYPE);
		recipes.sort((r1, r2) -> -Float.compare(r1.requirements.eterna, r2.requirements.eterna));
		for (EnchantingRecipe r : recipes)
			if (r.matches(input, eterna, quanta, arcana)) return r;
		return null;
	}

	public static EnchantingRecipe findItemMatch(Level level, ItemStack toEnchant) {
		return level.getRecipeManager().getAllRecipesFor(EnchantingRecipe.TYPE).stream().filter(r -> r.getInput().test(toEnchant)).findFirst().orElse(null);
	}

}
