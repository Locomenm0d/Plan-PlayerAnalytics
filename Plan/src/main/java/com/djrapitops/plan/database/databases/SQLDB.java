package main.java.com.djrapitops.plan.database.databases;

import com.djrapitops.plugin.task.AbsRunnable;
import main.java.com.djrapitops.plan.Log;
import main.java.com.djrapitops.plan.Plan;
import main.java.com.djrapitops.plan.data.KillData;
import main.java.com.djrapitops.plan.data.SessionData;
import main.java.com.djrapitops.plan.data.UserData;
import main.java.com.djrapitops.plan.data.cache.DBCallableProcessor;
import main.java.com.djrapitops.plan.database.Database;
import main.java.com.djrapitops.plan.database.tables.*;
import main.java.com.djrapitops.plan.utilities.Benchmark;
import main.java.com.djrapitops.plan.utilities.FormatUtils;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author Rsl1122
 */
public abstract class SQLDB extends Database {

    private final boolean supportsModification;

    private Connection connection;

    /**
     *
     * @param plugin
     * @param supportsModification
     */
    public SQLDB(Plan plugin, boolean supportsModification) {
        super(plugin);
        this.supportsModification = supportsModification;
        boolean usingMySQL = getName().equals("MySQL");

        usersTable = new UsersTable(this, usingMySQL);
        gmTimesTable = new GMTimesTable(this, usingMySQL);
        sessionsTable = new SessionsTable(this, usingMySQL);
        killsTable = new KillsTable(this, usingMySQL);
        locationsTable = new LocationsTable(this, usingMySQL);
        ipsTable = new IPsTable(this, usingMySQL);
        nicknamesTable = new NicknamesTable(this, usingMySQL);
        commandUseTable = new CommandUseTable(this, usingMySQL);
        versionTable = new VersionTable(this, usingMySQL);
        tpsTable = new TPSTable(this, usingMySQL);
        securityTable = new SecurityTable(this, usingMySQL);

        startConnectionPingTask();
    }

