function enter(pi) {
    try {
        var em = pi.getEventManager("Juliet");
        if (em != null && em.getProperty("stage1").equals("1")) {
            pi.warp(926110001, 0);
        } else {
            pi.playerMessage(5, "The portal has not opened yet.");
        }
    } catch (e) {
        pi.getPlayer().dropMessage(5, "Error: " + e);
    }
}