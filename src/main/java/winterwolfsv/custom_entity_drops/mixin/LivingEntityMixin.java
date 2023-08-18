package winterwolfsv.custom_entity_drops.mixin;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winterwolfsv.custom_entity_drops.DropSaver;
import winterwolfsv.custom_entity_drops.hash_map_handling.HashMapHandler;

import java.util.Optional;

import static winterwolfsv.custom_entity_drops.CED.customEntityDrop;
import static winterwolfsv.custom_entity_drops.CED.LOGGER;
//import static winterwolfsv.custom_entity_drops.CED.CustomEntityDrop2;


@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "onDeath", at = @At(value = "RETURN"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        if (!(((LivingEntity) (Object) this).world.isClient)) {
            onMobDeath((LivingEntity) (Object) this, source);
        }
    }

    private void onMobDeath(LivingEntity entity, DamageSource source) {
        LOGGER.info("Entity died: " + entity.getUuid().toString());
        for (DropSaver dropSaver : customEntityDrop) {
            if (dropSaver.UUID.equals(entity.getUuid().toString())) {
                Item item = Registry.ITEM.get(new Identifier(dropSaver.item));
                entity.dropItem(item, dropSaver.amount);
                customEntityDrop.remove(dropSaver);
                HashMapHandler.saveHashMap();
                break;
            }
        }
    }
}

