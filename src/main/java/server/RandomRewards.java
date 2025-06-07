package server;

import constants.GameConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomRewards {

    private static List<Integer> compiledGold = null;
    private static List<Integer> compiledSilver = null;
    private static List<Integer> compiledFishing = null;
    private static List<Integer> compiledPeanut = null;
    private static List<Integer> compiledEvent = null;
    private static List<Integer> compiledEventC = null;
    private static List<Integer> compiledEventB = null;
    private static List<Integer> compiledEventA = null;
    private static List<Integer> compiledDrops = null;
    private static List<Integer> compiledDropsB = null;
    private static List<Integer> compiledDropsA = null;
    private static List<Integer> tenPercent = null;
    private static List<Integer> compiledCashSurprise = null;

    private final static RandomRewards instance = new RandomRewards();

    static {
        // Gold Box
        List<Integer> returnArray = new ArrayList<>();

        compiledGold = returnArray;

        // Silver Box
        returnArray = new ArrayList<>();

        compiledSilver = returnArray;

        // Fishing Rewards
        returnArray = new ArrayList<>();

        processRewards(returnArray, GameConstants.fishingReward);

        compiledFishing = returnArray;

        // Event Rewards
        returnArray = new ArrayList<>();

        compiledEventC = returnArray;

        returnArray = new ArrayList<>();

        compiledEventB = returnArray;

        returnArray = new ArrayList<>();

        processRewardsSimple(returnArray, GameConstants.tenPercent);
        processRewardsSimple(returnArray, GameConstants.tenPercent);//hack: chance = 2

        compiledEventA = returnArray;

        returnArray = new ArrayList<>();

        compiledEvent = returnArray;

        returnArray = new ArrayList<>();

        compiledPeanut = returnArray;

        returnArray = new ArrayList<>();

        processRewardsSimple(returnArray, GameConstants.normalDrops);

        compiledDrops = returnArray;

        returnArray = new ArrayList<>();

        processRewardsSimple(returnArray, GameConstants.rareDrops);

        compiledDropsB = returnArray;

        returnArray = new ArrayList<>();

        processRewardsSimple(returnArray, GameConstants.superDrops);

        compiledDropsA = returnArray;

        returnArray = new ArrayList<>();
        processRewardsSimple(returnArray, GameConstants.tenPercent);
        tenPercent = returnArray;

        //神奇服飾箱相關內容
        returnArray = new ArrayList<>();
        processRewards(returnArray, GameConstants.cashSurpriseRewards);
        compiledCashSurprise = returnArray;

    }

    //神奇服飾箱相關內容
    public static RandomRewards getInstance() {
        return instance;
    }

    //神奇服飾箱相關內容
    public final int getCSSReward() {
        return compiledCashSurprise.get(Randomizer.nextInt(compiledCashSurprise.size()));
    }

    private static void processRewards(final List<Integer> returnArray, final int[] list) {
        int lastitem = 0;
        for (int i = 0; i < list.length; i++) {
            if (i % 2 == 0) { // Even
                lastitem = list[i];
            } else { // Odd
                for (int j = 0; j < list[i]; j++) {
                    returnArray.add(lastitem);
                }
            }
        }
        Collections.shuffle(returnArray);
    }

    private static void processRewardsSimple(final List<Integer> returnArray, final int[] list) {
        for (int i = 0; i < list.length; i++) {
            returnArray.add(list[i]);
        }
        Collections.shuffle(returnArray);
    }

    public static int getGoldBoxReward() {
        return compiledGold.get(Randomizer.nextInt(compiledGold.size()));
    }

    public static int getSilverBoxReward() {
        return compiledSilver.get(Randomizer.nextInt(compiledSilver.size()));
    }

    public static int getFishingReward() {
        return compiledFishing.get(Randomizer.nextInt(compiledFishing.size()));
    }

    public static int getPeanutReward() {
        return compiledPeanut.get(Randomizer.nextInt(compiledPeanut.size()));
    }

    public static int getEventReward() {
        final int chance = Randomizer.nextInt(101);
        if (chance < 66) {
            return compiledEventC.get(Randomizer.nextInt(compiledEventC.size()));
        } else if (chance < 86) {
            return compiledEventB.get(Randomizer.nextInt(compiledEventB.size()));
        } else if (chance < 96) {
            return compiledEventA.get(Randomizer.nextInt(compiledEventA.size()));
        } else {
            return compiledEvent.get(Randomizer.nextInt(compiledEvent.size()));
        }
    }

    public static int getDropReward() {
        final int chance = Randomizer.nextInt(101);
        if (chance < 76) {
            return compiledDrops.get(Randomizer.nextInt(compiledDrops.size()));
        } else if (chance < 96) {
            return compiledDropsB.get(Randomizer.nextInt(compiledDropsB.size()));
        } else {
            return compiledDropsA.get(Randomizer.nextInt(compiledDropsA.size()));
        }
    }

    public static List<Integer> getTenPercent() {
        return tenPercent;
    }

    static void load() {
        //Empty method to initialize class.
    }
}
