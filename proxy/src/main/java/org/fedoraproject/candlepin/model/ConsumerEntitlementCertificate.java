/**
 * Copyright (c) 2009 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.fedoraproject.candlepin.model;

import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.ForeignKey;

/**
 * Represents certificate used to entitle a consumer
 */
@XmlRootElement(name = "cert")
@XmlAccessorType(XmlAccessType.PROPERTY)
@Entity
@Table(name = "cp_ent_certificate")
@SequenceGenerator(name = "seq_consumer_ent_cert", 
                   sequenceName = "seq_consumer_ent_cert", allocationSize = 1)
public class ConsumerEntitlementCertificate implements Persisted {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
                    generator = "seq_consumer_ent_cert")
    private Long id;

    @Column(nullable = false)
    private byte[] key;

    @Column(nullable = false)
    private byte[] cert;

    @Column(nullable = false)
    private BigInteger serial;

    @ManyToOne
    @ForeignKey(name = "fk_cert_entitlement")
    @JoinColumn(nullable = false)
    private Entitlement entitlement;

    public BigInteger getSerial() {
        return serial;
    }

    public void setSerial(BigInteger serialNumber) {
        this.serial = serialNumber;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    @XmlTransient
    public byte[] getKey() {
        return key;
    }
    
    @XmlElement(name = "key")
    public String getKeyAsString() {
        return new String(key);
    }

    public void setCert(byte[] cert) {
        this.cert = cert;
    }

    @XmlTransient
    public byte[] getCert() {
        return cert;
    }
    
    @XmlElement(name = "cert")
    public String getCertAsString() {
        return new String(cert);
    }

    @XmlTransient
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Entitlement getEntitlement() {
        return entitlement;
    }

    public void setEntitlement(Entitlement entitlement) {
        this.entitlement = entitlement;
    }
}
