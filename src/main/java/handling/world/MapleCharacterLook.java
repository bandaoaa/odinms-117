/*
 * This file is part of the OdinMS MapleStory Private Server
 * Copyright (C) 2012 Patrick Huy and Matthias Butz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.world;

import client.MapleCharacter;

import java.util.Map;

/**
 * @author AlphaEta
 */
public interface MapleCharacterLook {

    public byte getGender();

    public byte getSkinColor();

    public int getFace();

    public int getHair();

    public int getDemonMarking();

    public short getJob();

    //public boolean isElf();

    public Map<Byte, Integer> getEquips();

    public boolean isElf(MapleCharacter player);
}
