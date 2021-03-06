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
package org.candlepin.util;

import static org.junit.Assert.*;

import org.candlepin.common.exceptions.BadRequestException;
import org.candlepin.model.ContentOverride;
import org.candlepin.test.DatabaseTestFixture;

import org.junit.Test;
import org.xnap.commons.i18n.I18n;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

/**
 * ContentOverrideValidatorTest
 */
public class ContentOverrideValidatorTest extends DatabaseTestFixture  {
    @Inject private I18n i18n;

    //@Inject private OverrideRules overrideRules;
    @Inject private ContentOverrideValidator validator;

    @Test
    public void testValidateValidCollection() {
        List<ContentOverride> overrides = new LinkedList<ContentOverride>();
        overrides.add(new ContentOverride("label", "testname", "value"));
        overrides.add(new ContentOverride("other label", "other name", "other value"));

        validator.validate(overrides);
    }

    @Test
    public void testValidateValidOverride() {
        ContentOverride override = new ContentOverride("label", "testname", "value");
        validator.validate(override);
    }

    @Test
    public void testValidateSingleInvalid() {
        ContentOverride override = new ContentOverride("label", "baseurl", "value");

        try {
            validator.validate(override);
            fail("Expected exception was \"BadRequestException\" not thrown.");
        }
        catch (BadRequestException bre) {
            assertEquals("Not allowed to override values for: baseurl", bre.getMessage());
        }
    }

    @Test
    public void testValidateCollectionBothInvalid() {
        List<ContentOverride> overrides = new LinkedList<ContentOverride>();
        overrides.add(new ContentOverride("label", "baseurl", "value"));
        overrides.add(new ContentOverride("other label", "name", "other value"));

        try {
            validator.validate(overrides);
            fail("Expected exception \"BadRequestException\" was not thrown.");
        }
        catch (BadRequestException bre) {
            assertTrue(bre.getMessage().matches(
                "^Not allowed to override values for: (?:baseurl, name|name, baseurl)"
            ));
        }

    }
}
