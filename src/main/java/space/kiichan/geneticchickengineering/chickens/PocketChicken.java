package space.kiichan.geneticchickengineering.chickens;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.cscorelib2.inventory.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.World;
import space.kiichan.geneticchickengineering.GeneticChickengineering;
import space.kiichan.geneticchickengineering.adapter.AnimalsAdapter;
import space.kiichan.geneticchickengineering.chickens.ChickenTypes;
import space.kiichan.geneticchickengineering.genetics.DNA;
import space.kiichan.geneticchickengineering.items.GCEItems;

public class PocketChicken<T extends LivingEntity> extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    private final AnimalsAdapter adapter = new AnimalsAdapter<>(Chicken.class);
    private final NamespacedKey adapterkey;
    private final NamespacedKey dnakey;
    public GeneticChickengineering plugin;
    private int mutationRate;
    private int maxMutation;
    private boolean displayResources;

    public PocketChicken(GeneticChickengineering plugin, Category category, SlimefunItemStack item, int mutationRate, int maxMutation, boolean displayResources, NamespacedKey dnakey, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
        this.plugin = plugin;
        this.adapterkey = new NamespacedKey(plugin, "gce_pocket_chicken_adapter");
        this.dnakey = dnakey;
        this.mutationRate = mutationRate;
        this.maxMutation = maxMutation;
        this.displayResources = displayResources;
    }
    public PocketChicken(GeneticChickengineering plugin, Category category, SlimefunItemStack item, int mutationRate, int maxMutation, boolean displayResources, DNA dna, NamespacedKey adapterkey, NamespacedKey dnakey, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
        this.plugin = plugin;
        this.adapterkey = adapterkey;
        this.dnakey = dnakey;
        this.mutationRate = mutationRate;
        this.maxMutation = maxMutation;
        this.displayResources = displayResources;
    }

    public JsonObject babyJson() {
        // Returns the json for a new baby chicken
        JsonObject json = new JsonObject();
        json.addProperty("_type", "CHICKEN");
        json.addProperty("_health", 4.0);
        json.addProperty("_absorption", 0.0);
        json.addProperty("_removeWhenFarAway", false);
        json.addProperty("_customName", (String) null);
        json.addProperty("_customNameVisible", false);
        json.addProperty("_ai", true);
        json.addProperty("_silent", false);
        json.addProperty("_glowing", false);
        json.addProperty("_invulnerable", false);
        json.addProperty("_collidable", true);
        json.addProperty("_gravity", true);
        json.addProperty("_fireTicks", 0);
        json.addProperty("baby", true);
        json.addProperty("_age", -24000);
        json.addProperty("_ageLock", false);
        json.addProperty("_breedable", false);
        json.addProperty("_loveModeTicks", 0);
        JsonObject attributes = new JsonObject();
        json.add("_attributes", attributes);
        JsonObject effects = new JsonObject();
        json.add("_effects", effects);
        JsonArray tags = new JsonArray();
        json.add("_scoreboardTags", tags);
        return json;
    }

    public ItemStack breed(ItemStack chick1, ItemStack chick2) {
        ItemMeta c1m = chick1.getItemMeta();
        ItemMeta c2m = chick2.getItemMeta();
        PersistentDataContainer c1c = c1m.getPersistentDataContainer();
        PersistentDataContainer c2c = c2m.getPersistentDataContainer();
        if (c1c.has(dnakey, PersistentDataType.INTEGER_ARRAY) && c2c.has(dnakey, PersistentDataType.INTEGER_ARRAY)) {
            DNA c1d = new DNA(c1c.get(dnakey, PersistentDataType.INTEGER_ARRAY));
            DNA c2d = new DNA(c2c.get(dnakey, PersistentDataType.INTEGER_ARRAY));
            return this.fromDNA(new DNA(c1d.split(), c2d.split()));
        }
        return null;
    }

    public ItemStack convert(Chicken entity) {
        JsonObject json = adapter.saveData(entity);
        ItemStack item = getItem().clone();
        ItemMeta meta = item.getItemMeta();

        DNA dna;
        String uuid = entity.getUniqueId().toString();

        if (entity.hasMetadata("gce_pocket_chicken_dna")) {
            dna = new DNA(entity.getMetadata("gce_pocket_chicken_dna").get(0).asString());
            this.plugin.db.delete(uuid);
        } else if (this.plugin.db.has(uuid)) {
            dna = new DNA(this.plugin.db.getDNAOrNull(uuid));
        } else {
            dna = new DNA(mutationRate, maxMutation);
        }

        if (this.displayResources && json.get("_customNameVisible").getAsBoolean() && dna.isKnown()) {
            String name;
            if (!json.get("_customName").isJsonNull()) {
                name = json.get("_customName").getAsString();
            } else {
                name = "";
            }
            name = name.replace(" ("+ChickenTypes.getName(dna.getTyping())+")","")
                       .replace("("+ChickenTypes.getName(dna.getTyping())+")","");
            if (name.isEmpty()) {
                json.addProperty("_customName", (String) null);
                json.addProperty("_customNameVisible", false);
            } else {
                json.addProperty("_customName", name);
            }
        }

        this.setLore(item, json, dna);
        return item;
    }

    public PocketChicken fakeVariant(int typing, String name, RecipeType rt) {
        // Returns a chicken variant of the typing
        // Just used for adding the variants to the guide
        ItemStack item = getItem().clone();
        int[] state = new int[7];
        String typeStr = Integer.toBinaryString(typing);
        String padded = String.format("%6s", typeStr).replaceAll(" ", "0");

        for (int i=0; i<6; i++) {
            state[i] = 3*(((int) padded.charAt(i))-((int) '0'));
        }
        state[6] = 1;
        DNA dna = new DNA(state);
        SlimefunItemStack fakeitem = GCEItems.makeChicken(ChickenTypes.getName(typing));
        this.setLore(fakeitem, null, dna);
        PocketChicken newpc = new PocketChicken(this.plugin, this.getCategory(), fakeitem, this.mutationRate, this.maxMutation, this.displayResources, dna, this.adapterkey, this.dnakey, rt, this.getRecipe());
        newpc.register(this.plugin);
        return newpc;
    }

    public ItemStack fromDNA(DNA dna) {
        /* Reverse the adapter's saveData function
         * to create fresh baby chicken data
         */
        JsonObject json = this.babyJson();

        ItemStack item = getItem().clone();
        this.setLore(item, json, dna);
        return item;
    }

    public DNA getDNA(ItemStack chick) {
        PersistentDataContainer container = chick.getItemMeta().getPersistentDataContainer();
        DNA dna = new DNA(container.get(dnakey, PersistentDataType.INTEGER_ARRAY));
        return dna;
    }

    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            e.cancel();

            Optional<Block> block = e.getClickedBlock();

            if (block.isPresent()) {
                Block b = block.get();
                Location l = b.getRelative(e.getClickedFace()).getLocation();
                Chicken entity = b.getWorld().spawn(l.toCenterLocation(), Chicken.class);

                PersistentDataContainer container = e.getItem().getItemMeta().getPersistentDataContainer();
                JsonObject json = container.get(adapterkey, (PersistentDataType<String, JsonObject>) adapter);
                DNA dna = new DNA(container.get(dnakey, PersistentDataType.INTEGER_ARRAY));

                String dss = dna.getStateString();
                entity.setMetadata("gce_pocket_chicken_dna", new FixedMetadataValue(plugin, dss));
                this.plugin.db.insert(entity.getUniqueId().toString(), dss);

                if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    ItemUtils.consumeItem(e.getItem(), false);
                }
                if (json != null) {
                    if (this.displayResources && dna.isKnown()) {
                        json.addProperty("_customNameVisible", true);
                        String name = "("+ChickenTypes.getName(dna.getTyping())+")";
                        if (!json.get("_customName").isJsonNull()) {
                            name = json.get("_customName").getAsString() + " " + name;
                        }
                        json.addProperty("_customName",name);
                    }
                    adapter.apply(entity, json);
                }

            }
        };
    }

    private List<String> getLore(JsonObject json, DNA dna) {
        List<String> lore = new LinkedList<>();
        if (json != null) {
            lore = adapter.getLore(json);
        }
        if (dna.isKnown()) {
            String chicktype = ChickenTypes.getName(dna.getTyping());
            lore.add(ChatColor.GOLD + "DNA: " + ChatColor.RESET + dna.toString());
            lore.add(ChatColor.GOLD + "Type: " + ChatColor.RESET + chicktype + " Chicken");
        }
        return lore;
    }

    public ItemStack getResource(ItemStack chick) {
        DNA dna = this.getDNA(chick);
        return ChickenTypes.getResource(dna.getTyping());
    }

    public int getResourceTier(ItemStack chick) {
        // Returns the number of homozygous recessive genes in the chicken
        // which represents the difficulty of obtaining this chicken
        DNA dna = this.getDNA(chick);
        return dna.getTier();
    }

    public boolean isAdult(ItemStack chick) {
        PersistentDataContainer container = chick.getItemMeta().getPersistentDataContainer();
        JsonObject json = container.get(adapterkey, (PersistentDataType<String, JsonObject>) adapter);
        return !json.get("baby").getAsBoolean();
    }

    public boolean isLearned(ItemStack chick) {
        DNA dna = this.getDNA(chick);
        return dna.isKnown();
    }

    public boolean isPocketChicken(ItemStack chick) {
        return chick.getItemMeta().getPersistentDataContainer().has(dnakey, PersistentDataType.INTEGER_ARRAY);
    }

    public ItemStack learnDNA(ItemStack chick) {
        ItemStack item = chick.clone();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(dnakey, PersistentDataType.INTEGER_ARRAY)) {
            DNA dna = new DNA(container.get(dnakey, PersistentDataType.INTEGER_ARRAY));

            dna.learn();
            JsonObject json = container.get(adapterkey, (PersistentDataType<String, JsonObject>) adapter);
            this.setLore(item, json, dna);
        }

        return item;
    }

    private void setLore(ItemStack item, JsonObject json, DNA dna) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(dnakey, PersistentDataType.INTEGER_ARRAY, dna.getState());
        if (json != null) {
            meta.getPersistentDataContainer().set(adapterkey, adapter, json);
        }
        meta.setLore(getLore(json, dna));

        item.setItemMeta(meta);
    }
}