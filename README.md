# track-idle-detector-for-geoevent

ArcGIS 10.4 GeoEvent Extension for Server sample Location Idle Track Detector Processor.

![App](track-idle-detector-for-geoevent.png?raw=true)

## Features
* Detects whether an event had moved
* Caches the last known location and time-stamp for a new or moved event.
* Allows the user to define a distance tolerance and an idle time threshold.
* Allows the user to define whether an idle report will be generated continuously or only on change.
* Allows the user to specify whether to create a GeoEvent with all the original GeoEvent's the fields, or only with the idle state fields.
* Allows the user to define the output GeoEvent Definition.
* Allows the user to specify whether to accumulate the idle duration value, or to report idle duration since the last idle report for the Track.
* Allows the user to specify whether to report the idle duration value when not idle, or to report zero idle duration.

## Instructions

Building the source code:

1. Make sure Maven and ArcGIS GeoEvent Extension SDK are installed on your machine.
2. Run 'mvn install -Dcontact.address=[YourContactEmailAddress]'

Installing the built jar files:

1. Copy the *.jar files under the 'target' sub-folder(s) into the [ArcGIS-GeoEvent-Extension-Install-Directory]/deploy folder.

## Requirements

* ArcGIS GeoEvent Extension for Server (certified with version 10.3).
* ArcGIS GeoEvent Extension SDK.
* Java JDK 1.7 or greater.
* Maven.

## Resources

* [GeoEvent gallery item](http://www.arcgis.com/home/item.html?id=5d8e3446736d4df299c7c96bc275d561) on the ArcGIS GeoEvent Extension Gallery
* [ArcGIS GeoEvent Extension for Server Resources](http://links.esri.com/geoevent)
* [ArcGIS Blog](http://blogs.esri.com/esri/arcgis/)
* [twitter@esri](http://twitter.com/esri)

## Issues

Find a bug or want to request a new feature?  Please let us know by submitting an issue.

## Contributing

Esri welcomes contributions from anyone and everyone. Please see our [guidelines for contributing](https://github.com/esri/contributing).

## Licensing
Copyright 2013 Esri

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

A copy of the license is available in the repository's [license.txt](license.txt?raw=true) file.

[](ArcGIS, GeoEvent, Processor)
[](Esri Tags: ArcGIS GeoEvent Extension for Server)
[](Esri Language: Java)
