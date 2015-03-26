# xsd-generator
Xsd-generator for use with DbUnit

To be used from within Ant

<project>
  <taskdef name="dbUnitSchemaGenerator" classname="pl.softmate.xsd.dbunit.ant.DbUnitSchemaGenerator" classpathref="common.classpath" />

  <target name="um.xsd.recreate" description="Recreate database XSD" depends="-set-props-if-not-set">
      <dbUnitSchemaGenerator driverName="${database.driver}" url="${database.url}" user="${database.user}" password="${database.password}" schemaName="public" outputFolder="${dbunit}/data" />
  </target>
</project>
