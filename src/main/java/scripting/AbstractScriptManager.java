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
import java.io.File;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.FileInputStream;
import tools.FileoutputUtil;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Matze
 */
public abstract class AbstractScriptManager {

    private static final ScriptEngineManager sem = new ScriptEngineManager();

    protected Invocable getInvocable(String path, MapleClient c) {
        return getInvocable(path, c, false);
    }

    protected Invocable getInvocable(String path, MapleClient c, boolean npc) {
        try {
            path = "scripts/" + path;
            ScriptEngine engine = null;

            if (c != null) {
                engine = c.getScriptEngine(path);
            }

            if (engine == null) {
                InputStream in = null;

                // 优先尝试读取文件系统
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    in = new FileInputStream(file);
                } else {
                    // 若文件系统无对应文件，再从 JAR 包内读取
                    in = getClass().getResourceAsStream("/" + path);
                }

                if (in == null) {
                    FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Script not found: " + path);
                    return null;
                }

                engine = sem.getEngineByName("graal.js");
                if (engine == null) {
                    engine = sem.getEngineByName("nashorn");
                }
                if (engine == null) {
                    throw new RuntimeException("No JavaScript engine available.");
                }

                try (BufferedReader bf = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String lines = "load('nashorn:mozilla_compat.js');" +
                            bf.lines().collect(Collectors.joining(System.lineSeparator()));
                    engine.eval(lines);
                }

                if (c != null) {
                    c.setScriptEngine(path, engine);
                }
            }

            return (Invocable) engine;
        } catch (Exception e) {
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error in script: " + path);
            return null;
        }
    }
}
