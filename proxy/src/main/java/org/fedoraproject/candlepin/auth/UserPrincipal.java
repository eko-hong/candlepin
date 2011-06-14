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
package org.fedoraproject.candlepin.auth;

import java.util.LinkedList;
import java.util.List;
import org.fedoraproject.candlepin.auth.permissions.Permission;
import org.fedoraproject.candlepin.auth.permissions.UserUserPermission;
import org.fedoraproject.candlepin.model.OwnerPermission;

import java.util.Collection;
import org.fedoraproject.candlepin.model.Owner;

/**
 *
 */
public class UserPrincipal extends Principal {

    private String username;
    private boolean admin;

    /**
     * Create a user principal with full system access.
     * 
     * @param username
     */
    public UserPrincipal(String username) {
        this(username, null);

        // TODO: a little risky, quite easy to just use the easier constructor
        // available not expecting it to be a super admin:
        this.admin = true;
        addPermissionToManageSelf();
    }

    public UserPrincipal(String username, Collection<Permission> permissions) {
        this.username = username;

        if (permissions != null) {
            this.permissions.addAll(permissions);
        }
        
        this.admin = false;
        addPermissionToManageSelf();
    }


    /*
     * User principals should have an implicit permission to view their own
     * data.
     */
    private void addPermissionToManageSelf() {
        this.permissions.add(new UserUserPermission(username));
    }

    public String getUsername() {
        return username;
    }

    // Note: automatically generated by Netbeans
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UserPrincipal other = (UserPrincipal) obj;
        if ((this.username == null) ?
            (other.username != null) : !this.username.equals(other.username)) {
            return false;
        }
        return true;
    }

    // Note: automatically generated by Netbeans
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + (this.username != null ? this.username.hashCode() : 0);
        return hash;
    }

    @Override
    public String getType() {
        return "user";
    }

    @Override
    public String getPrincipalName() {
        return username;
    }

    @Override
    public boolean hasFullAccess() {
        return this.admin;
    }

    public List<String> getOwnerIds() {
        List<String> ownerIds = new LinkedList<String>();

        for (Owner owner : getOwners()) {
            ownerIds.add(owner.getId());
        }

        return ownerIds;
    }

    public List<String> getOwnerKeys() {
        List<String> ownerKeys = new LinkedList<String>();

        for (Owner owner : getOwners()) {
            ownerKeys.add(owner.getKey());
        }

        return ownerKeys;
    }

    public List<Owner> getOwners() {
        List<Owner> owners = new LinkedList<Owner>();

        for (Permission permission : permissions) {
            if (permission instanceof OwnerPermission) {
                owners.add(((OwnerPermission) permission).getOwner());
            }
        }

        return owners;
    }

    @Override
    public boolean canAccess(Object target, Access access) {
        if (this.admin) {
            return true;
        }

        return super.canAccess(target, access);
    }

}
