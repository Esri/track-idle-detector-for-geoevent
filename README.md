# track-idle-detector-for-geoevent

ArcGIS GeoEvent Server sample Track Idle Detector Processor.

![App](track-idle-detector-for-geoevent.png?raw=true)

## Features
* Detects whether an event had moved
* Caches the last known location and time-stamp for a new or moved event.
* Allows the user to define a distance tolerance and an idle time threshold.
* Allows the user to define whether an idle report will be generated continuously or only on change.
* Allows the user to specify whether to output a GeoEvent with all the original incoming GeoEvent's fields, or only with the idle state fields.
* Allows the user to define the output GeoEvent Definition suffix name.
* Allows the user to specify whether to accumulate the idle duration value, or to report idle duration since the last idle value reported for the Track.
* Allows the user to specify whether to report the idle duration value when not idle, or to report zero idle duration.

## Requirements

* ArcGIS GeoEvent Processor for Server version 10.6 or later.
* ArcGIS GeoEvent Processor SDK version 10.6.
* Java JDK 1.8 or greater.
* Maven 3.6.3 or greater.

## Instructions

Building the source code:

1. Make sure Maven and ArcGIS GeoEvent Processor SDK are installed on your machine.  <br>
 _c:\temp>_ javac -version <br>
 _c:\temp>_ mvn -version  <br>
2. Clone the repository to your temp drive  <br>
 _c:\temp>_ git clone <repository URL> CD into the directory  <br>
3. Build with maven  <br>
 _c:\temp>_ mvn clean install -Dcontact.address=[YourContactEmailAddress]'

Installing the built jar files:

1. Use the .jar file built above or download a [zip of jar and documentation](https://www.arcgis.com/home/item.html?id=cf02f3b8564042db8de60f582e1ad2a3).
2. Copy the jar files into the [ArcGIS-GeoEvent-Processor-Install-Directory]/deploy folder.


## Resources

* [ArcGIS GeoEvent Server SDK](https://enterprise.arcgis.com/en/geoevent/latest/reference/getting-started-with-the-geoevent-server-sdk.htm)
* [ArcGIS GeoEvent Server](https://enterprise.arcgis.com/en/geoevent/)
* [Esri GeoEvent Community](https://enterprise.arcgis.com/en/geoevent/latest/reference/getting-started-with-the-geoevent-server-sdk.htm)


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
