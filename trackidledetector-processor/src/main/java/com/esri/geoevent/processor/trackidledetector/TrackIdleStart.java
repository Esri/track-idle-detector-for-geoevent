package com.esri.geoevent.processor.trackidledetector;

import java.util.Date;

import com.esri.ges.spatial.Geometry;

public class TrackIdleStart
{
  private String trackId;
  private Date startTime;
  private Geometry geometry;
  private boolean isIdling;
  private double idleDuration;
  
  public TrackIdleStart(String trackId, Date startTime, Geometry geometry)
  {
    this.trackId = trackId;
    this.startTime = startTime;
    this.geometry = geometry;
    this.setIdling(false);
    this.idleDuration = 0;
  }

  public String getTrackId()
  {
    return trackId;
  }

  public void setTrackId(String trackId)
  {
    this.trackId = trackId;
  }

  public Date getStartTime()
  {
    return startTime;
  }

  public void setStartTime(Date startTime)
  {
    this.startTime = startTime;
  }

  public Geometry getGeometry()
  {
    return geometry;
  }

  public void setGeometry(Geometry geometry)
  {
    this.geometry = geometry;
  }

  public boolean isIdling()
  {
    return isIdling;
  }

  public void setIdling(boolean isIdling)
  {
    this.isIdling = isIdling;
  }

  public double getIdleDuration()
  {
    return idleDuration;
  }

  public void setIdleDuration(double idleDuration)
  {
    this.idleDuration = idleDuration;
  }
  
  
}
