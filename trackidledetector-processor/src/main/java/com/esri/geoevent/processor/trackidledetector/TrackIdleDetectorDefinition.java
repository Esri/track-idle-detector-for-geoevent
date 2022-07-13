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
import java.util.List;

import com.esri.ges.core.geoevent.DefaultFieldDefinition;
import com.esri.ges.core.geoevent.DefaultGeoEventDefinition;
import com.esri.ges.core.geoevent.FieldDefinition;
import com.esri.ges.core.geoevent.FieldType;
import com.esri.ges.core.geoevent.GeoEventDefinition;
import com.esri.ges.core.property.LabeledValue;
import com.esri.ges.core.property.PropertyDefinition;
import com.esri.ges.core.property.PropertyType;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.processor.GeoEventProcessorDefinitionBase;

public class TrackIdleDetectorDefinition extends GeoEventProcessorDefinitionBase
{
  private static final BundleLogger LOGGER                      = BundleLoggerFactory.getLogger(TrackIdleDetectorDefinition.class);

  public static final String        DEFAULT_TRACK_IDLE_GED_NAME = "TrackIdle";

  public TrackIdleDetectorDefinition()
  {
    try
    {
      List<LabeledValue> allowableValues = new ArrayList<>();
      allowableValues.add(new LabeledValue("OnChange", "OnChange"));
      allowableValues.add(new LabeledValue("Continuous", "Continuous"));

      propertyDefinitions.put("notificationMode", new PropertyDefinition("notificationMode", PropertyType.String, "OnChange", "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_NOTIFICATION_MODE_LBL}", "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_NOTIFICATION_MODE_DESC}", true, false, allowableValues));
      propertyDefinitions.put("idleLimit", new PropertyDefinition("idleLimit", PropertyType.Long, 300, "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_IDLE_LIMIT_LBL}", "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_IDLE_LIMIT_DESC}", true, false));
      propertyDefinitions.put("tolerance", new PropertyDefinition("tolerance", PropertyType.Long, 120, "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_TOLERANCE_LBL}", "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_TOLERANCE_DESC}", true, false));
      propertyDefinitions.put("keepFields", new PropertyDefinition("keepFields", PropertyType.Boolean, false, "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_KEEP_FIELDS_LBL}", "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_KEEP_FIELDS_DESC}", true, false));
      propertyDefinitions.put("outGedSuffixName", new PropertyDefinition("outGedSuffixName", PropertyType.String, "TrackIdle", "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_OUT_GED_SUFFIX_NAME_LBL}", "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_OUT_GED_SUFFIX_NAME_DESC}", "keepFields=true", true, false));
      propertyDefinitions.put("accumulateIdleDuration", new PropertyDefinition("accumulateIdleDuration", PropertyType.Boolean, true, "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_ACCUMULATE_IDLE_DURATION_LBL}", "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_ACCUMULATE_IDLE_DURATION_DESC}", true, false));
      propertyDefinitions.put("reportIdleDurationWhileNotIdle", new PropertyDefinition("reportIdleDurationWhileNotIdle", PropertyType.Boolean, true, "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_REPORT_IDLE_DURATION_WHILE_NOT_IDLE_LBL}", "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_REPORT_IDLE_DURATION_WHILE_NOT_IDLE_DESC}", true, false));

      // create the default Track Idle GED
      GeoEventDefinition ged = new DefaultGeoEventDefinition();
      ged.setName(DEFAULT_TRACK_IDLE_GED_NAME);
      List<FieldDefinition> fds = new ArrayList<FieldDefinition>();
      fds.add(new DefaultFieldDefinition("trackId", FieldType.String, "TRACK_ID"));
      fds.add(new DefaultFieldDefinition("idle", FieldType.Boolean));
      fds.add(new DefaultFieldDefinition("idleDuration", FieldType.Double));
      fds.add(new DefaultFieldDefinition("idleStart", FieldType.Date, "TIME_START"));
      fds.add(new DefaultFieldDefinition("geometry", FieldType.Geometry, "GEOMETRY"));
      ged.setFieldDefinitions(fds);
      geoEventDefinitions.put(ged.getName(), ged);
    }
    catch (Exception error)
    {
      LOGGER.error("INIT_ERROR", error.getMessage());
      LOGGER.info(error.getMessage(), error);
    }
  }

  @Override
  public String getName()
  {
    return "TrackIdleDetector";
  }

  @Override
  public String getDomain()
  {
    return "com.esri.geoevent.processor";
  }

  @Override
  public String getVersion()
  {
    return "10.6.0";
  }

  @Override
  public String getLabel()
  {
    return "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_LABEL}";
  }

  @Override
  public String getDescription()
  {
    return "${com.esri.geoevent.processor.trackidledetector-processor.PROCESSOR_DESC}";
  }
}
