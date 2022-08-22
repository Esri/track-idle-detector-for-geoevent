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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.Point;
import com.esri.ges.core.ConfigurationException;
import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.geoevent.DefaultFieldDefinition;
import com.esri.ges.core.geoevent.FieldDefinition;
import com.esri.ges.core.geoevent.FieldType;
import com.esri.ges.core.geoevent.GeoEvent;
import com.esri.ges.core.geoevent.GeoEventDefinition;
import com.esri.ges.core.geoevent.GeoEventPropertyName;
import com.esri.ges.core.validation.ValidationException;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.manager.geoeventdefinition.GeoEventDefinitionManager;
import com.esri.ges.manager.geoeventdefinition.GeoEventDefinitionManagerException;
import com.esri.ges.messaging.GeoEventCreator;
import com.esri.ges.messaging.Messaging;
import com.esri.ges.messaging.MessagingException;
import com.esri.ges.processor.GeoEventProcessorBase;
import com.esri.ges.processor.GeoEventProcessorDefinition;
import com.esri.ges.util.Converter;
import com.esri.ges.util.Validator;

public class TrackIdleDetector extends GeoEventProcessorBase
{
  private static final BundleLogger         LOGGER                         = BundleLoggerFactory.getLogger(TrackIdleDetector.class);

  // transport properties
  private long                              idleLimit;
  private long                              tolerance;
  private String                            outGedSuffixName;
  private Boolean                           keepFields                     = false;
  private boolean                           accumulateIdleDuration         = true;
  private boolean                           reportIdleDurationWhileNotIdle = true;
  private TrackIdleNotificationMode         notificationMode;

  // injections
  private GeoEventCreator                   geoEventCreator;
  private GeoEventDefinitionManager         gedManager;

  // private data members
  private List<FieldDefinition>             trackIdleFields;
  private GeoEventDefinition                trackIdleGed;

  private final Map<String, TrackIdleState> trackIdleStates                = new ConcurrentHashMap<String, TrackIdleState>();

  protected TrackIdleDetector(GeoEventProcessorDefinition definition) throws ComponentException
  {
    super(definition);
    LOGGER.trace("PROCESSOR_DESC");
  }

  public void afterPropertiesSet()
  {
    // read properties
    notificationMode = Validator.valueOfIgnoreCase(TrackIdleNotificationMode.class, getProperty("notificationMode").getValueAsString(), TrackIdleNotificationMode.OnChange);
    idleLimit = Converter.convertToInteger(getProperty("idleLimit").getValueAsString(), 300);
    tolerance = Converter.convertToLong(getProperty("tolerance").getValueAsString(), 50l);
    keepFields = (Boolean) getProperty("keepFields").getValue();
    outGedSuffixName = getProperty("outGedSuffixName").getValueAsString();
    accumulateIdleDuration = (Boolean) getProperty("accumulateIdleDuration").getValue();
    reportIdleDurationWhileNotIdle = (Boolean) getProperty("reportIdleDurationWhileNotIdle").getValue();

    // get the "TrackIdle" GED
    trackIdleGed = gedManager.searchGeoEventDefinition(TrackIdleDetectorDefinition.DEFAULT_TRACK_IDLE_GED_NAME, definition.getUri().toString());

    // prepare to augment track idle fields to a GED
    trackIdleFields = new ArrayList<FieldDefinition>();
    try
    {
      trackIdleFields.add(new DefaultFieldDefinition("idle", FieldType.Boolean));
      trackIdleFields.add(new DefaultFieldDefinition("idleDuration", FieldType.Double));
      trackIdleFields.add(new DefaultFieldDefinition("idleStart", FieldType.Date));
    }
    catch (ConfigurationException error)
    {
      if (LOGGER.isDebugEnabled())
        LOGGER.warn("FAILED_ADD_FIELD_GED", error);
      else
        LOGGER.warn("FAILED_ADD_FIELD_GED", error.getMessage());
    }
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
      errors.add(LOGGER.translate("VALIDATION_IDLE_LIMIT_INVALID", definition.getName()));

    if (errors.size() > 0)
    {
      StringBuffer sb = new StringBuffer();
      for (String message : errors)
        sb.append(message).append("\n");
      throw new ValidationException(LOGGER.translate("VALIDATION_ERROR", this.getClass().getName(), sb.toString()));
    }
  }

