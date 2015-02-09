/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
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
package org.candlepin.model;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Represents a Product that can be consumed and entitled. Products define the
 * software or entity they want to entitle i.e. RHEL Server. They also contain
 * descriptive meta data that might limit the Product i.e. 4 cores per server
 * with 4 guests.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
@Entity
@Table(name = "cp_org_product")
public class OrgProduct extends AbstractHibernateObject implements Linkable {

    public static final  String UEBER_PRODUCT_POSTFIX = "_ueber_product";

    // Product ID is stored as a string.
    // This is a subset of the product OID known as the hash.
    @Id
    @Column(length = 32, unique = true)
    @NotNull
    private String id;

    @Column(nullable = false)
    @Size(max = 255)
    @NotNull
    private String name;

    @ManyToOne
    @ForeignKey(name = "fk_product_owner")
    @JoinColumn(nullable = false)
    @Index(name = "cp_product_owner_fk_idx")
    @NotNull
    private Owner owner;

    /**
     * How many entitlements per quantity
     */
    @Column
    private Long multiplier;

    // NOTE: we need a product "type" so we can tell what class of
    // product we are...

    @OneToMany(mappedBy = "product")
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
        org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    private Set<OrgProductAttribute> attributes;

    @ElementCollection
    @CollectionTable(name = "cp_product_content",
                     joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "element")
    @LazyCollection(LazyCollectionOption.EXTRA) // allows .size() without loading all data
    private List<OrgProductContent> productContent;

    @ElementCollection
    @CollectionTable(name = "cp_product_dependent_products",
                     joinColumns = @JoinColumn(name = "cp_product_id"))
    @Column(name = "element")
    private Set<String> dependentProductIds;

    protected OrgProduct() {
        // Intentionally left empty
    }

    /**
     * Constructor Use this variant when creating a new object to persist.
     *
     * @param id Product label
     * @param name Human readable Product name
     */
    public OrgProduct(String id, String name) {
        this(id, name, 1L);
    }

    public OrgProduct(String id, String name, Long multiplier) {
        setId(id);
        setName(name);
        setMultiplier(multiplier);
        setAttributes(new HashSet<OrgProductAttribute>());
        setProductContent(new LinkedList<OrgProductContent>());
        setSubscriptions(new LinkedList<Subscription>());
        setDependentProductIds(new HashSet<String>());
    }

    public OrgProduct(String id, String name, String variant, String version,
        String arch, String type) {
        setId(id);
        setName(name);
        setMultiplier(1L);
        setAttributes(new HashSet<OrgProductAttribute>());
        setProductContent(new LinkedList<OrgProductContent>());
        setSubscriptions(new LinkedList<Subscription>());
        setDependentProductIds(new HashSet<String>());
        setAttribute("version", version);
        setAttribute("variant", variant);
        setAttribute("type", type);
        setAttribute("arch", arch);
    }

    public static Product createUeberProductForOwner(Owner o) {
        return new Product(null, ueberProductNameForOwner(o), 1L);
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;

    }

    /**
     * @param id product id
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Product [id = " + id + ", name = " + name + "]";
    }

    /**
     * @return the product name
     */
    public String getName() {
        return name;
    }

    /**
     * sets the product name.
     *
     * @param name name of the product
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return The product's owner/organization
     */
    public Owner getOwner() {
        return this.owner;
    }

    /**
     * Sets the product's owner.
     *
     * @param owner
     *  The new owner/organization for this product.
     */
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * @return the number of entitlements to create from a single subscription
     */
    public Long getMultiplier() {
        return multiplier;
    }

    /**
     * @param multiplier the multiplier to set
     */
    public void setMultiplier(Long multiplier) {
        if (multiplier == null) {
            this.multiplier = 1L;
        }
        else {
            this.multiplier = Math.max(1L, multiplier);
        }
    }

    public void setAttributes(Set<OrgProductAttribute> attributes) {
        this.attributes = attributes;
    }

    public void setAttribute(String key, String value) {
        OrgProductAttribute existing = getAttribute(key);
        if (existing != null) {
            existing.setValue(value);
        }
        else {
            OrgProductAttribute attr = new OrgProductAttribute(key, value);
            attr.setProduct(this);
            addAttribute(attr);
        }
    }

