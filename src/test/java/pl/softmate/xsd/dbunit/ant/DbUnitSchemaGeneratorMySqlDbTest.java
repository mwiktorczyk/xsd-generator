package pl.softmate.xsd.dbunit.ant;

import java.sql.DriverManager;

import org.hibernate.dialect.MySQL5Dialect;
import org.junit.Test;

public class DbUnitSchemaGeneratorMySqlDbTest extends BaseHibernatePoweredTest {

    @Test
    public void test() throws Exception {
        DriverManager.deregisterDriver( new org.mariadb.jdbc.Driver() );
        initDbUnitSchemaGenerator( "com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/", MySQL5Dialect.class );
        generator.execute();
        checkBaseAssertions();
    }

}
