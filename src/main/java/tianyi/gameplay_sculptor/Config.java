package tianyi.gameplay_sculptor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class Config {
    public static class Common {
        //生命值
        public final ForgeConfigSpec.DoubleValue additionalHealth;
        //攻击力
        public final ForgeConfigSpec.DoubleValue additionalAttackDamage;
        //防御力
        public final ForgeConfigSpec.DoubleValue additionalArmor;
        // 生物生命值配置
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> entityHealthSettings;
        // 生物攻击力配置
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> entityAttackDamageSettings;
        // 生物防御力配置
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> entityArmorSettings;


        Common(ForgeConfigSpec.Builder builder) {
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
                    .defineListAllowEmpty("entityHealthSettings·生物最大生命值调整（使用方法：ID+,+具体数值）",
                            Lists.newArrayList("minecraft:zombie,20.0", "minecraft:zombie_villager,20.0"),
                            obj -> obj instanceof String && ((String) obj).contains(","));

            entityAttackDamageSettings = builder
                    .comment("Entity attack damage settings in format: entity_id,attack_damage_value·生物攻击力调整")
                    .defineListAllowEmpty("entityAttackDamageSettings·生物攻击力调整（使用方法：ID+,+具体数值）",
                            Lists.newArrayList("minecraft:zombie,3.0", "minecraft:zombie_villager,3.0"),
                            obj -> obj instanceof String && ((String) obj).contains(","));

            entityArmorSettings = builder
                    .comment("Entity armor settings in format: entity_id,armor_value·生物防御力调整")
                    .defineListAllowEmpty("entityArmorSettings·生物防御力调整（使用方法：ID+,+具体数值）",
                            Lists.newArrayList("minecraft:zombie,0.0", "minecraft:zombie_villager,0.0"),
                            obj -> obj instanceof String && ((String) obj).contains(","));
            builder.pop();
        }
    }

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}