package com.esri.geoevent.processor.trackidledetector;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.esri.ges.core.geoevent.DefaultFieldDefinition;
import com.esri.ges.core.geoevent.DefaultGeoEventDefinition;
import com.esri.ges.core.geoevent.FieldDefinition;
import com.esri.ges.core.geoevent.FieldType;
import com.esri.ges.core.geoevent.GeoEventDefinition;
import com.esri.ges.core.property.PropertyDefinition;
import com.esri.ges.core.property.PropertyType;
import com.esri.ges.processor.GeoEventProcessorDefinitionBase;

public class TrackIdleDetectorDefinition extends GeoEventProcessorDefinitionBase
{
  final private static Log LOG = LogFactory.getLog(TrackIdleDetectorDefinition.class);

  public TrackIdleDetectorDefinition()
  {
    try
    {
      propertyDefinitions.put("notificationMode", new PropertyDefinition("notificationMode", PropertyType.String, "OnChange", "Idle Notification Mode", "Idle Notification Mode", true, false, "OnChange", "Continuous"));
      propertyDefinitions.put("idleLimit", new PropertyDefinition("idleLimit", PropertyType.Long, 300, "Maximum time (seconds) allowed for idling", "Maximum time (seconds) of non-movement beyond which alerts will be generated.", true, false));
      propertyDefinitions.put("tolerance", new PropertyDefinition("tolerance", PropertyType.Long, 120, "Tolerance (feet)", "Tolerance (feet)", true, false));
//      propertyDefinitions.put("geometryField",  new PropertyDefinition("geometryField", PropertyType.String, "GEOMETRY", "Geometry Field Name", "Geometry Field Name", false, false));

      GeoEventDefinition ged = new DefaultGeoEventDefinition();
      ged.setName("TrackIdle");
      List<FieldDefinition> fds = new ArrayList<FieldDefinition>();
      fds.add(new DefaultFieldDefinition("trackId", FieldType.String, "TRACK_ID"));
      fds.add(new DefaultFieldDefinition("idle", FieldType.Boolean));
      fds.add(new DefaultFieldDefinition("idleDuration", FieldType.Double));
      fds.add(new DefaultFieldDefinition("idleStart", FieldType.Date));
      fds.add(new DefaultFieldDefinition("geometry", FieldType.Geometry));
      ged.setFieldDefinitions(fds);
      geoEventDefinitions.put(ged.getName(), ged);
    }
    catch (Exception e)
    {
      LOG.error("Error setting up Track Idle Detector Definition.", e);
    }
  }

  @Override
  public String getName()
  {
    return "TrackIdleDetector";
  }

  @Override
  public String getLabel()
  {
    return "Track Idle Detector";
  }

  @Override
  public String getDescription()
  {
    return "Detects non-movement of a Track beyond a specified period of time.";
  }
}