redmine-miss-api
================

A webapp which can be used to serve some missing redmine REST API resources. Currently these are:
* custom fields
* enumerations

The information is deliviered with the MIME type 'application/xml'.

Create a customized version and deploy
--------------------------------------
To create a customized webapp, do the following:
* copy the configuration template 'redmine-databases.groovy.template' to 'redmine-databases.groovy'
* modify this file according to your needs
* run the command 'gradlew war'
* copy the JAR of your jdbc driver (e.g. mysql-connector-java-5.1.21.jar) to your containers lib folder (consult your containers documentation for where JDBC drivers should be placed)
    * Alternatively you can modify the build-dependencies.gradle file and add your jdbc driver there
* deploy the generated war file from build/libs to your favorite servlet container, you probably want to rename the war file first (maybe just remove the version number)

Call your deployed webapp
-------------------------
Once deployed the webapp can be called to deliver the information. Which information that is, is determined by the extra path information.
Right now this is either 
* /custom_fields 
or 
*/enumerations
So if your webapp is deployed to a context 'missapi' and the webapp url is http://myserver.com/missapi,
you would receive the definition of custom_fields by calling 'http://myserver.com/missapi/custom_fields?location=<LOCATION>&key=<REDMINE_REST_API_KEY>&type=<TYPE>'.
The parameters location, key and type are mandatory:
* location
    * one of the configured locations, see your redmine-databases.groovy file (or the web.xml) for possible values
* key
    * the api key, this value can be found in your redmine instance. Go to 'My Account' in redmine, and find your personal REST api access key on the right side.
* type
    * one of the possible redmine customfield types (look in your redmine database in the table 'custom_fields' for possible values)

Additionally the following optional parameters are supported
* reload
    * Beware that the value is irrelevant, if this parameter is present it always signals true. So reload=false does not mean what it should, but rather reload=true.
    * Tells the servlet that it should reread the cached values. Useful if you modified the customfields or enumerations in your redmine instance.


To test your webapp, call your server and provide the necessary parameters e.g. for custom_fields: 
* http://localhost:8080/redmine-miss-api/custom_fields?key=1234567890123456789012345678901234567890&location=production&type=TimeEntryCustomField

and for enumerations:
* http://localhost:8080/redmine-miss-api/enumerations?key=1234567890123456789012345678901234567890&location=production&type=TimeEntryActivity



Technical information
=====================

This project is written in groovy. It uses gradle as a build system, and has the gradle wrapper task active - therefore no installation of gradle
is necessary to run the build.
