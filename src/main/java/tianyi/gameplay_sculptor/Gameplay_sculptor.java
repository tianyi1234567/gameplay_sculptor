package tianyi.gameplay_sculptor;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.*;
import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Gameplay_sculptor.MODID)
public class Gameplay_sculptor {

    public static final String MODID = "gameplay_sculptor";
    private static final Logger LOGGER = LogUtils.getLogger();

    // 生命值修改器的UUID（能避免一些不必要的冲突）
    public static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("20060930-1234-1234-1234-20060930");
    // 攻击力的UUID
    public static final UUID ATTACK_DAMAGE_MODIFIER_UUID = UUID.fromString("20060930-5678-5678-5678-20060930");
    // 护甲的UUID
    public static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("20060930-9012-9012-9012-20060930");
    // 添加实体UUID用于标识实体生命值修改器（最终还是以生命id为准）
    public static final UUID ENTITY_HEALTH_MODIFIER_UUID = UUID.fromString("20060930-3456-3456-3456-20060930");

    // 存储实体属性配置
    private final Map<String, Double> entityHealthMap = new HashMap<>();
    private final Map<String, Double> entityAttackDamageMap = new HashMap<>();
    private final Map<String, Double> entityArmorMap = new HashMap<>();
    // 存储玩家数量倍数
    private double playerCountMultiplier = 1.0;
    /**
     * 计算当前玩家数量倍数
     * 公式：基础倍数(100%) + 每个玩家增长百分比 × (玩家数 - 1)
     */
    private double calculatePlayerCountMultiplier(MinecraftServer server) {
        if (!Config.COMMON.enablePlayerCountMultiplier.get() || server == null) {
            return 1.0;
        }

        int playerCount = server.getPlayerList().getPlayers().size();
        double multiplierPercent = Config.COMMON.playerCountMultiplierPercent.get();
        
        // 公式：倍数 = 1.0 + (百分比 / 100) × (玩家数 - 1)
        // 例如：3个玩家，50%增长 = 1.0 + 0.5 × (3 - 1) = 2.0（200%）
        double multiplier = 1.0 + (multiplierPercent / 100.0) * (playerCount - 1);
        return Math.max(1.0, multiplier);
    }

    public Gameplay_sculptor() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        // 注册实体事件处理器
        MinecraftForge.EVENT_BUS.register(new EntityEvent());

        // Config设置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
    }

    @SubscribeEvent
    public void onConfigReload(ModConfigEvent.Reloading configEvent) {
        if (configEvent.getConfig().getSpec() == Config.COMMON_SPEC) {
            // 重新加载实体属性配置
            loadEntityConfig();

            // 更新玩家数量倍数
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                playerCountMultiplier = calculatePlayerCountMultiplier(server);
            }

            // 重新应用所有在线玩家的生命值修改
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    applyAttributeModifiers(player);
                }
            }
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 初始化实体属性配置
        event.enqueueWork(this::loadEntityConfig);
        LOGGER.info("Common setup completed.");
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        // 玩家数据克隆时保留属性修改
        applyAttributeModifiers(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // 玩家登录时应用修改
        applyAttributeModifiers(event.getEntity());
        
        // 更新玩家数量倍数并重新应用所有实体修改
        MinecraftServer server = event.getEntity().getServer();
        if (server != null) {
            playerCountMultiplier = calculatePlayerCountMultiplier(server);
            // 重新应用所有实体的属性修改
            for (var player : server.getPlayerList().getPlayers()) {
                applyAttributeModifiers(player);
            }
        }
    }

    //加载实体属性配置，这个映射必不可少！
    private void loadEntityConfig() {
        loadEntityHealthConfig();
        loadEntityAttackDamageConfig();
        loadEntityArmorConfig();

        // 将配置映射传递给 EntityEvent 类
        EntityEvent.setEntityHealthMap(entityHealthMap);
        EntityEvent.setEntityAttackDamageMap(entityAttackDamageMap);
        EntityEvent.setEntityArmorMap(entityArmorMap);
        // 传递玩家数量倍数计算器
        EntityEvent.setPlayerCountMultiplierCalculator(this::calculatePlayerCountMultiplier);

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
                    LOGGER.warn("Invalid health value in entity health config: " + setting);
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
                    LOGGER.warn("Invalid attack damage value in entity attack config: " + setting);
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
                    LOGGER.warn("Invalid armor value in entity armor config: " + setting);
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

        // 移除现有的修改器（通过 UUID）
        if (attributeInstance.getModifier(HEALTH_MODIFIER_UUID) != null) {
            attributeInstance.removeModifier(HEALTH_MODIFIER_UUID);
        }

        // 添加新的生命值修改器
        AttributeModifier healthModifier = new AttributeModifier(
                HEALTH_MODIFIER_UUID,
                "Custom Health Boost",
                additionalHealth,
                AttributeModifier.Operation.ADDITION
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

        // 移除现有的修改器（通过 UUID）
        if (attributeInstance.getModifier(ATTACK_DAMAGE_MODIFIER_UUID) != null) {
            attributeInstance.removeModifier(ATTACK_DAMAGE_MODIFIER_UUID);
        }

        // 添加新的攻击力修改器
        AttributeModifier attackDamageModifier = new AttributeModifier(
                ATTACK_DAMAGE_MODIFIER_UUID,
                "Custom Attack Damage Boost",
                additionalAttackDamage,
                AttributeModifier.Operation.ADDITION
        );

        attributeInstance.addPermanentModifier(attackDamageModifier);
    }

    /**
     * 获取当前的玩家数量倍数
     */
    public double getPlayerCountMultiplier() {
        return playerCountMultiplier;
    }

    private void applyArmorModifier(Player player) {
        // 从Config配置获取防御力增量
        double additionalArmor = Config.COMMON.additionalArmor.get();

        // 获取属性实例
        var attributeInstance = player.getAttribute(Attributes.ARMOR);

        // 移除现有的修改器（通过 UUID）
        if (attributeInstance.getModifier(ARMOR_MODIFIER_UUID) != null) {
            attributeInstance.removeModifier(ARMOR_MODIFIER_UUID);
        }

        // 添加新的防御力修改器
        AttributeModifier armorModifier = new AttributeModifier(
                ARMOR_MODIFIER_UUID,
                "Custom Armor Boost",
                additionalArmor,
                AttributeModifier.Operation.ADDITION
        );

        attributeInstance.addPermanentModifier(armorModifier);
    }

    // 服务端启动测试
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // 没啥用的监听器（保留这个主要是测试用）
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