  private GeoEvent processGeoEvent(GeoEvent geoEvent) throws GeoEventDefinitionManagerException
  {
    if (geoEvent.getTrackId() == null || geoEvent.getGeometry() == null || geoEvent.getField("TIME_START") == null)
    {
      String gedName = geoEvent.getGeoEventDefinition().getName();
      if (LOGGER.isDebugEnabled())
        LOGGER.warn("NULL_ERROR", new RuntimeException("Required field missing"), gedName, geoEvent);
      else
        LOGGER.warn("NULL_ERROR", gedName, "");
      return null;
    }
    if (trackIdleStates == null)
    {
      LOGGER.warn("TRACK_IDLES_NULL");
      return null;
    }
    LOGGER.trace("PROCESSING_EVENT", geoEvent);

    GeoEvent idleGeoEvent = null;
    try
    {
      String cacheKey = buildCacheKey(geoEvent);
      LOGGER.trace("PROCESSING_EVENT_CACHE_KEY", cacheKey);
      TrackIdleState idleSate = trackIdleStates.get(cacheKey);
      LOGGER.trace("FOUND_EVENT_IDLE_STATE", idleSate);
      Date geoEventTime = geoEvent.getStartTime();
      LOGGER.trace("CURRENT_EVENT_TIME", geoEventTime);

      if (idleSate != null && idleSate.getGeometry() != null)
      {

        if (!hasGeometryMoved(cacheKey, geoEvent.getGeometry(), idleSate.getGeometry(), tolerance))
        {
          LOGGER.trace("TRACK_NOT_MOVED_ENOUGH", cacheKey, tolerance, geoEvent);
          // didn't move more than tolerance (in feet)

          double idleDuration = 0;
          if (accumulateIdleDuration)
          {
            idleDuration = geoEventTime.getTime() - idleSate.getStartTime().getTime();
          }
          else
          {
            idleDuration = geoEventTime.getTime() - idleSate.getPreviousTime().getTime();
          }
          idleDuration = idleDuration / 1000.0;
          idleDuration = Math.abs(idleDuration);
          idleDuration = Math.round(idleDuration * 10.0) / 10.0;

          if (idleDuration >= idleLimit)
          {
            // track is idle more than idleLimit
            LOGGER.trace("IDLE_DURATION_EXCEEDS_LIMIT", idleDuration, idleLimit, geoEvent);

            // set track idle duration
            idleSate.setIdleDuration(idleDuration);

            if (notificationMode == TrackIdleNotificationMode.Continuous)
            {
              GeoEventDefinition ged = createTrackIdleGED(geoEvent);

              idleGeoEvent = createTrackIdleGeoEvent(ged, idleSate, true, true, geoEvent);
            }
            else if (!idleSate.isIdling())
            {
              GeoEventDefinition ged = createTrackIdleGED(geoEvent);
              idleGeoEvent = createTrackIdleGeoEvent(ged, idleSate, true, true, geoEvent);
            }
            // set track to idle
            idleSate.setIdling(true);
          }
        }
        else
        {
          // moved more than tolerance, track is not idle
          if (idleSate.isIdling())
          {
            // track is no longer idle
            GeoEventDefinition ged = createTrackIdleGED(geoEvent);
            idleGeoEvent = createTrackIdleGeoEvent(ged, idleSate, false, reportIdleDurationWhileNotIdle, geoEvent);
          }

          idleSate.setGeometry(geoEvent.getGeometry());
          idleSate.setStartTime(geoEventTime);

          // set track to not idle
          idleSate.setIdling(false);
        }
      }
      else
      {
        trackIdleStates.put(cacheKey, new TrackIdleState(geoEvent.getTrackId(), geoEventTime, geoEventTime, geoEvent.getGeometry()));
      }
    }
    catch (Exception error)
    {
      if (LOGGER.isTraceEnabled())
        LOGGER.warn("PROCESS_EVENT_FAILURE", error, geoEvent);
      else
        LOGGER.warn("PROCESS_EVENT_FAILURE", error.getMessage());
    }

    return idleGeoEvent;
  }

