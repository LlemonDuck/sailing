package com.duckblade.osrs.sailing.features.salvaging;

public class Wreck
{
    public enum WreckType
    {
        SALVAGE,
        STUMP
    }
    private final WreckType type;
    private final int levelReq;

    public Wreck(WreckType type, int levelReq)
    {
        this.type = type;
        this.levelReq = levelReq;
    }
    public WreckType getType()
    {
        return type;
    }

    public int getLevelReq()
    {
        return levelReq;
    }
}

