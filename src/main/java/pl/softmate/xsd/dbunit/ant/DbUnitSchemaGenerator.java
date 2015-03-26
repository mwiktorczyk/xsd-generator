package pl.softmate.xsd.dbunit.ant;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.sql.Types.BIGINT;
import static java.sql.Types.BINARY;
import static java.sql.Types.BIT;
import static java.sql.Types.BLOB;
import static java.sql.Types.BOOLEAN;
import static java.sql.Types.CHAR;
import static java.sql.Types.CLOB;
import static java.sql.Types.DATE;
import static java.sql.Types.DECIMAL;
import static java.sql.Types.DOUBLE;
import static java.sql.Types.FLOAT;
import static java.sql.Types.INTEGER;
import static java.sql.Types.LONGNVARCHAR;
import static java.sql.Types.LONGVARBINARY;
import static java.sql.Types.LONGVARCHAR;
import static java.sql.Types.NCHAR;
import static java.sql.Types.NCLOB;
import static java.sql.Types.NUMERIC;
import static java.sql.Types.NVARCHAR;
import static java.sql.Types.REAL;
import static java.sql.Types.REF;
import static java.sql.Types.SMALLINT;
import static java.sql.Types.TIME;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.TIMESTAMP_WITH_TIMEZONE;
import static java.sql.Types.TIME_WITH_TIMEZONE;
import static java.sql.Types.TINYINT;
import static java.sql.Types.VARBINARY;
import static java.sql.Types.VARCHAR;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class DbUnitSchemaGenerator extends Task {

    private String schemaName;
    private String outputFolder;
    private String driverName;
    private String url;
    private String user;
    private String password;

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName( String schemaName ) {
        this.schemaName = schemaName;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder( String outputFolder ) {
        this.outputFolder = outputFolder;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName( String driverName ) {
        this.driverName = driverName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl( String url ) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser( String user ) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword( String password ) {
        this.password = password;
    }

    @Override
    public void execute() throws BuildException {

        try {
            Class.forName( driverName ).newInstance();
        }
        catch ( Exception e ) {
            throw new BuildException( e );
        }

        try (Connection c = DriverManager.getConnection( url, user, password )) {

            final String VARCHAR_SIZE_COLUMN;
            final String DRIVER_NAME = c.getMetaData().getDriverName().toLowerCase();
            if ( DRIVER_NAME.contains( "mariadb" ) ) {
                VARCHAR_SIZE_COLUMN = "COLUMN_SIZE";
            }
            else {
                VARCHAR_SIZE_COLUMN = "CHAR_OCTET_LENGTH";
            }

            Set<String> tables = new TreeSet<>();
            try (ResultSet rs = c.getMetaData().getTables( null, schemaName, null, new String[] { "TABLE", "BASE TABLE" } )) {
                while ( rs.next() ) {
                    tables.add( rs.getString( "TABLE_NAME" ) );
                }
            }
            Map<String, Set<String>> table2pk = new HashMap<>();

            for ( String table : tables ) {
                Set<String> pks = new HashSet<>();
                table2pk.put( table, pks );
                try (ResultSet rs = c.getMetaData().getPrimaryKeys( null, schemaName, table )) {
                    while ( rs.next() ) {
                        String tableName = rs.getString( "TABLE_NAME" );
                        if ( table.equalsIgnoreCase( tableName ) ) {
                            pks.add( rs.getString( "COLUMN_NAME" ) );
                        }

                    }
                }
            }

            Map<String, List<XsdAttribute>> table2collumn = new LinkedHashMap<>();
            Set<XsdType> types = new TreeSet<>();
            for ( String table : tables ) {
                if ( table.equalsIgnoreCase( "attachment" ) ) {
                    System.out.println();
                }
                List<XsdAttribute> collumns = new ArrayList<>();
                table2collumn.put( table, collumns );
                try (ResultSet rs = c.getMetaData().getColumns( null, schemaName, table, null )) {
                    while ( rs.next() ) {
                        String tableName = rs.getString( "TABLE_NAME" );
                        if ( table.equalsIgnoreCase( tableName ) ) {
                            final int dataType = rs.getInt( "DATA_TYPE" );
                            String collumnName = rs.getString( "COLUMN_NAME" );
                            int varcharSize = rs.getInt( VARCHAR_SIZE_COLUMN );
                            String isNullable = rs.getString( "IS_NULLABLE" );
                            int columnSize = rs.getInt( "COLUMN_SIZE" );
                            int decimalDigits = rs.getInt( "DECIMAL_DIGITS" );

                            XsdAttribute attr = new XsdAttribute();
                            attr.name = collumnName;
                            attr.required = ( null != isNullable && "no".equalsIgnoreCase( isNullable ) );
                            attr.type = new XsdType();
                            attr.pk = table2pk.get( tableName ).contains( collumnName );

                            mapToXsdType( dataType, varcharSize, columnSize, decimalDigits, attr );

                            collumns.add( attr );
                            types.add( attr.type );
                        }

                    }
                }
            }

            generateFile( table2collumn, types, false );
            generateFile( table2collumn, types, true );
        }
        catch ( Exception e ) {
            throw new BuildException( e );
        }

    }

    private void generateFile( Map<String, List<XsdAttribute>> table2collumn, Set<XsdType> types, boolean update ) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter( Paths.get( outputFolder, update ? "updateSchema.xsd" : "schema.xsd" ), Charset.forName( "utf8" ), CREATE, TRUNCATE_EXISTING, WRITE )) {

            //start file                    
            bw.append( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" );
            bw.newLine();
            bw.append( "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" );
            bw.newLine();

            //define derived types for decimal, char and varchar
            for ( XsdType xsdType : types ) {
                if ( xsdType.base == XsdTypeNameBase.DECIMAL ) {
                    appendDecimal( bw, xsdType );
                }
            }
            for ( XsdType xsdType : types ) {
                if ( xsdType.base == XsdTypeNameBase.CHAR ) {
                    appendChar( bw, xsdType );
                }
            }
            for ( XsdType xsdType : types ) {
                if ( xsdType.base == XsdTypeNameBase.VARCHAR ) {
                    appendVarchar( bw, xsdType );
                }
            }

            for ( XsdType xsdType : types ) {
                if ( xsdType.base == XsdTypeNameBase.CLOB ) {
                    appendClob( bw );
                }
            }

            for ( XsdType xsdType : types ) {
                if ( xsdType.base == XsdTypeNameBase.TIMESTAMP ) {
                    appendTimestamp( bw );
                }
            }

            //append all tables
            bw.append( "\t<xs:element name=\"dataset\">" );
            bw.newLine();
            bw.append( "\t\t<xs:complexType>" );
            bw.newLine();
            bw.append( "\t\t\t<xs:choice minOccurs=\"0\" maxOccurs=\"unbounded\">" );
            bw.newLine();
            for ( Entry<String, List<XsdAttribute>> entry : table2collumn.entrySet() ) {
                if ( !entry.getValue().isEmpty() ) {
                    bw.append( "\t\t\t\t<xs:element ref=\"" + entry.getKey() + "\" minOccurs=\"0\" maxOccurs=\"unbounded\" />" );
                    bw.newLine();
                }
            }
            bw.append( "\t\t\t</xs:choice>" );
            bw.newLine();
            bw.append( "\t\t</xs:complexType>" );
            bw.newLine();
            bw.append( "\t</xs:element>" );
            bw.newLine();

            //describe each table
            for ( Entry<String, List<XsdAttribute>> entry : table2collumn.entrySet() ) {
                if ( !entry.getValue().isEmpty() ) {
                    bw.append( "\t<xs:element name=\"" + entry.getKey() + "\">" );
                    bw.newLine();
                    bw.append( "\t\t<xs:complexType>" );
                    bw.newLine();
                    for ( XsdAttribute attr : entry.getValue() ) {
                        String required = ( ( ( update && attr.pk ) || ( !update && attr.required ) ) ? "required" : "optional" );
                        bw.append( "\t\t\t\t<xs:attribute name=\"" + attr.name + "\" type=\"" + deriveType( attr.type ) + "\" use=\"" + required + "\" />" );
                        bw.newLine();
                    }
                    bw.append( "\t\t</xs:complexType>" );
                    bw.newLine();
                    bw.append( "\t</xs:element>" );
                    bw.newLine();
                }
            }
            bw.append( "</xs:schema>" );
            bw.newLine();

        }
    }

    private void appendClob( BufferedWriter bw ) throws IOException {
        bw.append( "\t<xs:simpleType name='varchar_max'>" );
        bw.newLine();
        bw.append( "\t\t<xs:restriction base='xs:string'>" );
        bw.newLine();
        bw.append( "\t\t\t<xs:maxLength value='2147483647'/>" );
        bw.newLine();
        bw.append( "\t\t</xs:restriction>" );
        bw.newLine();
        bw.append( "\t</xs:simpleType>" );
        bw.newLine();
    }

    private void appendTimestamp( BufferedWriter bw ) throws IOException {
        bw.append( "\t<xs:simpleType name='dbUnitTimestamp'>" );
        bw.newLine();
        bw.append( "\t\t<xs:restriction base='xs:string'>" );
        bw.newLine();
        bw.append( "\t\t\t<xs:pattern value='(19|20)\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])' />" );
        bw.newLine();
        bw.append( "\t\t</xs:restriction>" );
        bw.newLine();
        bw.append( "\t</xs:simpleType>" );
        bw.newLine();
    }

    private void appendVarchar( BufferedWriter bw, XsdType xsdType ) throws IOException {
        bw.append( "\t<xs:simpleType name='varchar_" + xsdType.size + "'>" );
        bw.newLine();
        bw.append( "\t\t<xs:restriction base='xs:string'>" );
        bw.newLine();
        bw.append( "\t\t\t<xs:maxLength value='" + xsdType.size + "'/>" );
        bw.newLine();
        bw.append( "\t\t</xs:restriction>" );
        bw.newLine();
        bw.append( "\t</xs:simpleType>" );
        bw.newLine();
    }

    private void appendChar( BufferedWriter bw, XsdType xsdType ) throws IOException {
        bw.append( "\t<xs:simpleType name='char_" + xsdType.size + "'>" );
        bw.newLine();
        bw.append( "\t\t<xs:restriction base='xs:string'>" );
        bw.newLine();
        bw.append( "\t\t\t<xs:length value='" + xsdType.size + "' fixed='true'/>" );
        bw.newLine();
        bw.append( "\t\t</xs:restriction>" );
        bw.newLine();
        bw.append( "\t</xs:simpleType>" );
        bw.newLine();
    }

    private void appendDecimal( BufferedWriter bw, XsdType xsdType ) throws IOException {
        bw.append( "\t<xs:simpleType name='decimal_" + xsdType.size + "_" + xsdType.ext + "'>" );
        bw.newLine();
        bw.append( "\t\t<xs:restriction base='xs:decimal'>" );
        bw.newLine();
        bw.append( "\t\t\t<xs:totalDigits value='" + xsdType.size + "'/>" );
        bw.newLine();
        bw.append( "\t\t\t<xs:fractionDigits value='" + xsdType.ext + "'/>" );
        bw.newLine();
        bw.append( "\t\t</xs:restriction>" );
        bw.newLine();
        bw.append( "\t</xs:simpleType>" );
        bw.newLine();
    }

    private void mapToXsdType( final int dataType, int varcharSize, int columnSize, int decimalDigits, XsdAttribute a ) {
        switch ( dataType ) {
            case BOOLEAN:
            case BIT:
                a.type.base = XsdTypeNameBase.BOOLEAN;
                break;
            case TINYINT:
                a.type.base = XsdTypeNameBase.BYTE;
                break;
            case SMALLINT:
                a.type.base = XsdTypeNameBase.SHORT;
                break;
            case INTEGER:
                a.type.base = XsdTypeNameBase.INT;
                break;
            case BIGINT:
                a.type.base = XsdTypeNameBase.LONG;
                break;
            case FLOAT:
                a.type.base = XsdTypeNameBase.FLOAT;
                break;
            case REAL:
            case DOUBLE:
                a.type.base = XsdTypeNameBase.DOUBLE;
                break;
            case NUMERIC:
            case DECIMAL:
                a.type.base = XsdTypeNameBase.DECIMAL;
                a.type.size = columnSize;
                a.type.ext = decimalDigits;
                break;
            case CHAR:
            case NCHAR:
                a.type.base = XsdTypeNameBase.CHAR;
                a.type.size = varcharSize;
                break;
            case VARCHAR:
            case NVARCHAR:
                a.type.base = XsdTypeNameBase.VARCHAR;
                a.type.size = varcharSize;
                if ( a.type.size == Integer.MAX_VALUE ) {
                    a.type.base = XsdTypeNameBase.CLOB;
                    a.type.size = 0;
                }
                break;
            case LONGVARCHAR:
            case LONGNVARCHAR:
            case CLOB:
            case NCLOB:
                a.type.base = XsdTypeNameBase.CLOB;
                break;
            case DATE:
                a.type.base = XsdTypeNameBase.DATE;
                break;
            case TIME:
                a.type.base = XsdTypeNameBase.TIME;
                break;
            case TIMESTAMP:
                a.type.base = XsdTypeNameBase.TIMESTAMP;
                break;
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
            case BLOB:
                a.type.base = XsdTypeNameBase.BLOB;
                break;
            case REF:
                a.type.base = XsdTypeNameBase.BLOB;
                break;
            case TIME_WITH_TIMEZONE:
                a.type.base = XsdTypeNameBase.TIME;
                break;
            case TIMESTAMP_WITH_TIMEZONE:
                a.type.base = XsdTypeNameBase.TIMESTAMP;
                break;
        }
    }

    private String deriveType( XsdType type ) {
        switch ( type.base ) {
            case BLOB:
                return "xs:base64Binary";
            case BOOLEAN:
                return "xs:boolean";
            case BYTE:
                return "xs:byte";
            case CHAR:
                return "char_" + type.size;
            case CLOB:
                return "varchar_max";
            case DATE:
                return "xs:date";
            case DECIMAL:
                return "decimal_" + type.size + "_" + type.ext;
            case DOUBLE:
                return "xs:double";
            case FLOAT:
                return "xs:float";
            case INT:
                return "xs:int";
            case LONG:
                return "xs:long";
            case SHORT:
                return "xs:short";
            case TIME:
                return "xs:time";
            case TIMESTAMP:
                return "dbUnitTimestamp";
            case VARCHAR:
                return "varchar_" + type.size;
        }
        return null;
    }

    static class XsdAttribute {
        String name;
        boolean required;
        boolean pk;
        XsdType type;

    }

    static enum XsdTypeNameBase {
        DECIMAL, CHAR, VARCHAR, BOOLEAN, BYTE, SHORT, INT, LONG, CLOB, BLOB, DATE, TIME, TIMESTAMP, FLOAT, DOUBLE;
    }

    static class XsdType implements Comparable<XsdType> {
        XsdTypeNameBase base;
        int size;
        int ext;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( base == null ) ? 0 : base.hashCode() );
            result = prime * result + ext;
            result = prime * result + size;
            return result;
        }

        @Override
        public boolean equals( Object obj ) {
            if ( this == obj )
                return true;
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            XsdType other = (XsdType) obj;
            if ( base != other.base )
                return false;
            if ( ext != other.ext )
                return false;
            if ( size != other.size )
                return false;
            return true;
        }

        @Override
        public int compareTo( XsdType o ) {
            int r = base.compareTo( o.base );
            if ( r == 0 ) {
                r = Integer.compare( size, o.size );
            }
            if ( r == 0 ) {
                r = Integer.compare( ext, o.ext );
            }
            return r;
        }

    }

}
