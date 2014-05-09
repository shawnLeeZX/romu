package com.venn.app;

import java.util.ArrayList;

import com.google.android.gms.maps.model.LatLng;

/**
 * class Route
 * Route contains all the information needed to travel from origin to
 * destination. More specifically, route consists of several {@link Segment}s,
 * each contains information to travel from the origin to the destination of the
 * segment. Also, it contains info to draw the route on the map.
 *
 * @author Shawn
 */
public class Route
{
    private String name;

    private String copyright;
    private String warning;
    private int length;

    private final ArrayList<Segment> segments;

    // The polyline to draw on the Google Map is encoded in string. It needs
    // decoding to get the list of points.
    private String polyline;
    // Array to contain the decoded polyline.
    private final ArrayList<LatLng> points;

    public Route()
    {
        segments = new ArrayList<Segment>();
        points = new ArrayList<LatLng>();
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
}
