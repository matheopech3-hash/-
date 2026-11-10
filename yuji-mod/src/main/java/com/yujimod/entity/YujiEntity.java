package com.yujimod.entity;

import com.yujimod.entity.ai.YujiCombatGoal;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Yuji Itadori : allié apprivoisable (pomme dorée) qui combat les monstres.
 *
 * Techniques :
 *  - Coup de poing       : attaque de base
 *  - Black Flash         : proc aléatoire, dégâts x 2.5^2.5 ; déclenche "la Zone" (proc plus fréquents)
 *  - Divergent Fist      : 1er impact normal, 2e impact retardé qui ignore l'armure
 *  - Manji Kick          : saut + coup de pied qui projette l'ennemi
 *  - Renforcement d'énergie occulte : buff Force/Vitesse/Résistance sous 50 % de vie
 */
public class YujiEntity extends TamableAnimal implements GeoEntity {

    public enum Technique {
        //            anim              windup (ticks avant l'impact) / recovery (ticks de repos après)
        PUNCH("punch", 6, 14),
        BLACK_FLASH("black_flash", 14, 36),
        DIVERGENT_FIST("divergent_fist", 10, 30),
        MANJI_KICK("manji_kick", 12, 24);

        public final String anim;
        public final int windup;
        public final int recovery;

        Technique(String anim, int windup, int recovery) {
            this.anim = anim;
            this.windup = windup;
            this.recovery = recovery;
        }
    }

    /** 2.5^2.5 ~ 9.88 : le multiplicateur "officiel" du Black Flash. */
    private static final float BLACK_FLASH_MULT = (float) Math.pow(2.5D, 2.5D);

    private static final String ANIM_PREFIX = "animation.yuji_itadori.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(ANIM_PREFIX + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(ANIM_PREFIX + "walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop(ANIM_PREFIX + "run");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // --- état des techniques (serveur uniquement, non sauvegardé) ---
    private Technique active;
    private int activeTicks;
    private LivingEntity activeTarget;
    private int attackCooldown;
    private int kickCooldown;
    private int divergentCooldown;
    private int ceCooldown;
    private int zoneTicks;
    private int streak;
    private LivingEntity divergentTarget;
    private int divergentDelay;
    private float divergentDamage;

