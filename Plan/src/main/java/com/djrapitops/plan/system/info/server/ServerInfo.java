/*
 * Licence is provided in the jar as license.yml also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/license.yml
 */
package com.djrapitops.plan.system.info.server;

import com.djrapitops.plan.api.exceptions.EnableException;
import com.djrapitops.plan.system.PlanSystem;
import com.djrapitops.plan.system.SubSystem;
import com.djrapitops.plan.utilities.NullCheck;

import java.util.UUID;

/**
 * SubSystem for managing Server information.
 *
 * Most information is accessible via static methods.
 *
 * @author Rsl1122
 */
public abstract class ServerInfo implements SubSystem {

    protected Server server;
    protected ServerProperties serverProperties;

    public static ServerInfo getInstance() {
        ServerInfo serverInfo = PlanSystem.getInstance().getServerInfo();
        NullCheck.check(serverInfo, new IllegalStateException("ServerInfo was not initialized."));
        return serverInfo;
    }

    public static Server getServer() {
        return getInstance().server;
    }

    public static ServerProperties getServerProperties() {
        return getInstance().serverProperties;
    }

    public static UUID getServerUUID() {
        return getServer().getUuid();
    }

    public static String getServerName() {
        return getServer().getName();
    }

    public static int getServerID() {
        return getServer().getId();
    }

    @Override
    public void enable() throws EnableException {
        // ServerProperties are required when creating Server
        NullCheck.check(serverProperties, new IllegalStateException("Server Properties did not load!"));
        server = loadServerInfo();
        NullCheck.check(server, new IllegalStateException("Server information did not load!"));
    }

    protected abstract Server loadServerInfo() throws EnableException;

    @Override
    public void disable() {

    }
}