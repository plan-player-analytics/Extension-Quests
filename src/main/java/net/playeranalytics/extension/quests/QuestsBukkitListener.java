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

import com.djrapitops.plan.extension.Caller;
import com.djrapitops.plan.settings.ListenerService;
import com.djrapitops.plan.settings.SchedulerService;
import me.blackvein.quests.Quester;
import me.blackvein.quests.events.quester.QuesterPostChangeStageEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class QuestsBukkitListener extends QuestsListener implements Listener {

    public QuestsBukkitListener(Caller caller) {
        super(caller);
    }

    @Override
    public void register() {
        ListenerService.getInstance().registerListenerForPlan(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangeStage(QuesterPostChangeStageEvent event) {
        Quester quester = event.getQuester();
        SchedulerService.getInstance().runAsync(() -> caller.updatePlayerData(quester.getUUID(), quester.getPlayer().getName()));
    }
}