    public void addAttribute(OrgProductAttribute attrib) {
        if (this.attributes == null) {
            this.attributes = new HashSet<OrgProductAttribute>();
        }

        attrib.setProduct(this);
        this.attributes.add(attrib);
    }

    public Set<OrgProductAttribute> getAttributes() {
        return attributes;
    }

    public OrgProductAttribute getAttribute(String key) {
        if (attributes != null) {
            for (OrgProductAttribute a : attributes) {
                if (a.getName().equals(key)) {
                    return a;
                }
            }
        }
        return null;
    }

    public String getAttributeValue(String key) {
        if (attributes != null) {
            for (OrgProductAttribute a : attributes) {
                if (a.getName().equals(key)) {
                    return a.getValue();
                }
            }
        }
        return null;
    }

    @XmlTransient
    public Set<String> getAttributeNames() {
        Set<String> toReturn = new HashSet<String>();

        if (attributes != null) {
            for (OrgProductAttribute attribute : attributes) {
                toReturn.add(attribute.getName());
            }
        }
        return toReturn;
    }

    public boolean hasAttribute(String key) {
        if (attributes != null) {
            for (OrgProductAttribute attribute : attributes) {
                if (attribute.getName().equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasContent(String contentId) {
        if (this.getProductContent() != null) {
            for (OrgProductContent pc : getProductContent()) {
                if (pc.getContent().getId().equals(contentId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (!(anObject instanceof OrgProduct)) {
            return false;
        }

        OrgProduct another = (OrgProduct) anObject;

        return id.equals(another.getId()) && name.equals(another.getName());
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31;
    }

    /**
     * @param content
     */
    public void addContent(OrgContent content) {
        if (productContent == null) {
            productContent = new LinkedList<OrgProductContent>();
        }

        productContent.add(new OrgProductContent(this, content, false));
    }

    /**
     * @param content
     */
    public void addEnabledContent(OrgContent content) {
        if (productContent == null) {
            productContent = new LinkedList<OrgProductContent>();
        }

        productContent.add(new OrgProductContent(this, content, true));
    }

    /**
     * @param productContent the productContent to set
     */
    public void setProductContent(List<OrgProductContent> productContent) {
        this.productContent = productContent;
    }

    /**
     * @return the productContent
     */
    public List<OrgProductContent> getProductContent() {
        return productContent;
    }

    // FIXME: this seems wrong, shouldn't this reset the content
    // not add to it?
    public void setContent(Set<OrgContent> content) {
        if (content == null) {
            return;
        }
        if (productContent == null) {
            productContent = new LinkedList<OrgProductContent>();
        }
        for (OrgContent newContent : content) {
            productContent.add(new OrgProductContent(this, newContent, false));
        }
    }

    public void setEnabledContent(Set<OrgContent> content) {
        if (content == null) {
            return;
        }
        if (productContent == null) {
            productContent = new LinkedList<OrgProductContent>();
        }
        for (OrgContent newContent : content) {
            productContent.add(new OrgProductContent(this, newContent, true));
        }
    }

    @XmlTransient
    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    /**
     * @param dependentProductIds the dependentProductIds to set
     */
    public void setDependentProductIds(Set<String> dependentProductIds) {
        this.dependentProductIds = dependentProductIds;
    }

    /**
     * @return the dependentProductIds
     */
    public Set<String> getDependentProductIds() {
        return dependentProductIds;
    }

    @Override
    public String getHref() {
        return "/products/" + getId();
    }

    @Override
    public void setHref(String href) {
        /*
         * No-op, here to aid with updating objects which have nested objects
         * that were originally sent down to the client in HATEOAS form.
         */
    }

    /**
     * Returns true if this product has a content set which modifies the given
     * product:
     *
     * @param productId
     * @return true if this product modifies the given product ID
     */
    public boolean modifies(String productId) {
        if (getProductContent() != null) {
            for (OrgProductContent pc : getProductContent()) {
                if (pc.getContent().getModifiedProductIds().contains(productId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String ueberProductNameForOwner(Owner o) {
        return o.getKey() + UEBER_PRODUCT_POSTFIX;
    }

}