    public YujiEntity(EntityType<? extends YujiEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    // ------------------------------------------------------------------ IA
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new YujiCombatGoal(this));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.2D, 8.0F, 2.5F, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, null));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // jamais son propriétaire
        if (target != null && this.isOwnedBy(target)) return;
        super.setTarget(target);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Player) return false;
        if (target instanceof TamableAnimal other && other.isTame()) return false;
        return true;
    }

    // --------------------------------------------------------- interaction
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame()) {
            if (stack.is(Items.GOLDEN_APPLE)) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(false);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                    player.displayClientMessage(Component.translatable("message.yujimod.tamed"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            return InteractionResult.PASS;
        }

        if (this.isOwnedBy(player)) {
            // nourriture = soin
            FoodProperties food = stack.getItem().getFoodProperties();
            if (food != null && this.getHealth() < this.getMaxHealth()) {
                if (!this.level().isClientSide) {
                    this.heal(food.getNutrition() * 1.5F);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            // main vide = s'asseoir / se lever
            if (hand == InteractionHand.MAIN_HAND) {
                if (!this.level().isClientSide) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.jumping = false;
                    this.navigation.stop();
                    this.setTarget(null);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        return super.mobInteract(player, hand);
    }

    // ------------------------------------------------------------ techniques
    public boolean isTechniqueActive() {
        return active != null;
    }

    private float blackFlashChance() {
        return zoneTicks > 0 ? Math.min(0.65F, 0.35F + 0.08F * streak) : 0.08F;
    }

    /** Appelé chaque tick par YujiCombatGoal quand une cible existe. */
    public void combatTick(LivingEntity target) {
        if (active != null || attackCooldown > 0) return;

        double gap = Math.sqrt(this.distanceToSqr(target)) - target.getBbWidth() * 0.5D;

        if (kickCooldown <= 0 && this.onGround() && gap > 3.5D && gap < 8.0D && this.hasLineOfSight(target)) {
            startTechnique(Technique.MANJI_KICK, target);
        } else if (gap <= 2.6D) {
            if (divergentCooldown <= 0 && this.random.nextFloat() < 0.22F) {
                startTechnique(Technique.DIVERGENT_FIST, target);
            } else if (this.random.nextFloat() < blackFlashChance()) {
                startTechnique(Technique.BLACK_FLASH, target);
            } else {
                startTechnique(Technique.PUNCH, target);
            }
        }
    }

    private void startTechnique(Technique tech, LivingEntity target) {
        this.active = tech;
        this.activeTicks = tech.windup;
        this.activeTarget = target;

        // se tourner franchement vers la cible
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;

        this.triggerAnim("technique", tech.anim);

        switch (tech) {
            case MANJI_KICK -> {
                kickCooldown = 120;
                Vec3 dir = new Vec3(dx, 0, dz).normalize();
                this.setDeltaMovement(dir.x * 0.9D, 0.42D, dir.z * 0.9D);
                this.hasImpulse = true;
                announce("manji_kick");
            }
            case DIVERGENT_FIST -> {
                divergentCooldown = 100;
                announce("divergent_fist");
            }
            case BLACK_FLASH -> announce("black_flash");
            default -> {
            }
        }
    }

    private static double reach(Technique tech) {
        return tech == Technique.MANJI_KICK ? 4.2D : 3.4D;
    }

    private void resolveTechnique(Technique tech, LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        double gap = Math.sqrt(this.distanceToSqr(target)) - target.getBbWidth() * 0.5D;
        if (gap > reach(tech)) return; // raté

        float base = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        switch (tech) {
            case PUNCH -> {
                if (strike(target, base, 0.8F)) this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
            }
            case BLACK_FLASH -> blackFlash(target, base);
            case DIVERGENT_FIST -> divergentFist(target, base);
            case MANJI_KICK -> {
                if (strike(target, base * 1.4F, 2.0F)) {
                    this.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.9F);
                    sparks(target, ParticleTypes.CRIT, 20);
                }
            }
        }
    }

    private boolean strike(LivingEntity target, float damage, float knockback) {
        boolean hit = target.hurt(this.damageSources().mobAttack(this), damage);
        if (hit) {
            target.knockback(knockback * 0.5D, this.getX() - target.getX(), this.getZ() - target.getZ());
        }
        return hit;
    }

    private void blackFlash(LivingEntity target, float base) {
        if (strike(target, base * BLACK_FLASH_MULT, 2.4F)) {
            zoneTicks = 200; // "la Zone" : 10 s
            streak++;
            blackFlashFx(target);
            this.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.3F, 1.4F);
            this.playSound(SoundEvents.GENERIC_EXPLODE, 0.5F, 1.6F);
        }
    }

    private void divergentFist(LivingEntity target, float base) {
        if (strike(target, base * 0.9F, 0.4F)) {
            this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
            divergentTarget = target;
            divergentDelay = 14; // le 2e impact arrive ~0.7 s plus tard
            divergentDamage = base * 2.2F;
        }
    }

    private void resolveDivergent() {
        LivingEntity t = divergentTarget;
        divergentTarget = null;
        if (t == null || !t.isAlive()) return;

        t.invulnerableTime = 0;
        // dégâts magiques indirects : ignorent l'armure
        if (t.hurt(this.damageSources().indirectMagic(this, this), divergentDamage)) {
            t.knockback(1.0D, this.getX() - t.getX(), this.getZ() - t.getZ());
            this.playSound(SoundEvents.GENERIC_EXPLODE, 0.6F, 1.3F);
            if (this.level() instanceof ServerLevel sl) {
                Vec3 c = t.position().add(0, t.getBbHeight() * 0.5D, 0);
                DustParticleOptions red = new DustParticleOptions(new Vector3f(0.9F, 0.05F, 0.1F), 1.6F);
                sl.sendParticles(red, c.x, c.y, c.z, 25, 0.5, 0.5, 0.5, 0.0);
                sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void activateReinforcement() {
        ceCooldown = 1200;
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
        announce("reinforcement");
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.0D, getZ(), 30, 0.4, 0.8, 0.4, 0.02);
        }
    }

    // ------------------------------------------------------------------ FX
    private void blackFlashFx(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.5D, 0);
        DustParticleOptions black = new DustParticleOptions(new Vector3f(0.05F, 0.0F, 0.08F), 2.2F);
        DustParticleOptions red = new DustParticleOptions(new Vector3f(0.9F, 0.05F, 0.1F), 1.6F);

        // nuage noir/rouge
        for (int i = 0; i < 40; i++) {
            double ox = this.random.nextGaussian() * 0.5D;
            double oy = this.random.nextGaussian() * 0.5D;
            double oz = this.random.nextGaussian() * 0.5D;
            sl.sendParticles(i % 3 == 0 ? red : black, c.x + ox, c.y + oy, c.z + oz, 1, 0, 0, 0, 0);
        }
        // éclair rouge en zigzag qui tombe sur la cible
        double x = c.x;
        double z = c.z;
        for (int i = 0; i < 14; i++) {
            x += (this.random.nextDouble() - 0.5D) * 0.5D;
            z += (this.random.nextDouble() - 0.5D) * 0.5D;
            sl.sendParticles(red, x, c.y + 3.0D - i * 0.3D, z, 2, 0.02, 0.02, 0.02, 0.0);
        }
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 30, 0.5, 0.5, 0.5, 0.4);
        sl.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 20, 0.4, 0.4, 0.4, 0.5);
    }

    private void sparks(LivingEntity target, net.minecraft.core.particles.ParticleOptions type, int count) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(type, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    count, 0.3, 0.3, 0.3, 0.3);
        }
    }

    private void announce(String key) {
        for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0D))) {
            p.displayClientMessage(Component.translatable("technique.yujimod." + key), true);
        }
    }

    // ---------------------------------------------------------------- tick
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.isAlive()) {
            serverTick();
        }
    }

    private void serverTick() {
        if (attackCooldown > 0) attackCooldown--;
        if (kickCooldown > 0) kickCooldown--;
        if (divergentCooldown > 0) divergentCooldown--;
        if (ceCooldown > 0) ceCooldown--;
        if (zoneTicks > 0 && --zoneTicks == 0) streak = 0;

        if (active != null && --activeTicks <= 0) {
            Technique tech = active;
            LivingEntity target = activeTarget;
            active = null;
            activeTarget = null;
            resolveTechnique(tech, target);
            attackCooldown = tech.recovery;
        }
        if (divergentDelay > 0 && --divergentDelay == 0) {
            resolveDivergent();
        }
        if (ceCooldown <= 0 && this.getTarget() != null && this.getHealth() < this.getMaxHealth() * 0.5F) {
            activateReinforcement();
        }
    }

    // -------------------------------------------------------------- GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 5, this::moveController));
        controllers.add(new AnimationController<>(this, "technique", 0, state -> PlayState.STOP)
                .triggerableAnim("punch", once("punch"))
                .triggerableAnim("black_flash", once("black_flash"))
                .triggerableAnim("divergent_fist", once("divergent_fist"))
                .triggerableAnim("manji_kick", once("manji_kick")));
    }

    private static RawAnimation once(String name) {
        return RawAnimation.begin().then(ANIM_PREFIX + name, Animation.LoopType.PLAY_ONCE);
    }

    private PlayState moveController(AnimationState<YujiEntity> state) {
        if (state.isMoving()) {
            return state.setAndContinue(this.isAggressive() ? RUN : WALK);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ---------------------------------------------------------------- divers
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }
}
