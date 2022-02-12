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

import org.apache.maven.scm.ScmException;
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.semver.Version;
import org.semver.Version.Element;

import java.util.List;

/**
 *
 * Uses SemVer implementation to increase minor element when resolving the development version
 */
@Component(
           role = VersionPolicy.class,
           hint = "CCSemVerVersionPolicy",
           description = "A VersionPolicy following the SemVer rules and looks at " 
                   + "the commit messages following the Conventional Commits convention."
       )
public class CCSemVerVersionPolicy implements VersionPolicy
{
    public VersionPolicyResult getReleaseVersion( VersionPolicyRequest request )
            throws VersionParseException
    {
        VersionRules versionRules = new VersionRules( request.getConfig() );
        try 
        {
            return getReleaseVersion(
                    request,
                    versionRules,
                    CommitHistory.getHistory( request, versionRules ) );
        }
        catch ( ScmException e ) 
        {
            throw new VersionParseException( "Unable to obtain the information from the SCM history", e );
        }
    }

    public VersionPolicyResult getReleaseVersion( VersionPolicyRequest request,
                                                  VersionRules versionRules,
                                                  CommitHistory commitHistory )
        throws VersionParseException
    {
        String versionString = request.getVersion(); // The current version in the pom
        Version version;

        Element maxElementSinceLastVersionTag = versionRules.getMaxElementSinceLastVersionTag( commitHistory );

        List<String> commitHistoryTags = commitHistory.getTags();
        if ( commitHistoryTags.size() == 1 )
        {
            // Use the latest tag we have
            versionString = commitHistoryTags.get( 0 );
        }

        try
        {
            version = Version.parse( versionString );
        }
        catch ( IllegalArgumentException e )
        {
            throw new VersionParseException( e.getMessage() );
        }

        // If we have a version from the tag we use that + the calculated update.
        // If only have the version from the current pom version with -SNAPSHOT removed.
        if ( maxElementSinceLastVersionTag != null )
        {
            version = version.next( maxElementSinceLastVersionTag );
        }

        VersionPolicyResult result = new VersionPolicyResult();
        result.setVersion( version.toReleaseVersion().toString() );
        return result;
    }

    public VersionPolicyResult getDevelopmentVersion( VersionPolicyRequest request )
        throws VersionParseException
    {
        Version version;
        try 
        {
            version = Version.parse( request.getVersion() );
        }
        catch ( IllegalArgumentException e )
        {
            throw new VersionParseException( e.getMessage() );
        }
        
        version = version.next( Element.MINOR );  
        VersionPolicyResult result = new VersionPolicyResult();
        result.setVersion( version + "-SNAPSHOT" );
        return result;
    }
}