  private GeoEventDefinition createTrackIdleGED(GeoEvent event)
  {
    GeoEventDefinition ged = null;
    if (keepFields)
    {
      String gedName = event.getGeoEventDefinition().getName() + "_" + outGedSuffixName; // TrackIdleDetectorDefinition.DEFAULT_TRACK_IDLE_GED_NAME;
      ged = gedManager.searchGeoEventDefinition(gedName, definition.getUri().toString());
      if (ged == null)
      {
        // create the GED
        GeoEventDefinition eventGED = event.getGeoEventDefinition();

        List<String> duplicateFieldList = new ArrayList<String>();
        for (FieldDefinition eventField : eventGED.getFieldDefinitions())
        {
          String eventFieldName = eventField.getName();
          for (FieldDefinition trackIdleField : trackIdleFields)
          {
            String trackIdleFieldName = trackIdleField.getName();
            if (trackIdleFieldName.equalsIgnoreCase(eventFieldName))
            {
              duplicateFieldList.add(trackIdleFieldName);
            }
          }
        }
        if (duplicateFieldList.size() > 0)
        {
          throw new RuntimeException(LOGGER.translate("DUPLICATE_FIELD", gedName, eventGED.getName(), Arrays.toString(duplicateFieldList.toArray())));
        }

        // augment Track Idle basic fields
        try
        {
          ged = eventGED.augment(trackIdleFields);
        }
        catch (ConfigurationException e)
        {
          if (LOGGER.isDebugEnabled())
            LOGGER.warn("ADD_TRACK_IDLE_FIELDS_FAILURE", e, gedName);
          else
            LOGGER.warn("ADD_TRACK_IDLE_FIELDS_FAILURE", gedName);
        }

        // make sure to also augment the "GEOMETRY" and "TRACK_ID" fields
        FieldDefinition trackIdFD = eventGED.getFieldDefinition("TRACK_ID");
        FieldDefinition geometryFD = eventGED.getFieldDefinition("GEOMETRY");
        if (trackIdFD == null || geometryFD == null)
        {
          List<FieldDefinition> fds = ged.getFieldDefinitions();
          if (trackIdFD == null)
            fds.add(trackIdFD);
          if (geometryFD == null)
            fds.add(geometryFD);

          ged.setFieldDefinitions(fds);
        }

        ged.setName(gedName);
        ged.setOwner(definition.getUri().toString());

        try
        {
          gedManager.addGeoEventDefinition(ged);
        }
        catch (GeoEventDefinitionManagerException e)
        {
          if (LOGGER.isDebugEnabled())
            LOGGER.warn("ADD_DEFINITION_FAILURE", e, gedName, "");
          else
            LOGGER.warn("ADD_DEFINITION_FAILURE", gedName, e.getMessage());
        }
      }
    }
    else
    {
      ged = this.trackIdleGed;
    }

    return ged;
  }

  private boolean hasGeometryMoved(String cacheKey, MapGeometry geom1, MapGeometry geom2, double tolerance)
  {
    if (geom1 != null && geom1.getGeometry() != null && geom1.getGeometry().getType() == Type.Point && geom2 != null && geom2.getGeometry() != null && geom2.getGeometry().getType() == Type.Point)
    {
      Point corePt1 = (Point) geom1.getGeometry();
      Point corePt2 = (Point) geom2.getGeometry();
      double meters = 0.0;
      try
      {
        meters = GeometryEngine.geodesicDistanceOnWGS84(corePt1, corePt2);
        LOGGER.trace("TRACK_MOVED_X_METERS", cacheKey, meters);
      }
      catch (Throwable error)
      {
        if (LOGGER.isDebugEnabled())
          LOGGER.warn("DISTANCE_FAILURE", error, cacheKey);
        else
          LOGGER.warn("DISTANCE_FAILURE", cacheKey);
      }

      double feet = meter2feet(meters);
      if (feet >= tolerance)
        return true;
      else
        return false;
    }
    else
    {
      throw new RuntimeException(LOGGER.translate("INVALID_GEOMETRY_TYPE"));
    }
  }

  private double meter2feet(double meter)
  {
    return meter * 3.28084;
  }

