package com.tianyi.gameplay_sculptor.Event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.Map;

public class EntityEvent {
    // 实体生命值修改器ResourceLocation
    public static final ResourceLocation ENTITY_HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("gameplay_sculptor", "entity_health");
    // 实体攻击力修改器ResourceLocation
    public static final ResourceLocation ENTITY_ATTACK_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("gameplay_sculptor", "entity_attack_damage");
    // 实体防御力修改器ResourceLocation
    public static final ResourceLocation ENTITY_ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("gameplay_sculptor", "entity_armor");

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
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.getServer().tell(new TickTask(1, () -> {
                    applyEntityHealthModifier(livingEntity);
                    applyEntityAttackDamageModifier(livingEntity);
                    applyEntityArmorModifier(livingEntity);
                }));
            }
        }
    }

    /**
     * 应用实体生命值修改器
     */
    private void applyEntityHealthModifier(LivingEntity entity) {
        // 获取实体注册名
        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityKey == null) return;

        String entityId = entityKey.toString();

        // 检查是否有针对此实体的配置
        if (entityHealthMap != null && entityHealthMap.containsKey(entityId)) {
            double newMaxHealth = entityHealthMap.get(entityId);

            // 获取生命值属性实例
            var attributeInstance = entity.getAttribute(Attributes.MAX_HEALTH);
            if (attributeInstance != null) {
                // 移除现有的修改器（通过 ResourceLocation）
                attributeInstance.removeModifier(ENTITY_HEALTH_MODIFIER_ID);

                // 添加新的生命值修改器
                AttributeModifier healthModifier = new AttributeModifier(
                        ENTITY_HEALTH_MODIFIER_ID,
                        newMaxHealth - attributeInstance.getBaseValue(), // 计算差值
                        AttributeModifier.Operation.ADD_VALUE
                );

                attributeInstance.addTransientModifier(healthModifier); // 使用addTransientModifier而不是addPermanentModifier

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
        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityKey == null) return;

        String entityId = entityKey.toString();

        // 检查是否有针对此实体的配置
        if (entityAttackDamageMap != null && entityAttackDamageMap.containsKey(entityId)) {
            double newAttackDamage = entityAttackDamageMap.get(entityId);

            // 获取攻击力属性实例
            var attributeInstance = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attributeInstance != null) {
                // 移除现有的修改器（通过 ResourceLocation）
                if (attributeInstance.getModifier(ENTITY_ATTACK_DAMAGE_MODIFIER_ID) != null) {
                    attributeInstance.removeModifier(ENTITY_ATTACK_DAMAGE_MODIFIER_ID);
                }

                // 添加新的攻击力修改器
                AttributeModifier attackDamageModifier = new AttributeModifier(
                        ENTITY_ATTACK_DAMAGE_MODIFIER_ID,
                        newAttackDamage - attributeInstance.getBaseValue(), // 计算差值
                        AttributeModifier.Operation.ADD_VALUE
                );

                attributeInstance.addTransientModifier(attackDamageModifier);
            }
        }
    }

    /**
     * 应用实体防御力修改器
     */
    private void applyEntityArmorModifier(LivingEntity entity) {
        // 获取实体注册名
        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityKey == null) return;

        String entityId = entityKey.toString();

        // 检查是否有针对此实体的配置
        if (entityArmorMap != null && entityArmorMap.containsKey(entityId)) {
            double newArmor = entityArmorMap.get(entityId);

            // 获取防御力属性实例
            var attributeInstance = entity.getAttribute(Attributes.ARMOR);
            if (attributeInstance != null) {
                // 移除现有的修改器（通过 ResourceLocation）
                if (attributeInstance.getModifier(ENTITY_ARMOR_MODIFIER_ID) != null) {
                    attributeInstance.removeModifier(ENTITY_ARMOR_MODIFIER_ID);
                }

                // 添加新的防御力修改器
                AttributeModifier armorModifier = new AttributeModifier(
                        ENTITY_ARMOR_MODIFIER_ID,
                        newArmor - attributeInstance.getBaseValue(), // 计算差值
                        AttributeModifier.Operation.ADD_VALUE
                );

                attributeInstance.addTransientModifier(armorModifier);
            }
        }
    }
}