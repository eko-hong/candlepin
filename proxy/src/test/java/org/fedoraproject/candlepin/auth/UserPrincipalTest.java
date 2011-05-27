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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.fedoraproject.candlepin.auth.permissions.Permission;
import org.fedoraproject.candlepin.model.Owner;
import org.fedoraproject.candlepin.model.OwnerPermission;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


/**
 * UserPrincipalTest
 */
public class UserPrincipalTest {
    private Owner owner;
    private OwnerPermission ownerPerm;
    private UserPrincipal user;

    @Before
    public void init() {
        owner = mock(Owner.class);
        ownerPerm = mock(OwnerPermission.class);
        user = new UserPrincipal("icup");
    }

    @Test
    public void usernameMatches() {
        assertEquals("icup", user.getUsername());
        assertEquals(user.getUsername(), user.getPrincipalName());
    }

    @Test
    public void defaultsToAdmin() {
        assertTrue(user.hasFullAccess());
    }

    @Test
    public void shouldNotBeAnAdmin() {
        UserPrincipal up = new UserPrincipal("notadmin", null);
        assertFalse(up.hasFullAccess());
    }

    @Test
    public void accessOwnerGivenPermission() {
        when(owner.getId()).thenReturn("1");
        when(owner.getKey()).thenReturn("owner1");
        when(ownerPerm.getOwner()).thenReturn(owner);
        List<Permission> ops = new ArrayList<Permission>();
        ops.add(ownerPerm);

        UserPrincipal up = new UserPrincipal("admin", ops);

        List<Owner> owners = up.getOwners();
        assertNotNull(owners);
        assertSame(owner, owners.get(0));
    }

    @Test
    public void accessOwnerKeysGivenPermission() {
        when(owner.getId()).thenReturn("1");
        when(owner.getKey()).thenReturn("owner1");
        when(ownerPerm.getOwner()).thenReturn(owner);
        List<Permission> ops = new ArrayList<Permission>();
        ops.add(ownerPerm);

        UserPrincipal up = new UserPrincipal("admin", ops);

        List<String> keys = up.getOwnerKeys();
        assertNotNull(keys);
        assertEquals("owner1", keys.get(0));
    }

    @Test
    public void accessOwnerIdsGivenPermission() {
        when(owner.getId()).thenReturn("1");
        when(owner.getKey()).thenReturn("owner1");
        when(ownerPerm.getOwner()).thenReturn(owner);
        List<Permission> ops = new ArrayList<Permission>();
        ops.add(ownerPerm);

        UserPrincipal up = new UserPrincipal("admin", ops);

        List<String> ids = up.getOwnerIds();
        assertNotNull(ids);
        assertEquals("1", ids.get(0));
    }

    @Test
    public void permsShouldNotAffectOwners() {
        List<Permission> perms = new ArrayList<Permission>();
        perms.add(mock(Permission.class));
        UserPrincipal up = new UserPrincipal("admin", perms);
        assertTrue(up.getOwners().isEmpty());
    }
}
