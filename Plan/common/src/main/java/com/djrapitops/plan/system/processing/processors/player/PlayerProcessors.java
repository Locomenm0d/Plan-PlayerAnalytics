/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.system.processing.processors.player;

import com.djrapitops.plan.system.database.DBSystem;
import dagger.Lazy;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Factory for creating Runnables related to Player data to run with {@link com.djrapitops.plan.system.processing.Processing}.
 *
 * @author Rsl1122
 */
@Singleton
public class PlayerProcessors {

    private final Lazy<DBSystem> dbSystem;

    @Inject
    public PlayerProcessors(
            Lazy<DBSystem> dbSystem
    ) {
        this.dbSystem = dbSystem;
    }

    public BanAndOpProcessor banAndOpProcessor(UUID uuid, BooleanSupplier banned, boolean op) {
        return new BanAndOpProcessor(uuid, banned, op, dbSystem.get().getDatabase());
    }
}