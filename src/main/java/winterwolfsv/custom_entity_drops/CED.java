package winterwolfsv.custom_entity_drops;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.tags.CobblemonEntityTypeTags;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntitySummonArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import winterwolfsv.custom_entity_drops.hash_map_handling.HashMapHandler;

import java.util.*;

public class CED implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("CED");
    public static List<DropSaver> customEntityDrop = new ArrayList<>();

    @Override
    public void onInitialize() {
        HashMapHandler.loadHashMap();
        if (customEntityDrop == null) {
            customEntityDrop = new ArrayList<>();
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            dispatcher.register(CommandManager.literal("itemsummon")
                    .then(CommandManager.argument("coordinates", BlockPosArgumentType.blockPos())
                            .then(CommandManager.argument("entity", EntitySummonArgumentType.entitySummon())
                                    .suggests((context, builder) -> {
                                        Set<Identifier> livingEntities = new HashSet<>();
                                        for (Identifier identifier : Registry.ENTITY_TYPE.getIds()) {

                                            EntityType<?> entityType = Registry.ENTITY_TYPE.get(identifier);
                                            if (entityType.create(context.getSource().getWorld()) instanceof LivingEntity) {
                                                livingEntities.add(identifier);
                                            }
                                        }
                                        return CommandSource.suggestIdentifiers(livingEntities, builder);
                                    })
                                    .then(CommandManager.argument("item", IdentifierArgumentType.identifier())
                                            .suggests((context, builder) -> CommandSource.suggestIdentifiers(Registry.ITEM.getIds(), builder))
                                            .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                    .executes(context -> executeItemSummonCommand(
                                                            context.getSource(),
                                                            BlockPosArgumentType.getBlockPos(context, "coordinates"),
                                                            EntitySummonArgumentType.getEntitySummon(context, "entity"),
                                                            IdentifierArgumentType.getIdentifier(context, "item"),
                                                            IntegerArgumentType.getInteger(context, "amount")
                                                    ))
                                            )
                                    )
                            )
                    )
            );

        });
        boolean isPokeInstalled = FabricLoader.getInstance().isModLoaded("cobblemon");
        if (isPokeInstalled) {
            LOGGER.info("Found Cobblemon, enabling more commands.");

            CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {

                dispatcher.register(CommandManager.literal("pokedrops")
                        .then(CommandManager.argument("coordinates", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("pokemon", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            Set<String> pokemonSet = new HashSet<>();
                                            Collection<Species> pokemon = PokemonSpecies.INSTANCE.getSpecies();
                                            for (Species species : pokemon) {
                                                pokemonSet.add(species.getName());
                                            }
                                            return CommandSource.suggestMatching(pokemonSet, builder);
                                        })
                                        .then(CommandManager.argument("item", IdentifierArgumentType.identifier())
                                                .suggests((context, builder) -> CommandSource.suggestIdentifiers(Registry.ITEM.getIds(), builder))
                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(context -> executePokeDropCommand(
                                                                context.getSource(),
                                                                BlockPosArgumentType.getBlockPos(context, "coordinates"),
                                                                StringArgumentType.getString(context, "pokemon"),
                                                                IdentifierArgumentType.getIdentifier(context, "item"),
                                                                IntegerArgumentType.getInteger(context, "amount")
                                                        ))
                                                )
                                        )
                                )));
            });
        }

    }

    private static int executeItemSummonCommand(
            ServerCommandSource source,
            BlockPos coordinates,
            Identifier entityType,
            Identifier itemIdentifier,
            int amount) {
        try {

            World world = source.getWorld();
            Entity entity = EntityType.get(String.valueOf(entityType)).get().create(world);
            if (!(entity instanceof LivingEntity)) {
                source.sendMessage(Text.literal("§cThis entity is not a living entity!"));
                return 0;
            }
            entity.refreshPositionAndAngles(coordinates.getX(), coordinates.getY(), coordinates.getZ(), 0.0F, 0.0F);

            if (customEntityDrop == null) {
                customEntityDrop = new ArrayList<>();
            }
            customEntityDrop.add(new DropSaver(entity.getUuid().toString(), itemIdentifier.toString(), amount));
            HashMapHandler.saveHashMap();

            if (entity instanceof MobEntity) {
                ((MobEntity) entity).setPersistent();
            }

            world.spawnEntity(entity);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int executePokeDropCommand(
            ServerCommandSource source,
            BlockPos coordinates,
            String pokeType,
            Identifier itemIdentifier,
            int amount) {
        MinecraftServer server = source.getServer();
        String command = "pokespawnat " + coordinates.getX() + " " + coordinates.getY() + " " + coordinates.getZ() + " " + pokeType;

        ParseResults<ServerCommandSource> parseResults = server.getCommandManager().getDispatcher().parse(command, source);
        server.getCommandManager().execute(parseResults, "");

        ServerWorld world = source.getWorld();
        if (world != null) {
            world.getEntitiesByClass(LivingEntity.class, new Box(coordinates), entity -> true)
                    .forEach(livingEntity -> {
                        System.out.println(livingEntity.getDisplayName().getString() + " :: " + pokeType);
                        if (Objects.equals(livingEntity.getDisplayName().getString().toLowerCase(), pokeType.toLowerCase())) {

                            customEntityDrop.add(new DropSaver(livingEntity.getUuid().toString(), itemIdentifier.toString(), amount));
                            HashMapHandler.saveHashMap();
                        }
                    });
        }
        return 1;
        //pokeitemsummon ~ ~ ~ pikachu minecraft:stone 1
    }
}
