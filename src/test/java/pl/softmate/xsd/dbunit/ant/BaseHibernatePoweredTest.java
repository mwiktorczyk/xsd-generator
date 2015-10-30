package pl.softmate.xsd.dbunit.ant;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.junit.After;

public abstract class BaseHibernatePoweredTest {

    public DbUnitSchemaGenerator generator;
    public Path schemaFilePath = Paths.get( System.getProperty( "java.io.tmpdir" ), "schema.xsd" );
    public Path updateSchemaFilePath = Paths.get( System.getProperty( "java.io.tmpdir" ), "updateSchema.xsd" );
    public DocumentBuilder builder;
    public XPath xpath;
    final String sampletableXpath = "//*[local-name()='element' and translate(@name,'ABELMPST', 'abelmpst')='sampletable']";

    public BaseHibernatePoweredTest() {
        super();
    }

    public void initDbUnitSchemaGenerator( String driverName, String url, Class<? extends Dialect> clazz ) throws Exception {
        final Properties properties = new Properties();
        properties.load( this.getClass().getResourceAsStream( "/db.props" ) );
        generator = new DbUnitSchemaGenerator();
        generator.setDriverName( driverName );
        generator.setUrl( url + properties.getProperty( "db" ) );
        generator.setUser( properties.getProperty( "user" ) );
        generator.setPassword( properties.getProperty( "pass" ) );
        generator.setSchemaName( "public" );
        generator.setOutputFolder( System.getProperty( "java.io.tmpdir" ) );

        initDbSchemaByHibernate( clazz );

        initPArserAndXPath();
    }

    public void initDbSchemaByHibernate( Class<? extends Dialect> clazz ) throws Exception {
        //@formatter:off
        Configuration cfg = new Configuration()
            .addAnnotatedClass( SampleTable.class )
            .setProperty( "hibernate.hbm2ddl.auto", "create" )
            .setProperty( "hibernate.connection.pool_size", "1" )
            .setProperty( "hibernate.current_session_context_class", "thread" )
            .setProperty( "hibernate.cache.provider_class", "org.hibernate.cache.internal.NoCacheProvider" )
            .setProperty( "hibernate.show_sql", "true" )
            .setProperty( "hibernate.connection.driver_class", generator.getDriverName() )
            .setProperty( "hibernate.connection.url", generator.getUrl())
            .setProperty( "hibernate.connection.username", generator.getUser() )
            .setProperty( "hibernate.connection.password", generator.getPassword() )
            .setProperty( "hibernate.dialect", clazz.getName() );
        //@formatter:on

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings( cfg.getProperties() ).build();
        SessionFactory sf = cfg.buildSessionFactory( serviceRegistry );
        sf.close();
    }

    public void initPArserAndXPath() throws Exception {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware( true );
        builder = domFactory.newDocumentBuilder();
        xpath = XPathFactory.newInstance().newXPath();
    }

    public String getAttr( String name, String type, String use ) {
        return "[local-name()='attribute' and @name='" + name + "' and @type='" + type + "' and @use='" + use + "']";
    }

    public void assertXPath( Document doc, String _xpath ) throws Exception {
        XPathExpression c1 = xpath.compile( _xpath );
        Object result = c1.evaluate( doc, XPathConstants.NODESET );
        NodeList nodes = (NodeList) result;
        Assert.assertEquals( "XPath :: " + _xpath, 1, nodes.getLength() );
    }

    public void checkBaseAssertions() throws SAXException, IOException, Exception {
        assertTrue( schemaFilePath.toFile().exists() );
        Document schemaDoc = builder.parse( schemaFilePath.toFile() );
        assertXPath( schemaDoc, sampletableXpath + "//*" + getAttr( "c1", "xs:long", "required" ) );
        assertXPath( schemaDoc, sampletableXpath + "//*" + getAttr( "c2", "xs:int", "required" ) );
        assertXPath( schemaDoc, sampletableXpath + "//*" + getAttr( "c3", "varchar_100", "optional" ) );
        assertXPath( schemaDoc, sampletableXpath + "//*" + getAttr( "c4", "decimal_5_2", "optional" ) );

        assertTrue( updateSchemaFilePath.toFile().exists() );
        Document updateSchemaDoc = builder.parse( updateSchemaFilePath.toFile() );
        assertXPath( updateSchemaDoc, sampletableXpath + "//*" + getAttr( "c1", "xs:long", "required" ) );
        assertXPath( updateSchemaDoc, sampletableXpath + "//*" + getAttr( "c2", "xs:int", "optional" ) );
        assertXPath( updateSchemaDoc, sampletableXpath + "//*" + getAttr( "c3", "varchar_100", "optional" ) );
        assertXPath( updateSchemaDoc, sampletableXpath + "//*" + getAttr( "c4", "decimal_5_2", "optional" ) );
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(schemaFilePath);
        Files.deleteIfExists(updateSchemaFilePath);
    }

}
