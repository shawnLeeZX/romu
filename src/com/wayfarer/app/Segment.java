package com.wayfarer.app;

import com.google.android.gms.maps.model.LatLng;

/**
 * class Segment
 * Segment represents one segment of the {@link Route}, which contains all the
 * information needed to travel from current location to the destination of this
 * segment.
 *
 * @author Shawn
 */
public class Segment
{
    // Geographical points in this segment.
    private LatLng start;
    private LatLng end;
    // Turn instruction to reach next segment.
    private String instruction;
    // Length of segment.
    private int length;
    /**
    * Create an empty segment.
    */
    public Segment() {}

    /**
    * Set the turn instruction.
    * 
    * @param turn. Turn instruction string.
    */
    public void setInstruction(final String turn)
    {
        this.instruction = turn;
    }

    /**
    * Get the turn instruction to reach next segment.
    * 
    * @return a String of the turn instruction.
    */
    public String getInstruction()
    {
        return instruction;
    }

    public LatLng getStart()
    {
        return start;
    }

    public void setStart(LatLng start)
    {
        this.start = start;
    }

    public LatLng getEnd()
    {
        return end;
    }

    public void setEnd(LatLng end)
    {
        this.end = end;
    }

    public int getLength()
    {
        return length;
    }

    public void setLength(int length)
    {
        this.length = length;
    }

}
