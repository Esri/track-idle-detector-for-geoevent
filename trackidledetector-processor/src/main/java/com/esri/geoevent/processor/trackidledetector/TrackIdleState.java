/*
  Copyright 1995-2016 Esri

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

  For additional information, contact:
  Environmental Systems Research Institute, Inc.
  Attn: Contracts Dept
  380 New York Street
  Redlands, California, USA 92373

  email: contracts@esri.com
 */

package com.esri.geoevent.processor.trackidledetector;

import java.util.Date;

import com.esri.core.geometry.MapGeometry;

public class TrackIdleState
{
  private String      trackId;
  private Date        startTime;
  private Date        previousTime;
  private MapGeometry geometry;
  private boolean     isIdling;
  private double      idleDuration;

  public TrackIdleState(String trackId, Date startTime, Date previousTime, MapGeometry geometry)
  {
    this.trackId = trackId;
    this.startTime = startTime;
    this.previousTime = startTime;
    this.geometry = geometry;
    this.idleDuration = 0;
    this.setIdling(false);
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

  public Date getPreviousTime()
  {
    return previousTime;
  }

  public void setPreviousTime(Date previousTime)
  {
    this.previousTime = previousTime;
  }

  public MapGeometry getGeometry()
  {
    return geometry;
  }

  public void setGeometry(MapGeometry geometry)
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
