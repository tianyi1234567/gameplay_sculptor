package tianyi.gameplay_sculptor;

import net.minecraft.server.TickTask;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.UUID;

public class EntityEvent {
    // 实体生命值修改器UUID
    public static final UUID ENTITY_HEALTH_MODIFIER_UUID = UUID.fromString("20060930-3456-3456-3456-20060930");
    // 实体攻击力修改器UUID
    public static final UUID ENTITY_ATTACK_DAMAGE_MODIFIER_UUID = UUID.fromString("20060930-7890-7890-7890-20060930");
    // 实体防御力修改器UUID
    public static final UUID ENTITY_ARMOR_MODIFIER_UUID = UUID.fromString("20060930-1111-1111-1111-20060930");

    // 引用主类中的实体属性映射
    private static Map<String, Double> entityHealthMap;
    private static Map<String, Double> entityAttackDamageMap;
    private static Map<String, Double> entityArmorMap;

    public static void setEntityHealthMap(Map<String, Double> map) {
        entityHealthMap = map;
    }

    public static void setEntityAttackDamageMap(Map<String, Double> map) {
        entityAttackDamageMap = map;
    }

    public static void setEntityArmorMap(Map<String, Double> map) {
        entityArmorMap = map;
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        // 只在服务端处理
        if (event.getLevel().isClientSide()) return;

        // 检查是否是生物实体
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            // 延迟1 tick执行，确保实体完全初始化
            event.getLevel().getServer().tell(new TickTask(1, () -> {
                applyEntityHealthModifier(livingEntity);
                applyEntityAttackDamageModifier(livingEntity);
                applyEntityArmorModifier(livingEntity);
            }));
        }
    }

    /**
     * 应用实体生命值修改器
     */
    private void applyEntityHealthModifier(LivingEntity entity) {
        // 获取实体注册名
        ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityKey == null) return;

        String entityId = entityKey.toString();

        // 检查是否有针对此实体的配置
        if (entityHealthMap != null && entityHealthMap.containsKey(entityId)) {
            double newMaxHealth = entityHealthMap.get(entityId);

            // 获取生命值属性实例
            var attributeInstance = entity.getAttribute(Attributes.MAX_HEALTH);
            if (attributeInstance != null) {
                // 移除现有的修改器（通过 UUID）
                if (attributeInstance.getModifier(ENTITY_HEALTH_MODIFIER_UUID) != null) {
                    attributeInstance.removeModifier(ENTITY_HEALTH_MODIFIER_UUID);
                }

                // 添加新的生命值修改器
                AttributeModifier healthModifier = new AttributeModifier(
                        ENTITY_HEALTH_MODIFIER_UUID,
                        "Custom Entity Health Boost",
                        newMaxHealth - attributeInstance.getBaseValue(), // 计算差值
                        AttributeModifier.Operation.ADDITION
                );

                attributeInstance.addPermanentModifier(healthModifier);

                // 回满血
                entity.setHealth(entity.getMaxHealth());
            }
        }
    }

    /**
     * 应用实体攻击力修改器
     */
    private void applyEntityAttackDamageModifier(LivingEntity entity) {
        // 获取实体注册名
        ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityKey == null) return;

        String entityId = entityKey.toString();

        // 检查是否有针对此实体的配置
        if (entityAttackDamageMap != null && entityAttackDamageMap.containsKey(entityId)) {
            double newAttackDamage = entityAttackDamageMap.get(entityId);

            // 获取攻击力属性实例
            var attributeInstance = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attributeInstance != null) {
                // 移除现有的修改器（通过 UUID）
                if (attributeInstance.getModifier(ENTITY_ATTACK_DAMAGE_MODIFIER_UUID) != null) {
                    attributeInstance.removeModifier(ENTITY_ATTACK_DAMAGE_MODIFIER_UUID);
                }

                // 添加新的攻击力修改器
                AttributeModifier attackDamageModifier = new AttributeModifier(
                        ENTITY_ATTACK_DAMAGE_MODIFIER_UUID,
                        "Custom Entity Attack Damage Boost",
                        newAttackDamage - attributeInstance.getBaseValue(), // 计算差值
                        AttributeModifier.Operation.ADDITION
                );

                attributeInstance.addPermanentModifier(attackDamageModifier);
            }
        }
    }

    /**
     * 应用实体防御力修改器
     */
    private void applyEntityArmorModifier(LivingEntity entity) {
        // 获取实体注册名
        ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityKey == null) return;

        String entityId = entityKey.toString();

        // 检查是否有针对此实体的配置
        if (entityArmorMap != null && entityArmorMap.containsKey(entityId)) {
            double newArmor = entityArmorMap.get(entityId);

            // 获取防御力属性实例
            var attributeInstance = entity.getAttribute(Attributes.ARMOR);
            if (attributeInstance != null) {
                // 移除现有的修改器（通过 UUID）
                if (attributeInstance.getModifier(ENTITY_ARMOR_MODIFIER_UUID) != null) {
                    attributeInstance.removeModifier(ENTITY_ARMOR_MODIFIER_UUID);
                }

                // 添加新的防御力修改器
                AttributeModifier armorModifier = new AttributeModifier(
                        ENTITY_ARMOR_MODIFIER_UUID,
                        "Custom Entity Armor Boost",
                        newArmor - attributeInstance.getBaseValue(), // 计算差值
                        AttributeModifier.Operation.ADDITION
                );

                attributeInstance.addPermanentModifier(armorModifier);
            }
        }
    }
}
