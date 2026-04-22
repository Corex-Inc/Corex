package dev.corexinc.corex.environment.utils.nms;

public class NMSHandler {
    private static final NMSRegistry registry = new NMSRegistry();

    public static NMSRegistry get() {
        return registry;
    }
}