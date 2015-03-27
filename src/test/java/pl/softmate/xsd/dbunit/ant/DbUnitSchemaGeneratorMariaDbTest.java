package pl.softmate.xsd.dbunit.ant;

import java.sql.DriverManager;

import org.hibernate.dialect.MySQL5Dialect;
import org.junit.Test;

public class DbUnitSchemaGeneratorMariaDbTest extends BaseHibernatePoweredTest {

    @Test
    public void test() throws Exception {
        DriverManager.deregisterDriver( new com.mysql.jdbc.Driver() );
        initDbUnitSchemaGenerator( "org.mariadb.jdbc.Driver", "jdbc:mariadb://localhost:3306/", MySQL5Dialect.class );
        generator.execute();
        checkBaseAssertions();
    }

}
