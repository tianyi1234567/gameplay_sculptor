package com.tianyi.gameplay_sculptor;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.tianyi.gameplay_sculptor.Event.EntityEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//neoforge改了好多好难受啊啊啊啊啊啊啊啊啊！为什处理事件必须搞个全新的啊啊啊啊啊！
@Mod(GameplaySculptor.MODID)
public class GameplaySculptor {
    public static final String MODID = "gameplay_sculptor";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 生命值（能避免一些不必要的冲突）
    public static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("gameplay_sculptor", "health_boost");
    // 攻击力的
    public static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("gameplay_sculptor", "attack_damage_boost");
    // 护甲的
    public static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("gameplay_sculptor", "armor_boost");

    private final Map<String, Double> entityHealthMap = new HashMap<>();
    private final Map<String, Double> entityAttackDamageMap = new HashMap<>();
    private final Map<String, Double> entityArmorMap = new HashMap<>();

    private static GameplaySculptor instance;

    public GameplaySculptor(IEventBus modEventBus, ModContainer modContainer) {//neoforge改的这个太难用了.....
        instance = this;

        modEventBus.addListener(this::commonSetup);

        // Mod的事件
        modEventBus.register(new ModEventHandler());

        // 玩家相关事件
        NeoForge.EVENT_BUS.register(new PlayerEventHandler());

        NeoForge.EVENT_BUS.register(new EntityEvent());

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);// 注册配置
    }

    // Mod事件处理内部类(我不知道为什么要搞这个，但事件不框起来根本跑不了！)
    public static class ModEventHandler {
        @SubscribeEvent
        public void onConfigReload(ModConfigEvent.Reloading configEvent) {
            if (configEvent.getConfig().getSpec() == Config.COMMON_SPEC) {
                // 重新加载实体属性配置
                GameplaySculptor.getInstance().loadEntityConfig();

                // 重新应用所有在线玩家的生命值修改
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        GameplaySculptor.getInstance().applyAttributeModifiers(player);
                    }
                }
            }
        }
    }

    // 玩家事件处理内部类
    public static class PlayerEventHandler {//(我不知道为什么要搞这个，但事件不框起来根本跑不了！)
        @SubscribeEvent
        public void onPlayerClone(PlayerEvent.Clone event) {
            // 玩家数据克隆时保留属性修改
            GameplaySculptor.getInstance().applyAttributeModifiers(event.getEntity());
        }

        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            // 玩家登录时应用修改
            GameplaySculptor.getInstance().applyAttributeModifiers(event.getEntity());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 初始化实体属性配置
        event.enqueueWork(this::loadEntityConfig);
        LOGGER.info("Common setup completed.");
    }

    //加载实体属性配置，这个映射必不可少！
    private void loadEntityConfig() {
        loadEntityHealthConfig();
        loadEntityAttackDamageConfig();
        loadEntityArmorConfig();

        // 将配置映射传递给 EntityEvent 类
        com.tianyi.gameplay_sculptor.Event.EntityEvent.setEntityHealthMap(entityHealthMap);
        com.tianyi.gameplay_sculptor.Event.EntityEvent.setEntityAttackDamageMap(entityAttackDamageMap);
        com.tianyi.gameplay_sculptor.Event.EntityEvent.setEntityArmorMap(entityArmorMap);

        LOGGER.info("Loaded entity configs: {} health, {} attack, {} armor settings",
                   entityHealthMap.size(), entityAttackDamageMap.size(), entityArmorMap.size());
    }

    /**
     * 加载实体生命值配置
     */
    private void loadEntityHealthConfig() {
        entityHealthMap.clear();

        List<? extends String> settings = Config.COMMON.entityHealthSettings.get();
        for (String setting : settings) {
            String[] parts = setting.split(",");
            if (parts.length == 2) {
                try {
                    String entityId = parts[0].trim();
                    double healthValue = Double.parseDouble(parts[1].trim());
                    entityHealthMap.put(entityId, healthValue);
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    /**
     * 加载实体攻击力配置
     */
    private void loadEntityAttackDamageConfig() {
        entityAttackDamageMap.clear();

        List<? extends String> settings = Config.COMMON.entityAttackDamageSettings.get();
        for (String setting : settings) {
            String[] parts = setting.split(",");
            if (parts.length == 2) {
                try {
                    String entityId = parts[0].trim();
                    double attackValue = Double.parseDouble(parts[1].trim());
                    entityAttackDamageMap.put(entityId, attackValue);
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    /**
     * 加载实体防御力配置
     */
    private void loadEntityArmorConfig() {
        entityArmorMap.clear();

        List<? extends String> settings = Config.COMMON.entityArmorSettings.get();
        for (String setting : settings) {
            String[] parts = setting.split(",");
            if (parts.length == 2) {
                try {
                    String entityId = parts[0].trim();
                    double armorValue = Double.parseDouble(parts[1].trim());
                    entityArmorMap.put(entityId, armorValue);
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    // 写个方法用来处理多个属性
    private void applyAttributeModifiers(Player player) {
        // 应用生命值修改器
        applyHealthModifier(player);

        // 应用攻击力修改器
        applyAttackDamageModifier(player);

        // 应用防御力修改器
        applyArmorModifier(player);
    }

    private void applyHealthModifier(Player player) {
        // 从Config配置获取生命值增量
        double additionalHealth = Config.COMMON.additionalHealth.get();

        // 获取属性实例
        var attributeInstance = player.getAttribute(Attributes.MAX_HEALTH);

        // 直接移除现有的修改器（通过 ResourceLocation）
        attributeInstance.removeModifier(HEALTH_MODIFIER_ID);

        // 添加新的生命值修改器（仅传入 ID、值、操作类型）
        AttributeModifier healthModifier = new AttributeModifier(
                HEALTH_MODIFIER_ID,
                additionalHealth,
                AttributeModifier.Operation.ADD_VALUE
        );

        attributeInstance.addPermanentModifier(healthModifier);

        // 更新当前生命值
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private void applyAttackDamageModifier(Player player) {
        // 从Config配置获取攻击力增量
        double additionalAttackDamage = Config.COMMON.additionalAttackDamage.get();

        // 获取属性实例
        var attributeInstance = player.getAttribute(Attributes.ATTACK_DAMAGE);

        // 直接移除现有的修改器（通过 ResourceLocation）
        attributeInstance.removeModifier(ATTACK_DAMAGE_MODIFIER_ID);

        // 添加新的攻击力修改器
        AttributeModifier attackDamageModifier = new AttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID,
                additionalAttackDamage,
                AttributeModifier.Operation.ADD_VALUE
        );

        attributeInstance.addPermanentModifier(attackDamageModifier);
    }


    private void applyArmorModifier(Player player) {
        // 从Config配置获取防御力增量
        double additionalArmor = Config.COMMON.additionalArmor.get();

        // 获取属性实例
        var attributeInstance = player.getAttribute(Attributes.ARMOR);

        // 直接移除现有的修改器（通过 ResourceLocation）
        attributeInstance.removeModifier(ARMOR_MODIFIER_ID);

        // 添加新的防御力修改器
        AttributeModifier armorModifier = new AttributeModifier(
                ARMOR_MODIFIER_ID,
                additionalArmor,
                AttributeModifier.Operation.ADD_VALUE
        );

        attributeInstance.addPermanentModifier(armorModifier);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // 获取实例的方法
    public static GameplaySculptor getInstance() {
        return instance;
    }
}
