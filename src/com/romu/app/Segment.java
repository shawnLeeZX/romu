package com.romu.app;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

/**
 * class Segment
 * Segment represents one segment of the {@link Route}, which contains all the
 * information needed to travel from current location to the destination of this
 * segment.
 *
 * @author Shawn
 */
public class Segment implements Parcelable
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

    public Segment(Parcel in)
    {
        start = new LatLng(
                in.readDouble(),
                in.readDouble()
                );
        end = new LatLng(
                in.readDouble(),
                in.readDouble()
                );
        instruction = in.readString();
        length = in.readInt();
    }

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

    // For parcelable.
    // ====================================================================
    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeDouble(start.latitude);
        dest.writeDouble(start.longitude);
        dest.writeDouble(end.latitude);
        dest.writeDouble(end.longitude);

        dest.writeString(instruction);
        dest.writeInt(length);
    }

    /**
     * It will be required during un-marshaling data stored in a Parcel.
     */
    public class SegmentCreator implements Parcelable.Creator<Segment>
    {
        @Override
        public Segment createFromParcel(Parcel source)
        {
            return new Segment(source);
        }

        @Override
        public Segment[] newArray(int size)
        {
            return new Segment[size];
        }
    }
}
