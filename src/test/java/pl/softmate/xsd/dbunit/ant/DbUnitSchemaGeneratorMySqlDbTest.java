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
        generator.setUrl( "jdbc:mysql://localhost:3306/c4c_tt" );
        generator.setUser( "c4c" );
        generator.setPassword( "ro21ot11" );
        generator.setSchemaName( "public" );
        generator.setOutputFolder( System.getProperty( "java.io.tmpdir" ) );

        generator.execute();

    }

}
