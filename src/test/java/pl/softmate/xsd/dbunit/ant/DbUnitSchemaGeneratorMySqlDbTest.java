package pl.softmate.xsd.dbunit.ant;

import java.sql.DriverManager;

import org.junit.Ignore;
import org.junit.Test;

public class DbUnitSchemaGeneratorMySqlDbTest {

    @Test
    @Ignore
    public void test() throws Exception {
        DbUnitSchemaGenerator generator = new DbUnitSchemaGenerator();

        DriverManager.deregisterDriver( new org.mariadb.jdbc.Driver() );
        generator.setDriverName( "com.mysql.jdbc.Driver" );
        generator.setUrl( "jdbc:mariadb://localhost:3306/dbname" );
        generator.setUser( "user" );
        generator.setPassword( "pass" );
        generator.setSchemaName( "public" );
        generator.setOutputFolder( System.getProperty( "java.io.tmpdir" ) );

        generator.execute();

    }

}
