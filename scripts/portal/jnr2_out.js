function enter(pi) {
    try {
        var em = pi.getEventManager("Juliet");
        if (em != null && em.getProperty("stage3").equals("3")) {
            pi.warp(926110200, 0);
        } else {
            pi.playerMessage(5, "The portal has not opened yet.");
        }
    } catch (e) {
        pi.getPlayer().dropMessage(5, "Error: " + e);
    }
}