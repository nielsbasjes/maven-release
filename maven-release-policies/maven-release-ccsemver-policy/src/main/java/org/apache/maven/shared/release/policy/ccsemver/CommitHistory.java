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


import org.apache.maven.scm.ChangeSet;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.changelog.ChangeLogScmRequest;
import org.apache.maven.scm.command.changelog.ChangeLogScmResult;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.versions.VersionParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Helper class
 */
public class CommitHistory
{
    private final List<String> changes = new ArrayList<>();
    private final List<String> tags = new ArrayList<>();

    public List<String> getChanges()
    {
        return changes;
    }

    public void addChanges( String change )
    {
        this.changes.add( change );
    }

    public void addChanges( List<String> changes )
    {
        this.changes.addAll( changes );
    }

    public List<String> getTags()
    {
        return tags;
    }

    public void addTags( List<String> tags )
    {
        if ( tags != null )
        {
            tags.forEach( this::addTags );
        }
    }

    public void addTags( String tag )
    {
        if ( tag != null && !tag.isEmpty() )
        {
            this.tags.add( tag );
        }
    }

    public String getLastVersionTag()
    {
        if ( tags.size() != 1 )
        {
            return null;
        }
        return tags.get( 0 );
    }

    public static CommitHistory getHistory( VersionPolicyRequest request, VersionRules versionRules )
            throws ScmException, VersionParseException
    {
        CommitHistory commitHistory = new CommitHistory();

        ScmRepository scmRepository = request.getScmRepository();
        ScmProvider scmProvider = request.getScmProvider();

        ChangeLogScmRequest changeLogRequest = new ChangeLogScmRequest(
                scmRepository,
                new ScmFileSet( new File( request.getWorkingDirectory() ) )
        );

        int limit = 0;
        while ( commitHistory.getTags().isEmpty() )
        {
            limit += 10;
            changeLogRequest.setLimit( null );
            changeLogRequest.setLimit( limit );
            commitHistory.changes.clear();

            ChangeLogScmResult changeLog = scmProvider.changeLog( changeLogRequest );

            for ( ChangeSet changeSet : changeLog.getChangeLog().getChangeSets() )
            {
                List<String> changeSetTags = changeSet.getTags();
                List<String> versionTags = changeSetTags
                        .stream()
                        .map( tag ->
                            {
                                Matcher matcher = versionRules.getTagPattern().matcher( tag );
                                if ( matcher.find() )
                                {
                                    return matcher.group( 1 );
                                }
                                return null;
                            }
                        )
                        .filter( Objects::nonNull )
                        .collect( Collectors.toList() );

                if ( !versionTags.isEmpty() )
                {
                    // Found the previous release tag
                    if ( versionTags.size() > 1 )
                    {
                        throw new VersionParseException( "Most recent commit with tags has multiple version tags: "
                                + versionTags );
                    }
                    commitHistory.addTags( versionTags );
                    break; // We have the last version tag
                }
                else
                {
                    commitHistory.addChanges( changeSet.getComment() );
                }
            }
            if ( changeLog.getChangeLog().getChangeSets().size() <= limit )
            {
                // Apparently there are simply no more commits.
                break;
            }
        }
        return commitHistory;
    }

}
