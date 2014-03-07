package com.esri.geoevent.processor.trackidledetector;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.messaging.Messaging;
import com.esri.ges.processor.GeoEventProcessor;
import com.esri.ges.processor.GeoEventProcessorServiceBase;

public class TrackIdleDetectorService extends GeoEventProcessorServiceBase
{
  private Messaging messaging;

  public TrackIdleDetectorService()
  {
    definition = new TrackIdleDetectorDefinition();
  }

  @Override
  public GeoEventProcessor create() throws ComponentException
  {
    TrackIdleDetector detector = new TrackIdleDetector(definition);
    detector.setMessaging(messaging);
    return detector;
  }

  public void setMessaging(Messaging messaging)
  {
    this.messaging = messaging;
  }
}