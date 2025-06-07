/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting;

import client.MapleClient;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.script.*;
import server.MaplePortal;
import tools.FileoutputUtil;

public class PortalScriptManager {

    private static final PortalScriptManager instance = new PortalScriptManager();
    private final Map<String, PortalScript> scripts = new HashMap<>();
    private final static ScriptEngineFactory sef = new ScriptEngineManager().getEngineByName("javascript").getFactory();

    public static PortalScriptManager getInstance() {
        return instance;
    }

    private final PortalScript getPortalScript(final String scriptName) {
        if (scripts.containsKey(scriptName)) {
            return scripts.get(scriptName);
        }

        final String path = "scripts/portal/" + scriptName + ".js";
        ScriptEngine engine = sef.getScriptEngine();
        InputStream in = null;

        try {
            // 优先尝试从文件系统读取
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                in = new FileInputStream(file);
            } else {
                // 若文件系统无对应文件，再从 JAR 包中读取
                in = getClass().getClassLoader().getResourceAsStream(path);
            }

            if (in == null) {
                System.err.println("Portal script not found: " + path);
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Portal script not found: " + path);
                return null;
            }

            try (BufferedReader bf = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String scriptCode = bf.lines().collect(Collectors.joining(System.lineSeparator()));
                ((Compilable) engine).compile(scriptCode).eval();
            }

            final PortalScript script = ((Invocable) engine).getInterface(PortalScript.class);
            scripts.put(scriptName, script);
            return script;

        } catch (Exception e) {
            System.err.println("Error loading portal script: " + scriptName + " : " + e);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing portal script: (" + scriptName + ") " + e);
            return null;
        }
    }


    public final void executePortalScript(final MaplePortal portal, final MapleClient c) {
        final PortalScript script = getPortalScript(portal.getScriptName());

        if (script != null) {
            try {
                script.enter(new PortalPlayerInteraction(c, portal));
                if (c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(6, "Portal : " + portal.getScriptName() + " Map : " + c.getPlayer().getMapId() + " - " + c.getPlayer().getMap().getMapName());
                }
            } catch (Exception e) {
                System.err.println("Error entering Portalscript: " + portal.getScriptName() + " : " + e);
            }
        } else {
            System.out.println("Unhandled portal script " + portal.getScriptName() + " on map " + c.getPlayer().getMapId());
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Unhandled portal script " + portal.getScriptName() + " on map " + c.getPlayer().getMapId());
        }
    }

    public final void clearScripts() {
        scripts.clear();
    }
}
