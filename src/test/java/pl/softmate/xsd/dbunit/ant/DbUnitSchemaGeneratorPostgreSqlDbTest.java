package pl.softmate.xsd.dbunit.ant;

import org.hibernate.dialect.PostgreSQL9Dialect;
import org.junit.Test;

public class DbUnitSchemaGeneratorPostgreSqlDbTest extends BaseHibernatePoweredTest {

    @Test
    public void test() throws Exception {
        initDbUnitSchemaGenerator( "org.postgresql.Driver", "jdbc:postgresql://localhost:5432/", PostgreSQL9Dialect.class );
        generator.execute();
        checkBaseAssertions();
    }

}
