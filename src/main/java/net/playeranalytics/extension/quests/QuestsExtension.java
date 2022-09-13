/*
    Copyright(c) 2021 AuroraLS3

    The MIT License(MIT)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files(the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions :
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package net.playeranalytics.extension.quests;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.annotation.TableProvider;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import com.djrapitops.plan.query.QueryService;
import me.blackvein.quests.Quest;
import me.blackvein.quests.Quester;
import me.blackvein.quests.Quests;
import me.blackvein.quests.quests.IQuest;
import me.blackvein.quests.storage.Storage;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * DataExtension.
 *
 * @author AuroraLS3
 */
@PluginInfo(name = "Quests", iconName = "book", iconFamily = Family.SOLID, color = Color.LIGHT_GREEN)
public class QuestsExtension implements DataExtension {

    private Quests quests;

    public QuestsExtension() {
        quests = (Quests) Bukkit.getPluginManager().getPlugin("Quests");
    }

    public QuestsExtension(boolean forTesting) {}

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.PLAYER_LEAVE,
                CallEvents.SERVER_EXTENSION_REGISTER,
                CallEvents.SERVER_PERIODICAL
        };
    }

    @TableProvider(tableColor = Color.LIGHT_GREEN)
    public Table playerQuestsTable(UUID playerUUID) {
        Table.Factory table = Table.builder()
                .columnOne("Quest", Icon.called("book").build())
                .columnTwo("Times completed", Icon.called("check-square").of(Family.REGULAR).build());
        try {
            Quester quester = getQuester(playerUUID);
            List<Quest> completedQuests = new ArrayList<>(quester.getCompletedQuests());
            Collections.sort(completedQuests);
            Map<IQuest, Integer> amountsCompleted = quester.getAmountsCompleted();

            for (Quest completedQuest : completedQuests) {
                String questName = completedQuest.getName();
                int amountCompleted = amountsCompleted.getOrDefault(completedQuest, 1);
                table.addRow(questName, amountCompleted);
            }

            return table.build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotReadyException();
        } catch (ExecutionException | TimeoutException | InvocationTargetException | IllegalAccessException e) {
            throw new NotReadyException();
        }
    }

    private Quester getQuester(UUID playerUUID) throws InterruptedException, ExecutionException, TimeoutException, InvocationTargetException, IllegalAccessException {
        Storage storage = quests.getStorage();
        Method getQuesterMethod;
        try {
            getQuesterMethod = storage.getClass().getDeclaredMethod("loadQuesterData", UUID.class);
        } catch (NoSuchMethodException e) {
            try {
                getQuesterMethod = storage.getClass().getDeclaredMethod("loadQuester", UUID.class);
            } catch (NoSuchMethodException e2) {
                throw new NotReadyException();
            }
        }

        Object future = getQuesterMethod.invoke(storage, playerUUID);
        if (!(future instanceof Future)) {
            throw new IllegalStateException("Quests plugin has incompatibly changed, Quests Storage has no loadQuesterData or loadQuester method");
        }
        return ((Future<Quester>) future).get(1, TimeUnit.MINUTES);
    }

    @TableProvider(tableColor = Color.LIGHT_GREEN)
    public Table questCompletedTable() {
        Table.Factory table = Table.builder()
                .columnOne("Quest", Icon.called("book").build())
                .columnTwo("Times completed", Icon.called("check-square").of(Family.REGULAR).build());

        String sql = "SELECT tv.col_1_value as quest, SUM(tv.col_2_value) as completion_times" +
                " FROM plan_extension_user_table_values tv" +
                " JOIN plan_extension_tables t on t.id=tv.table_id" +
                " JOIN plan_extension_plugins p on p.id=t.plugin_id" +
                " WHERE p.name=?" +
                " AND t.name=?" +
                " GROUP BY quest" +
                " ORDER BY quest ASC";
        QueryService.getInstance().query(sql, statement -> {
            statement.setString(1, "Quests");
            statement.setString(2, "playerQuestsTable");
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    table.addRow(set.getString("quest"), set.getInt("completion_times"));
                }
                return true;
            }
        });
        return table.build();
    }
}