    /**
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     */
    public void startConnectionPingTask() throws IllegalArgumentException, IllegalStateException {
        // Maintains Connection.
        plugin.getRunnableFactory().createNew(new AbsRunnable("DBConnectionPingTask " + getName()) {
            @Override
            public void run() {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.createStatement().execute("/* ping */ SELECT 1");
                    }
                } catch (SQLException e) {
                    connection = getNewConnection();
                }
            }
        }).runTaskTimerAsynchronously(60 * 20, 60 * 20);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean init() {
        super.init();
        setStatus("Init");
        Benchmark.start("Database: Init " + getConfigName());
        try {
            if (!checkConnection()) {
                return false;
            }
            convertBukkitDataToDB();
            clean();
            return true;
        } catch (SQLException e) {
            Log.toLog(this.getClass().getName(), e);
            return false;
        } finally {
            Benchmark.stop("Database: Init " + getConfigName());
        }
    }

    /**
     *
     * @return @throws SQLException
     */
    public boolean checkConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = getNewConnection();

            if (connection == null || connection.isClosed()) {
                return false;
            }

            boolean newDatabase = true;
            try {
                getVersion();
                newDatabase = false;
            } catch (Exception e) {
            }
            if (!versionTable.createTable()) {
                Log.error("Failed to create table: " + versionTable.getTableName());
                return false;
            }
            if (newDatabase) {
                Log.info("New Database created.");
                setVersion(5);
            }
            Benchmark.start("Database: Create tables");
            for (Table table : getAllTables()) {
                if (!table.createTable()) {
                    Log.error("Failed to create table: " + table.getTableName());
                    return false;
                }
            }
            if (!securityTable.createTable()) {
                Log.error("Failed to create table: " + securityTable.getTableName());
                return false;
            }
            Benchmark.stop("Database: Create tables");
            if (!newDatabase && getVersion() < 5) {
                setVersion(5);
            }
        }
        return true;
    }

    /**
     *
     */
    public void convertBukkitDataToDB() {
        plugin.getRunnableFactory().createNew(new AbsRunnable("BukkitDataConversionTask") {
            @Override
            public void run() {
                try {
                    Benchmark.start("Database: Convert Bukkitdata to DB data");
                    Set<UUID> uuids = usersTable.getSavedUUIDs();
                    uuids.removeAll(usersTable.getContainsBukkitData(uuids));
                    if (uuids.isEmpty()) {
                        Log.debug("No conversion necessary.");
                        return;
                    }
                    setStatus("Bukkit Data Conversion");
                    Log.info("Beginning Bukkit Data -> DB Conversion for " + uuids.size() + " players");
                    int id = plugin.getBootAnalysisTaskID();
                    if (id != -1) {
                        Log.info("Analysis | Cancelled Boot Analysis Due to conversion.");
                        plugin.getServer().getScheduler().cancelTask(id);
                    }
                    saveMultipleUserData(getUserDataForUUIDS(uuids));
                    Log.info("Conversion complete, took: " + FormatUtils.formatTimeAmount(Benchmark.stop("Database: Convert Bukkitdata to DB data")) + " ms");
                } catch (SQLException ex) {
                    Log.toLog(this.getClass().getName(), ex);
                } finally {
                    setAvailable();
                    this.cancel();
                }
            }
        }).runTaskAsynchronously();
    }

    /**
     *
     * @return
     */
    public Table[] getAllTables() {
        return new Table[]{usersTable, gmTimesTable, ipsTable, nicknamesTable, sessionsTable, killsTable, commandUseTable, tpsTable};
    }

    /**
     *
     * @return
     */
    public Table[] getAllTablesInRemoveOrder() {
        return new Table[]{locationsTable, gmTimesTable, ipsTable, nicknamesTable, sessionsTable, killsTable, usersTable, commandUseTable, tpsTable};
    }

    /**
     *
     * @return
     */
    public abstract Connection getNewConnection();

    /**
     *
     * @throws SQLException
     */
    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
        setStatus("Closed");
    }

    /**
     *
     * @return @throws SQLException
     */
    @Override
    public int getVersion() throws SQLException {
        return versionTable.getVersion();
    }

    /**
     *
     * @param version
     * @throws SQLException
     */
    @Override
    public void setVersion(int version) throws SQLException {
        versionTable.setVersion(version);
    }

    /**
     *
     * @param uuid
     * @return
     */
    @Override
    public boolean wasSeenBefore(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        setStatus("User exist check");
        try {
            return usersTable.getUserId(uuid.toString()) != -1;
        } catch (SQLException e) {
            Log.toLog(this.getClass().getName(), e);
            return false;
        } finally {
            setAvailable();
        }
    }

    /**
     *
     * @param uuid
     * @return
     * @throws SQLException
     */
    @Override
    public boolean removeAccount(String uuid) throws SQLException {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        try {
            setStatus("Remove account " + uuid);
            Benchmark.start("Database: Remove Account");
            Log.debug("Removing Account: " + uuid);
            try {
                checkConnection();
            } catch (Exception e) {
                Log.toLog(this.getClass().getName(), e);
                return false;
            }
            int userId = usersTable.getUserId(uuid);
            return userId != -1 && locationsTable.removeUserLocations(userId) && ipsTable.removeUserIps(userId) && nicknamesTable.removeUserNicknames(userId) && gmTimesTable.removeUserGMTimes(userId) && sessionsTable.removeUserSessions(userId) && killsTable.removeUserKillsAndVictims(userId) && usersTable.removeUser(uuid);
        } finally {
            Benchmark.stop("Database: Remove Account");
            setAvailable();
        }
    }

    /**
     *
     * @param uuid
     * @param processors
     * @throws SQLException
     */
    @Override
    public void giveUserDataToProcessors(UUID uuid, Collection<DBCallableProcessor> processors) throws SQLException {
        Benchmark.start("Database: Give userdata to processors");
        try {
            checkConnection();
        } catch (Exception e) {
            Log.toLog(this.getClass().getName(), e);
            return;
        }
        // Check if user is in the database
        if (!wasSeenBefore(uuid)) {
            return;
        }
        setStatus("Get single userdata for " + uuid);
        // Get the data
        UserData data = usersTable.getUserData(uuid);

        int userId = usersTable.getUserId(uuid);

        List<String> nicknames = nicknamesTable.getNicknames(userId);
        data.addNicknames(nicknames);
        if (nicknames.size() > 0) {
            data.setLastNick(nicknames.get(nicknames.size() - 1));
        }

        List<InetAddress> ips = ipsTable.getIPAddresses(userId);
        data.addIpAddresses(ips);

        Map<String, Long> times = gmTimesTable.getGMTimes(userId);
        data.setGmTimes(times);
        List<SessionData> sessions = sessionsTable.getSessionData(userId);
        data.addSessions(sessions);
        data.setPlayerKills(killsTable.getPlayerKills(userId));
        processors.stream().forEach((processor) -> processor.process(data));
        Benchmark.stop("Database: Give userdata to processors");
        setAvailable();
    }

    /**
     *
     * @param uuidsCol
     * @return
     * @throws SQLException
     */
    @Override
    public List<UserData> getUserDataForUUIDS(Collection<UUID> uuidsCol) throws SQLException {
        if (uuidsCol == null || uuidsCol.isEmpty()) {
            return new ArrayList<>();
        }
        setStatus("Get userdata (multiple) for: " + uuidsCol.size());
        Benchmark.start("Database: Get UserData for " + uuidsCol.size());
        Map<UUID, Integer> userIds = usersTable.getAllUserIds();
        Set<UUID> remove = uuidsCol.stream()
                .filter((uuid) -> (!userIds.containsKey(uuid)))
                .collect(Collectors.toSet());
        List<UUID> uuids = new ArrayList<>(uuidsCol);
        Log.debug("Data not found for: " + remove.size());
        uuids.removeAll(remove);
        Benchmark.start("Database: Create UserData objects for " + userIds.size());
        List<UserData> data = usersTable.getUserData(new ArrayList<>(uuids));
        Benchmark.stop("Database: Create UserData objects for " + userIds.size());
        if (data.isEmpty()) {
            return data;
        }
        Map<Integer, UUID> idUuidRel = userIds.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        List<Integer> ids = userIds.entrySet().stream().filter(e -> uuids.contains(e.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());
        Log.debug("Ids: " + ids.size());
        Map<Integer, List<String>> nicknames = nicknamesTable.getNicknames(ids);
        Map<Integer, Set<InetAddress>> ipList = ipsTable.getIPList(ids);
        Map<Integer, List<KillData>> playerKills = killsTable.getPlayerKills(ids, idUuidRel);
        Map<Integer, List<SessionData>> sessionData = sessionsTable.getSessionData(ids);
        Map<Integer, Map<String, Long>> gmTimes = gmTimesTable.getGMTimes(ids);
        Log.debug("Sizes: UUID:" + uuids.size() + " DATA:" + data.size() + " ID:" + userIds.size() + " N:" + nicknames.size() + " I:" + ipList.size() + " K:" + playerKills.size() + " S:" + sessionData.size());
        for (UserData uData : data) {
            UUID uuid = uData.getUuid();
            Integer id = userIds.get(uuid);
            uData.addIpAddresses(ipList.get(id));
            uData.addNicknames(nicknames.get(id));
            uData.addSessions(sessionData.get(id));
            uData.setPlayerKills(playerKills.get(id));
            uData.setGmTimes(gmTimes.get(id));
        }
        Benchmark.stop("Database: Get UserData for " + uuidsCol.size());
        setAvailable();
        return data;
    }

    /**
     *
     * @param data
     * @throws SQLException
     */
    @Override
    public void saveMultipleUserData(Collection<UserData> data) throws SQLException {
        Benchmark.start("Database: Save multiple Userdata");
        checkConnection();
        if (data.isEmpty()) {
            return;
        }
        setStatus("Save userdata (multiple) for " + data.size());
        usersTable.saveUserDataInformationBatch(data);
        // Transform to map
        Map<UUID, UserData> userDatas = data.stream().collect(Collectors.toMap(UserData::getUuid, Function.identity()));
        // Get UserIDs
        Map<UUID, Integer> userIds = usersTable.getAllUserIds();
        // Empty dataset
        Map<Integer, Set<String>> nicknames = new HashMap<>();
        Map<Integer, String> lastNicks = new HashMap<>();
        Map<Integer, Set<InetAddress>> ips = new HashMap<>();
        Map<Integer, List<KillData>> kills = new HashMap<>();
        Map<Integer, UUID> uuids = userIds.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        Map<Integer, List<SessionData>> sessions = new HashMap<>();
        Map<Integer, Map<String, Long>> gmTimes = new HashMap<>();
        // Put to dataset
        for (UUID uuid : userDatas.keySet()) {
            Integer id = userIds.get(uuid);
            UserData uData = userDatas.get(uuid);
            if (id == -1) {
                Log.debug("User not seen before, saving last: " + uuid);
                continue;
            }
            uData.access();
            nicknames.put(id, new HashSet<>(uData.getNicknames()));
            lastNicks.put(id, uData.getLastNick());
            ips.put(id, new HashSet<>(uData.getIps()));
            kills.put(id, new ArrayList<>(uData.getPlayerKills()));
            sessions.put(id, new ArrayList<>(uData.getSessions()));
            gmTimes.put(id, uData.getGmTimes());
        }
        // Save
        nicknamesTable.saveNickLists(nicknames, lastNicks);
        ipsTable.saveIPList(ips);
        killsTable.savePlayerKills(kills, uuids);
        sessionsTable.saveSessionData(sessions);
        gmTimesTable.saveGMTimes(gmTimes);
        userDatas.values().stream()
                .filter(Objects::nonNull)
                .filter(UserData::isAccessed)
                .forEach(UserData::stopAccessing);
        Benchmark.stop("Database: Save multiple Userdata");
        setAvailable();
    }

    /**
     *
     * @param data
     * @throws SQLException
     */
    @Override
    public void saveUserData(UserData data) throws SQLException {
        if (data == null) {
            return;
        }
        UUID uuid = data.getUuid();
        if (uuid == null) {
            return;
        }
        setStatus("Save userdata: " + uuid);
        checkConnection();
        Log.debug("DB_Save: " + data);
        data.access();
        usersTable.saveUserDataInformation(data);
        int userId = usersTable.getUserId(uuid.toString());
        sessionsTable.saveSessionData(userId, new ArrayList<>(data.getSessions()));
        nicknamesTable.saveNickList(userId, new HashSet<>(data.getNicknames()), data.getLastNick());
        ipsTable.saveIPList(userId, new HashSet<>(data.getIps()));
        killsTable.savePlayerKills(userId, new ArrayList<>(data.getPlayerKills()));
        gmTimesTable.saveGMTimes(userId, data.getGmTimes());
        data.stopAccessing();
        setAvailable();
    }

    /**
     *
     */
    @Override
    public void clean() {
        Log.info("Cleaning the database.");
        try {
            checkConnection();
            tpsTable.clean();
            locationsTable.removeAllData();
//            sessionsTable.clean();
            Log.info("Clean complete.");
        } catch (SQLException e) {
            Log.toLog(this.getClass().getName(), e);
        }
    }

    /**
     *
     * @return
     */
    @Override
    public boolean removeAllData() {
        setStatus("Clearing all data");
        for (Table table : getAllTablesInRemoveOrder()) {
            if (!table.removeAllData()) {
                return false;
            }
        }
        setAvailable();
        return true;
    }

    /**
     *
     * @return
     */
    public boolean supportsModification() {
        return supportsModification;
    }

    /**
     *
     * @return
     */
    public Connection getConnection() {
        return connection;
    }

    private void setStatus(String status) {
        plugin.processStatus().setStatus("DB-" + getName(), status);
    }

    private void setAvailable() {
        setStatus("Running");
    }
}