  private GeoEvent createTrackIdleGeoEvent(GeoEventDefinition ged, TrackIdleState idleState, boolean isIdle, boolean reportIdleDuration, GeoEvent geoEvent) throws MessagingException
  {
    if (geoEventCreator == null)
      return null;

    GeoEvent idleGeoEvent = null;
    try
    {
      idleGeoEvent = geoEventCreator.create(ged.getName(), definition.getUri().toString());
      idleGeoEvent.setField("idle", isIdle);
      idleGeoEvent.setField("idleStart", idleState.getStartTime());

      if (reportIdleDuration)
        idleGeoEvent.setField("idleDuration", idleState.getIdleDuration());
      else
        idleGeoEvent.setField("idleDuration", 0);

      if (keepFields)
      {
        for (FieldDefinition fd : geoEvent.getGeoEventDefinition().getFieldDefinitions())
        {
          idleGeoEvent.setField(fd.getName(), geoEvent.getField(fd.getName()));
        }
      }
      else
      {
        if (LOGGER.isTraceEnabled())
        {
          LOGGER.trace("SETTING_IDLE_NOTIFICATION_FIELD", "TRACK_ID", idleState.getTrackId());
          LOGGER.trace("SETTING_IDLE_NOTIFICATION_FIELD", "GEOMETRY", idleState.getGeometry());
        }
        idleGeoEvent.setField("TRACK_ID", idleState.getTrackId());
        idleGeoEvent.setField("GEOMETRY", idleState.getGeometry());

        // Update from the new event if possible
        for (FieldDefinition fd : geoEvent.getGeoEventDefinition().getFieldDefinitions())
        {
          if (fd.getTags().contains("TRACK_ID"))
          {
            if (idleGeoEvent.getField(fd.getName()) != null)
            {
              if (LOGGER.isTraceEnabled())
                LOGGER.trace("UPDATING_EXISTING_FIELD_VALUE", fd.getName(), geoEvent.getField(fd.getName()));
              idleGeoEvent.setField(fd.getName(), geoEvent.getField(fd.getName()));
            }
            else
            {
              if (LOGGER.isTraceEnabled())
                LOGGER.trace("UPDATING_TRACK_ID_TAG_INSTEAD", fd.getName(), geoEvent.getField(fd.getName()));
              idleGeoEvent.setField("TRACK_ID", geoEvent.getField(fd.getName()));
            }
          }
          else if (fd.getTags().contains("GEOMETRY"))
          {
            if (idleGeoEvent.getField(fd.getName()) != null)
            {
              if (LOGGER.isTraceEnabled())
                LOGGER.trace("UPDATING_GEOMETRY_FIELD", fd.getName(), geoEvent.getField(fd.getName()));
            idleGeoEvent.setField(fd.getName(), geoEvent.getField(fd.getName()));
          }
            else
            {
              if (LOGGER.isTraceEnabled())
                LOGGER.trace("UPDATING_GEOMETRY_TAG_INSTEAD", fd.getName(), geoEvent.getField(fd.getName()));
              idleGeoEvent.setField("GEOMETRY", geoEvent.getField(fd.getName()));
            }
          }
        }
      }

      idleGeoEvent.setProperty(GeoEventPropertyName.TYPE, "event");
      idleGeoEvent.setProperty(GeoEventPropertyName.OWNER_ID, getId());
      idleGeoEvent.setProperty(GeoEventPropertyName.OWNER_URI, definition.getUri());

      // set previous time to the current GeoEvent time
      idleState.setPreviousTime(geoEvent.getStartTime());
    }
    catch (Exception error)
    {
      idleGeoEvent = null;
      if (LOGGER.isDebugEnabled())
        LOGGER.warn("GEOEVENT_CREATION_ERROR", error, geoEvent);
      else
        LOGGER.warn("GEOEVENT_CREATION_ERROR", geoEvent);
    }

    if (idleGeoEvent != null)
      LOGGER.trace("RELEASING_IDLE_EVENT", idleGeoEvent);

    return idleGeoEvent;
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

  public void setManager(GeoEventDefinitionManager gedManager)
  {
    this.gedManager = gedManager;
  }
}
