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
	private static final BundleLogger					LOGGER			= BundleLoggerFactory.getLogger(TrackIdleDetector.class);

	// transport properties
	private long															idleLimit;
	private long															tolerance;
	private String														outGedSuffixName;
	private Boolean														keepFields											= false;
	private boolean														accumulateIdleDuration					= true;
	private boolean														reportIdleDurationWhileNotIdle	= true;
	private TrackIdleNotificationMode					notificationMode;

	// injections
	private GeoEventCreator										geoEventCreator;
	private GeoEventDefinitionManager					gedManager;

	// private data members
	private List<FieldDefinition>							trackIdleFields;
	private GeoEventDefinition								trackIdleGed;

	private final Map<String, TrackIdleState>	trackIdleStates	= new ConcurrentHashMap<String, TrackIdleState>();


	protected TrackIdleDetector(GeoEventProcessorDefinition definition) throws ComponentException
	{
		super(definition);
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
			LOGGER.error(error.getMessage(), error);
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
			errors.add(LOGGER.translate("VALIDATION_GAP_DURATION_INVALID", definition.getName()));

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
		if (geoEvent.getTrackId() == null || geoEvent.getGeometry() == null)
		{
			LOGGER.warn("NULL_ERROR");
			return null;
		}
		if (trackIdleStates == null)
		{
			LOGGER.warn("TRACK_IDLES_NULL");
			return null;
		}

		GeoEvent idleGeoEvent = null;
		try
		{
			String cacheKey = buildCacheKey(geoEvent);
			TrackIdleState idleSate = trackIdleStates.get(cacheKey);
			Date geoEventTime = geoEvent.getStartTime();

			if (idleSate != null && idleSate.getGeometry() != null)
			{
				// make sure to create/retrieve the correct GED
				GeoEventDefinition ged = createTrackIdleGED(geoEvent);

				if (!hasGeometryMoved(geoEvent.getGeometry(), idleSate.getGeometry(), tolerance))
				{
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

						// set track idle duration
						idleSate.setIdleDuration(idleDuration);

						if (notificationMode == TrackIdleNotificationMode.Continuous)
							idleGeoEvent = createTrackIdleGeoEvent(ged, idleSate, true, true, geoEvent);
						else if (!idleSate.isIdling())
							idleGeoEvent = createTrackIdleGeoEvent(ged, idleSate, true, true, geoEvent);

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
			LOGGER.error(error.getMessage(), error);
		}

		return idleGeoEvent;
	}

	private GeoEventDefinition createTrackIdleGED(GeoEvent event)
	{
		GeoEventDefinition ged = null;
		if (keepFields)
		{
			String gedName = event.getGeoEventDefinition().getName() + "_" + outGedSuffixName; //TrackIdleDetectorDefinition.DEFAULT_TRACK_IDLE_GED_NAME;
			ged = gedManager.searchGeoEventDefinition(gedName, definition.getUri().toString());
			if (ged == null)
			{
				// create the GED
				GeoEventDefinition eventGED = event.getGeoEventDefinition();

				// augment Track Idle basic fields
				try
				{
					ged = eventGED.augment(trackIdleFields);
				}
				catch (ConfigurationException e)
				{
					LOGGER.error(e.getLocalizedMessage());
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
					LOGGER.error(e.getLocalizedMessage());
				}
			}
		}
		else
		{
			ged = this.trackIdleGed;
		}
		
		return ged;
	}

	private boolean hasGeometryMoved(MapGeometry geom1, MapGeometry geom2, double tolerance)
	{
		if (geom1 != null && geom1.getGeometry() != null && geom1.getGeometry().getType() == Type.Point && geom2 != null && geom2.getGeometry() != null && geom2.getGeometry().getType() == Type.Point)
		{
			Point corePt1 = (Point) geom1.getGeometry();
			Point corePt2 = (Point) geom2.getGeometry();
			double meters = 0.0;
			try
			{
				meters = GeometryEngine.geodesicDistanceOnWGS84(corePt1, corePt2);
			}
			catch (Throwable error)
			{
				LOGGER.error(error.getMessage());
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
				// idleEvent.setField("trackId", idleState.getTrackId());
				// idleEvent.setField("GEOMETRY", idleState.getGeometry());
				for (FieldDefinition fd : geoEvent.getGeoEventDefinition().getFieldDefinitions())
				{
					if (fd.getTags().contains("TRACK_ID") || fd.getTags().contains("GEOMETRY"))
					{
						idleGeoEvent.setField(fd.getName(), geoEvent.getField(fd.getName()));
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
			LOGGER.error("GEOEVENT_CREATION_ERROR", error.getMessage());
			LOGGER.info(error.getMessage(), error);
		}

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
