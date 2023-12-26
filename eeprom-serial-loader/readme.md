# EEPROM Serial Loader
EEPROM Serial Loader is a tool for visualizing and editing binary data.
It also offers the possibility to upload it to a specific EEPROM chip, through the use of an Arduino microcontroller and a custom loader sketch and circuit.

## Opening the project
    
The project uses IntelliJ's UI designer, so it is recommended to use IntelliJ IDEA to open the project.
When using IntelliJ, make sure to enable the option to compile the forms to source code, otherwise it will not work when building with Maven.

## Building

To build and run the project, either run the project as an application in IntelliJ IDEA or use Maven to generate a JAR file:

    mvn package
