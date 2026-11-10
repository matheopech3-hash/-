package com.yujimod.entity.ai;

import com.yujimod.entity.YujiEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Poursuit la cible et délègue le choix / l'exécution des techniques à YujiEntity. */
public class YujiCombatGoal extends Goal {
    private final YujiEntity yuji;
    private int repath;

    public YujiCombatGoal(YujiEntity yuji) {
        this.yuji = yuji;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = yuji.getTarget();
        return t != null && t.isAlive() && !yuji.isInSittingPose();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        yuji.setAggressive(true);
        repath = 0;
    }

    @Override
    public void stop() {
        yuji.setAggressive(false);
        yuji.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity t = yuji.getTarget();
        if (t == null) return;

        yuji.getLookControl().setLookAt(t, 30.0F, 30.0F);

        if (yuji.isTechniqueActive()) {
            yuji.getNavigation().stop();
            return;
        }
        if (--repath <= 0) {
            repath = 4 + yuji.getRandom().nextInt(7);
            yuji.getNavigation().moveTo(t, 1.3D);
        }
        yuji.combatTick(t);
    }
}
