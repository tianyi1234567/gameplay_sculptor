package com.tianyi.gameplay_sculptor;

import com.google.common.collect.Lists;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Config {
    public static class Common {
        //生命值
        public final ModConfigSpec.DoubleValue additionalHealth;
        //攻击力
        public final ModConfigSpec.DoubleValue additionalAttackDamage;
        //防御力
        public final ModConfigSpec.DoubleValue additionalArmor;
        // 生物生命值配置
        public final ModConfigSpec.ConfigValue<List<? extends String>> entityHealthSettings;
        // 生物攻击力配置
        public final ModConfigSpec.ConfigValue<List<? extends String>> entityAttackDamageSettings;
        // 生物防御力配置
        public final ModConfigSpec.ConfigValue<List<? extends String>> entityArmorSettings;


        Common(ModConfigSpec.Builder builder) {
            builder.comment("Health Modifier Mod Configuration·玩家基础属性修改配置").push("attribute·玩家属性");
            //生命值配置
            additionalHealth = builder
                    .comment("Additional health to give players (in half-hearts)·每1点会为玩家提供半颗心")
                    .defineInRange("additionalHealth·玩家基础生命值调整", 0.0D, 0.0D, 1000000.0D);

            // 攻击力配置
            additionalAttackDamage = builder
                    .comment("Additional attack damage to give players·玩家基础攻击力调整")
                    .defineInRange("additionalAttack·玩家基础攻击力调整", 0.0D, 0.0D, 100000.0D);

            // 防御力配置
            additionalArmor = builder
                    .comment("Additional armor to give players·玩家基础防御力调整")
                    .defineInRange("additionalArmor·玩家基础防御力调整", 0.0D, 0.0D, 100000.0D);
            builder.pop();

            builder.comment("Entity Health Settings·生物属性修改配置").push("entity_health·生物属性");
            entityHealthSettings = builder
                    .comment("Entity health settings in format: entity_id,health_value·生物最大生命值调整")
                    .defineListAllowEmpty(
                            "entityHealthSettings·生物最大生命值调整（使用方法：ID+,+具体数值）",
                            Lists.newArrayList("minecraft:zombie,20.0", "minecraft:zombie_villager,20.0"),
                            getStringValidator()
                    );

            entityAttackDamageSettings = builder
                    .comment("Entity attack damage settings in format: entity_id,attack_damage_value·生物攻击力调整")
                    .defineListAllowEmpty(
                            "entityAttackDamageSettings·生物攻击力调整（使用方法：ID+,+具体数值）",
                            Lists.newArrayList("minecraft:zombie,3.0", "minecraft:zombie_villager,3.0"),
                            getStringValidator()
                    );

            entityArmorSettings = builder
                    .comment("Entity armor settings in format: entity_id,armor_value·生物防御力调整")
                    .defineListAllowEmpty(
                            "entityArmorSettings·生物防御力调整（使用方法：ID+,+具体数值）",
                            Lists.newArrayList("minecraft:zombie,0.0", "minecraft:zombie_villager,0.0"),
                            getStringValidator()
                    );
            builder.pop();
        }
        
        private static Predicate<Object> getStringValidator() {
            return obj -> obj instanceof String && ((String) obj).contains(",");
        }
    }

    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        final Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}