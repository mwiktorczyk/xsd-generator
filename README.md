# xsd-generator
Xsd-generator for use with DbUnit

To be used from within Ant or Gradle

```xml
...
  <taskdef name="dbUnitSchemaGenerator" classname="pl.softmate.xsd.dbunit.ant.DbUnitSchemaGenerator" classpathref="common.classpath" />
  <target name="xsd.recreate" description="Recreate database XSD" >
      <dbUnitSchemaGenerator
        driverName="${database.driver}"
        url="${database.url}"
        user="${database.user}"
        password="${database.password}"
        schemaName="public"
        outputFolder="${dbunit}/data" />
  </target>
...
```
