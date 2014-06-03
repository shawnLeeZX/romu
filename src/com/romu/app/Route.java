package com.romu.app;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * class Route
 * Route contains all the information needed to travel from origin to
 * destination. More specifically, route consists of several {@link Segment}s,
 * each contains information to travel from the origin to the destination of the
 * segment. Also, it contains info to draw the route on the map.
 *
 * @author Shawn
 */
public class Route implements Parcelable
{
    private String name;
    private String copyright;
    private String warning;
    private int length;

    private String startAddr;
    private String endAddr;
    private LatLng startLocation;
    private LatLng endLocation;

    private LatLngBounds bounds;

    private final ArrayList<Segment> segments;
    private int currentSegment;
    private int segmentNum;

    // The polyline to draw on the Google Map is encoded in string. It needs
    // decoding to get the list of points.
    private String polyline;
    // Array to contain the decoded polyline.
    private final ArrayList<LatLng> points;

    public Route()
    {
        segments = new ArrayList<Segment>();
        points = new ArrayList<LatLng>();
        currentSegment = 0;
        segmentNum = segments.size();
    }

    public Route(Parcel in)
    {
        name = in.readString();
        copyright = in.readString();
        warning = in.readString();

        length = in.readInt();

        startAddr = in.readString();
        endAddr = in.readString();

        startLocation = new LatLng(
            in.readDouble(),
            in.readDouble()
        );

        endLocation = new LatLng(
            in.readDouble(),
            in.readDouble()
        );

        bounds = new LatLngBounds(
            new LatLng(
                in.readDouble(),
                in.readDouble()
            ),
            new LatLng(
                in.readDouble(),
                in.readDouble()
            )
        );

        segments = new ArrayList<Segment>();
        in.readList(segments, null);
        currentSegment = in.readInt();
        segmentNum = in.readInt();

        polyline = in.readString();

        points = Utilities.decodePolyLine(polyline);
    }

    public LatLng getCurrentDestination()
    {
        return points.get(currentSegment + 1);
    }

    public boolean incrementCurrentSegment()
    {
        if(segmentNum >= segmentNum - 1)
        {
            return false;
        }
        else
        {
            currentSegment++;
            return true;
        }
    }

    public void addPoint(final LatLng p)
    {
        points.add(p);
    }

    public void addPoints(final ArrayList<LatLng> points) 
    {
        this.points.addAll(points);
    }

    public ArrayList<LatLng> getPoints()
    {
        return points;
    }

    public void addSegment(final Segment s)
    {
        segments.add(s);
    }

    public ArrayList<Segment> getSegments()
    {
        return segments;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setCopyright(String copyright)
    {
        this.copyright = copyright;
    }

    public String getWarning()
    {
        return warning;
    }

    public void setWarning(String warning)
    {
        this.warning = warning;
    }

    public int getLength()
    {
        return length;
    }

    public void setLength(int length)
    {
        this.length = length;
    } 
    public String getPolyline()
    {
        return polyline;
    }

    public void setPolyline(String polyline)
    {
        this.polyline = polyline;
    }

    public String getCopyright()
    {
        return copyright;
    }

    public LatLngBounds getBounds()
    {
        return bounds;
    }

    public void setBounds(LatLngBounds bounds)
    {
        this.bounds = bounds;
    }

    public String getStartAddr()
    {
        return startAddr;
    }

    public void setStartAddr(String startAddr)
    {
        this.startAddr = startAddr;
    }

    public String getDestAddr()
    {
        return endAddr;
    }

    public void setDestAddr(String destAddr)
    {
        this.endAddr = destAddr;
    }

    public LatLng getStartLocation()
    {
        return startLocation;
    }

    public void setStartLocation(LatLng startLocation)
    {
        this.startLocation = startLocation;
    }

    public LatLng getEndLocation()
    {
        return endLocation;
    }

    public void setEndLocation(LatLng destLocation)
    {
        this.endLocation = destLocation;
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
        dest.writeString(name);
        dest.writeString(copyright);
        dest.writeString(warning);

        dest.writeInt(length);

        dest.writeString(startAddr);
        dest.writeString(endAddr);

        dest.writeDouble(startLocation.latitude);
        dest.writeDouble(startLocation.longitude);

        dest.writeDouble(endLocation.latitude);
        dest.writeDouble(endLocation.longitude);

        dest.writeDouble(bounds.southwest.latitude);
        dest.writeDouble(bounds.southwest.longitude);

        dest.writeDouble(bounds.northeast.latitude);
        dest.writeDouble(bounds.northeast.longitude);

        dest.writeTypedList(segments);
        dest.writeInt(currentSegment);
        dest.writeInt(segmentNum);

        dest.writeString(polyline);

    }

    /**
     * It will be required during un-marshaling data stored in a Parcel.
     */
    public class RouteCreator implements Parcelable.Creator<Route>
    {
        @Override
        public Route createFromParcel(Parcel source)
        {
            return new Route(source);
        }

        @Override
        public Route[] newArray(int size)
        {
            return new Route[size];
        }
    }
}
