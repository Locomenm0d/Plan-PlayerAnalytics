package com.djrapitops.pluginbridge.plan;

import com.djrapitops.pluginbridge.plan.advancedachievements.AdvancedAchievementsHook;
import com.djrapitops.pluginbridge.plan.askyblock.ASkyBlockHook;
import com.djrapitops.pluginbridge.plan.essentials.EssentialsHook;
import com.djrapitops.pluginbridge.plan.factions.FactionsHook;
import com.djrapitops.pluginbridge.plan.griefprevention.GriefPreventionHook;
import com.djrapitops.pluginbridge.plan.jobs.JobsHook;
import com.djrapitops.pluginbridge.plan.litebans.LiteBansHook;
import com.djrapitops.pluginbridge.plan.mcmmo.McmmoHook;
import com.djrapitops.pluginbridge.plan.ontime.OnTimeHook;
import com.djrapitops.pluginbridge.plan.superbvote.SuperbVoteHook;
import com.djrapitops.pluginbridge.plan.towny.TownyHook;
import com.djrapitops.pluginbridge.plan.vault.VaultHook;
import com.djrapitops.pluginbridge.plan.viaversion.ViaVersionHook;
import main.java.com.djrapitops.plan.data.additional.HookHandler;

/**
 *
 * @author Rsl1122
 */
public class Bridge {

    public static void hook(HookHandler handler) {
        try {
            AdvancedAchievementsHook advancedAchievementsHook = new AdvancedAchievementsHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            EssentialsHook essentialsHook = new EssentialsHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            FactionsHook factionsHook = new FactionsHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            McmmoHook mcMmoHook = new McmmoHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            JobsHook jobsHook = new JobsHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            OnTimeHook onTimeHook = new OnTimeHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            TownyHook townyHook = new TownyHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            VaultHook vaultHook = new VaultHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            ASkyBlockHook askyblockHook = new ASkyBlockHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            GriefPreventionHook griefPrevHook = new GriefPreventionHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            LiteBansHook hook = new LiteBansHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            SuperbVoteHook hook = new SuperbVoteHook(handler);
        } catch (NoClassDefFoundError e) {
        }
        try {
            ViaVersionHook hook = new ViaVersionHook(handler);
        } catch (NoClassDefFoundError e) {
        }
    }
}