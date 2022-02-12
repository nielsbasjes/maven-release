package org.apache.maven.shared.release.policy.ccsemver;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CCSemVerVersionPolicyTest
{

    private void verifyNextVersion(
            String versionRulesConfig,
            String pomVersion,
            String expectedVersion,
            String comment,
            String... tags
    ) throws VersionParseException
    {
        VersionPolicyRequest request = new VersionPolicyRequest();
        request.setMetaData( null );
        request.setScmProvider( FakeSCM.getSCMProvider( comment, tags ) );
        request.setScmRepository( FakeSCM.getScmRepository() );
        request.setBasedir( "/tmp" );
        request.setConfig( versionRulesConfig );
        request.setVersion( pomVersion );

        String suggestedVersion = new CCSemVerVersionPolicy().getReleaseVersion( request ).getVersion();

        assertEquals( expectedVersion, suggestedVersion );
    }

    private void verifyNextVersionMustFail(
            String versionRulesConfig,
            String pomVersion,
            String comment,
            String... tags
    )
    {
        try
        {
            verifyNextVersion( versionRulesConfig, pomVersion, "ignore", comment, tags );
        }
        catch ( VersionParseException vpe )
        {
            // Success !
            return;
        }
        fail( "Should have failed" );
    }

    @Test
    public void testDefaultVersionRules() throws VersionParseException
    {
        String normal = "Did something";
        String patch = "fix(core): Another fix.";
        String minor = "feat(core): New thingy.";
        String major = "fix!(core): Breaking improvement";

        String versionRulesConfig = "";
        verifyNextVersion( versionRulesConfig, "1.1.1-SNAPSHOT", "1.1.1", normal ); // No Tag - No Comments
        verifyNextVersion( versionRulesConfig, "1.1.1-SNAPSHOT", "2.0.0", major );  // No Tag - Major Comments
        verifyNextVersion( versionRulesConfig, "1.1.1-SNAPSHOT", "1.2.0", minor );  // No Tag - Minor Comments
        verifyNextVersion( versionRulesConfig, "1.1.1-SNAPSHOT", "1.1.1", patch );  // No Tag - Patch Comments
        verifyNextVersion( versionRulesConfig, "1.1.1-SNAPSHOT", "2.3.5", normal, "2.3.4", "v3.4.5" ); // Tag - No Comments
        verifyNextVersion( versionRulesConfig, "1.1.1-SNAPSHOT", "3.0.0", major,  "2.3.4", "v3.4.5" ); // Tag - Major Comments
        verifyNextVersion( versionRulesConfig, "1.1.1-SNAPSHOT", "2.4.0", minor,  "2.3.4", "v3.4.5" ); // Tag - Minor Comments
        verifyNextVersion( versionRulesConfig, "1.1.1-SNAPSHOT", "2.3.5", patch,  "2.3.4", "v3.4.5" ); // Tag - Patch Comments

        // Too many valid version tags on one commit
        verifyNextVersionMustFail( versionRulesConfig, "1.1.1-SNAPSHOT", major, "1.1.1", "2.2.2" );
    }

    @Test
    public void testCustomTagPattern() throws VersionParseException {
        String normal = "Did something";
        String patch = "fix(core): Another fix.";
        String minor = "feat(core): New thingy.";
        String major = "fix!(core): Breaking improvement";

        String versionRulesConfig = ""
                + "<cCSemverConfig>"
                +   "<versionTag>^v([0-9]+(?:\\.[0-9]+(?:\\.[0-9]+)?)?)$</versionTag>"
                + "</cCSemverConfig>" +
                "";

        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "2.2.2", normal ); // No Tag - No Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "3.0.0", major );  // No Tag - Major Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "2.3.0", minor );  // No Tag - Minor Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "2.2.2", patch );  // No Tag - Patch Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "3.4.6", normal, "2.3.4", "v3.4.5" ); // Tag - No Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "4.0.0", major,  "2.3.4", "v3.4.5" ); // Tag - Major Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "3.5.0", minor,  "2.3.4", "v3.4.5" ); // Tag - Minor Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "3.4.6", patch,  "2.3.4", "v3.4.5" ); // Tag - Patch Comments

        // Too many valid version tags on one commit
        verifyNextVersionMustFail( versionRulesConfig, "2.2.2-SNAPSHOT", minor, "v1.1.1", "v2.2.2" );
    }

    @Test
    public void testCustomVersionRules() throws VersionParseException {
        String normal = "This is a different commit.";
        String patch  = "This is a No Change commit.";
        String minor  = "This is a Nice Change commit.";
        String major  = "This is a Big Change commit.";

        String versionRulesConfig = ""
            + "<cCSemverConfig>"
            +   "<versionTag>^The awesome ([0-9]+(?:\\.[0-9]+(?:\\.[0-9]+)?)?) release$</versionTag>"
            +   "<majorRules>"
            +     "<majorRule>^.*Big Change.*$</majorRule>"
            +   "</majorRules>"
            +   "<minorRules>"
            +     "<minorRule>^.*Nice Change.*$</minorRule>"
            +   "</minorRules>"
            + "</cCSemverConfig>" +
            "";

        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "2.2.2", normal ); // No Tag - No Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "3.0.0", major );  // No Tag - Major Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "2.3.0", minor );  // No Tag - Minor Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "2.2.2", patch );  // No Tag - Patch Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "3.4.6", normal, "2.3.4", "The awesome 3.4.5 release" ); // Tag - No Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "4.0.0", major,  "2.3.4", "The awesome 3.4.5 release" ); // Tag - Major Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "3.5.0", minor,  "2.3.4", "The awesome 3.4.5 release" ); // Tag - Minor Comments
        verifyNextVersion( versionRulesConfig, "2.2.2-SNAPSHOT", "3.4.6", patch,  "2.3.4", "The awesome 3.4.5 release" ); // Tag - Patch Comments

        // Too many valid version tags on one commit
        verifyNextVersionMustFail( versionRulesConfig, "2.2.2-SNAPSHOT", minor, "The awesome 1.1.1 release", "The awesome 2.2.2 release" );
    }

}
