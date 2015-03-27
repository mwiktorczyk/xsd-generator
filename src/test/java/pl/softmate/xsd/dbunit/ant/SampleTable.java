package pl.softmate.xsd.dbunit.ant;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SampleTable {

    @Id
    public Long c1;

    @Column(nullable = false)
    public int c2;

    @Column(nullable = true, length = 100)
    public String c3;

    @Column(nullable = true, precision = 5, scale = 2)
    public BigDecimal c4;

}
