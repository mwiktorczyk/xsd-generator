package pl.softmate.xsd.dbunit.ant;

import java.sql.DriverManager;

import org.junit.Ignore;
import org.junit.Test;

public class DbUnitSchemaGeneratorMariaDbTest {

    @Test
    @Ignore
    public void test() throws Exception {
        DbUnitSchemaGenerator generator = new DbUnitSchemaGenerator();

        DriverManager.deregisterDriver( new com.mysql.jdbc.Driver() );
        generator.setDriverName( "org.mariadb.jdbc.Driver" );
        generator.setUrl( "jdbc:mariadb://localhost:3306/dbname" );
        generator.setUser( "user" );
        generator.setPassword( "pass" );
        generator.setSchemaName( "public" );
        generator.setOutputFolder( System.getProperty( "java.io.tmpdir" ) );

        generator.execute();

    }

}
