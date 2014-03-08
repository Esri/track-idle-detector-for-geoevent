/*
  Copyright 1995-2014 Esri

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.esri.core.geometry.GeometryEngine;
import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.geoevent.FieldException;
import com.esri.ges.core.geoevent.GeoEvent;
import com.esri.ges.core.geoevent.GeoEventDefinition;
import com.esri.ges.core.geoevent.GeoEventPropertyName;
import com.esri.ges.core.validation.ValidationException;
import com.esri.ges.messaging.GeoEventCreator;
import com.esri.ges.messaging.Messaging;
import com.esri.ges.messaging.MessagingException;
import com.esri.ges.processor.GeoEventProcessorBase;
import com.esri.ges.processor.GeoEventProcessorDefinition;
import com.esri.ges.spatial.Geometry;
import com.esri.ges.spatial.Point;
import com.esri.ges.util.Converter;
import com.esri.ges.util.Validator;

public class TrackIdleDetector extends GeoEventProcessorBase
{
  private static final Log                  log        = LogFactory.getLog(TrackIdleDetector.class);
  private TrackIdleNotificationMode         notificationMode;
  private long                              idleLimit;
  private GeoEventCreator                   geoEventCreator;
  private long                              tolerance;

  private final Map<String, TrackIdleStart> trackIdles = new ConcurrentHashMap<String, TrackIdleStart>();

  protected TrackIdleDetector(GeoEventProcessorDefinition definition) throws ComponentException
  {
    super(definition);
  }

  public void afterPropertiesSet()
  {
    notificationMode = Validator.validateEnum(TrackIdleNotificationMode.class, getProperty("notificationMode").getValueAsString(), TrackIdleNotificationMode.OnChange);
    idleLimit = Converter.convertToInteger(getProperty("idleLimit").getValueAsString(), 300);
    tolerance = Converter.convertToLong(getProperty("tolerance").getValueAsString(), 50l);
  }
  
  @Override
  public GeoEvent process(GeoEvent geoEvent) throws Exception
  {
    return processGeoEvent(geoEvent);
  }

  @Override
  public void validate() throws ValidationException
  {
    super.validate();
    List<String> errors = new ArrayList<String>();
    if (idleLimit <= 0)
      errors.add("'" + definition.getName() + "' property 'gapDuration' is invalid.");

    if (errors.size() > 0)
    {
      StringBuffer sb = new StringBuffer();
      for (String message : errors)
        sb.append(message).append("\n");
      throw new ValidationException(this.getClass().getName() + " validation failed: " + sb.toString());
    }
  }

  private GeoEvent processGeoEvent(GeoEvent geoEvent)
  {
    GeoEvent geoevent = null;

    if (geoEvent.getTrackId() == null || geoEvent.getGeometry() == null)
    {
      log.warn("Event not processed because the Track ID or the geometry is null");
      return null;

    }
    if (trackIdles == null)
    {
      log.warn("trackIdles is null");
      return null;
    }
    try
    {
      String cacheKey = buildCacheKey(geoEvent);
      TrackIdleStart idleStart = trackIdles.get(cacheKey);
      if (idleStart != null && idleStart.getGeometry() != null)
      {
        if (!hasGeometryMoved(geoEvent.getGeometry(), idleStart.getGeometry(), tolerance))
        {
          double idleDuration = (geoEvent.getStartTime().getTime() - idleStart.getStartTime().getTime()) / 1000.0;
          idleDuration = idleDuration >= 0 ? idleDuration : -idleDuration;
          idleDuration = Math.round(idleDuration * 10.0) / 10.0;
          if (idleDuration >= idleLimit)
          {
            idleStart.setIdleDuration(idleDuration);
            if (notificationMode == TrackIdleNotificationMode.Continuous)
              geoevent = createTrackIdleGeoEvent(idleStart, true);
            else if (!idleStart.isIdling())
              geoevent = createTrackIdleGeoEvent(idleStart, true);
            idleStart.setIdling(true);
          }
        }
        else
        {
          if (idleStart.isIdling())
          {
            geoevent = createTrackIdleGeoEvent(idleStart, false);
          }
          idleStart.setGeometry(geoEvent.getGeometry());
          idleStart.setStartTime(geoEvent.getStartTime());
          idleStart.setIdling(false);
        }
      }
      else
      {
        trackIdles.put(cacheKey, new TrackIdleStart(geoEvent.getTrackId(), geoEvent.getStartTime(), geoEvent.getGeometry()));
      }
    }
    catch (Exception e)
    {
      log.error(e);
    }
    return geoevent;
  }

  private boolean hasGeometryMoved(Geometry geom1, Geometry geom2, double tolerance)
  {
    if (geom1 instanceof Point && geom2 instanceof Point)
    {
      Point pt1 = (Point) geom1;
      Point pt2 = (Point) geom2;
      com.esri.core.geometry.Point corePt1 = new com.esri.core.geometry.Point(pt1.getX(), pt1.getY());
      com.esri.core.geometry.Point corePt2 = new com.esri.core.geometry.Point(pt2.getX(), pt2.getY());
      double meters = 0.0;
      try
      {
        meters = GeometryEngine.geodesicDistanceOnWGS84(corePt1, corePt2);
      }
      catch (Exception e)
      {
        log.error(e);
      }
      catch (Error er)
      {
        log.error(er.getMessage());
      }
      catch (Throwable t)
      {
        log.error(t.getMessage());
      }

      double feet = meter2feet(meters);
      if (feet >= tolerance)
        return true;
      else
        return false;
    }
    else
    {
      throw new RuntimeException("Only points are supported in this version.");
    }
  }

  private double meter2feet(double meter)
  {
    return meter * 3.28084;
  }

  private GeoEvent createTrackIdleGeoEvent(TrackIdleStart idleStart, boolean isIdle) throws MessagingException
  {
    GeoEvent idleEvent = null;
    if (geoEventCreator != null)
    {
      try
      {
        idleEvent = geoEventCreator.create("TrackIdle", definition.getUri().toString());
        idleEvent.setField(0, idleStart.getTrackId());
        idleEvent.setField(1, isIdle);
        idleEvent.setField(2, idleStart.getIdleDuration());
        idleEvent.setField(3, idleStart.getStartTime());
        idleEvent.setField(4, idleStart.getGeometry());
        idleEvent.setProperty(GeoEventPropertyName.TYPE, "event");
        idleEvent.setProperty(GeoEventPropertyName.OWNER_ID, getId());
        idleEvent.setProperty(GeoEventPropertyName.OWNER_URI, definition.getUri());
      }
      catch (FieldException e)
      {
        idleEvent = null;
        log.error("Failed to create Track Gap GeoEvent: " + e.getMessage());
      }
    }
    return idleEvent;
  }

  private String buildCacheKey(GeoEvent geoEvent)
  {
    if (geoEvent != null && geoEvent.getTrackId() != null)
    {
      GeoEventDefinition definition = geoEvent.getGeoEventDefinition();
      return definition.getOwner() + "/" + definition.getName() + "/" + geoEvent.getTrackId();
    }
    return null;
  }

  public void setMessaging(Messaging messaging)
  {
    geoEventCreator = messaging.createGeoEventCreator();
  }
}
