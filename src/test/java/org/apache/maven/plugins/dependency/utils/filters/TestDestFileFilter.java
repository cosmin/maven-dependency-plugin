package org.apache.maven.plugins.dependency.utils.filters;

/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.dependency.testUtils.DependencyArtifactStubFactory;
import org.apache.maven.plugins.dependency.testUtils.DependencyTestUtils;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;

/**
 * @author brianf
 */
public class TestDestFileFilter
    extends TestCase
{
    Set<Artifact> artifacts = new HashSet<>();

    Log log = new SilentLog();

    File outputFolder;

    DependencyArtifactStubFactory fact;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        outputFolder = new File( "target/markers/" );
        DependencyTestUtils.removeDirectory( outputFolder );
        assertFalse( outputFolder.exists() );

        this.fact = new DependencyArtifactStubFactory( outputFolder, false );
        artifacts = fact.getReleaseAndSnapshotArtifacts();
    }

    protected void tearDown()
        throws IOException
    {
        DependencyTestUtils.removeDirectory( outputFolder );
    }

    public File createFile( Artifact artifact )
        throws IOException
    {
        return createFile( artifact, false, false, false );
    }

    public File createFile( Artifact artifact, boolean useSubDirectoryPerArtifact, boolean useSubDirectoryPerType,
                            boolean removeVersion )
        throws IOException
    {
        return createFile( artifact, useSubDirectoryPerArtifact, useSubDirectoryPerType, removeVersion, false );
    }

    public File createFile( Artifact artifact, boolean useSubDirectoryPerArtifact, boolean useSubDirectoryPerType,
                            boolean removeVersion, boolean removeClassifier )
        throws IOException
    {
        File destFolder =
            DependencyUtil.getFormattedOutputDirectory( false, useSubDirectoryPerType, useSubDirectoryPerArtifact,
                                                        false, false, outputFolder, artifact );
        File destFile =
            new File( destFolder,
                      DependencyUtil.getFormattedFileName( artifact, removeVersion, false, false, removeClassifier ) );

        destFile.getParentFile().mkdirs();
        assertTrue( destFile.createNewFile() );
        return destFile;
    }

    public void testDestFileRelease()
        throws IOException, ArtifactFilterException
    {
        DestFileFilter filter = new DestFileFilter( outputFolder );
        Artifact artifact = fact.getReleaseArtifact();

        assertTrue( filter.isArtifactIncluded( artifact ) );
        createFile( artifact );
        assertFalse( filter.isArtifactIncluded( artifact ) );

        filter.setOverWriteReleases( true );
        assertTrue( filter.isArtifactIncluded( artifact ) );
    }

    public void testDestFileSnapshot()
        throws IOException, ArtifactFilterException
    {
        DestFileFilter filter = new DestFileFilter( outputFolder );
        Artifact artifact = fact.getSnapshotArtifact();

        assertTrue( filter.isArtifactIncluded( artifact ) );
        createFile( artifact );
        assertFalse( filter.isArtifactIncluded( artifact ) );

        filter.setOverWriteSnapshots( true );
        assertTrue( filter.isArtifactIncluded( artifact ) );
    }

    public void testDestFileStripVersion()
        throws IOException, ArtifactFilterException
    {
        DestFileFilter filter = new DestFileFilter( outputFolder );
        Artifact artifact = fact.getSnapshotArtifact();
        filter.setRemoveVersion( true );

        assertTrue( filter.isArtifactIncluded( artifact ) );
        createFile( artifact, false, false, true );
        assertFalse( filter.isArtifactIncluded( artifact ) );

        filter.setOverWriteSnapshots( true );
        assertTrue( filter.isArtifactIncluded( artifact ) );
    }

    public void testDestFileStripClassifier()
        throws IOException, ArtifactFilterException
    {
        DestFileFilter filter = new DestFileFilter( outputFolder );
        Artifact artifact = fact.getSnapshotArtifact();
        filter.setRemoveClassifier( true );

        assertTrue( filter.isArtifactIncluded( artifact ) );
        createFile( artifact, false, false, false, true );
        assertFalse( filter.isArtifactIncluded( artifact ) );

        filter.setOverWriteSnapshots( true );
        assertTrue( filter.isArtifactIncluded( artifact ) );
    }

    public void testDestFileSubPerArtifact()
        throws IOException, ArtifactFilterException
    {
        DestFileFilter filter = new DestFileFilter( outputFolder );
        Artifact artifact = fact.getSnapshotArtifact();
        filter.setUseSubDirectoryPerArtifact( true );

        assertTrue( filter.isArtifactIncluded( artifact ) );
        createFile( artifact, true, false, false );
        assertFalse( filter.isArtifactIncluded( artifact ) );

        filter.setOverWriteSnapshots( true );
        assertTrue( filter.isArtifactIncluded( artifact ) );
    }

    public void testDestFileSubPerType()
        throws MojoExecutionException, IOException, ArtifactFilterException
    {
        DestFileFilter filter = new DestFileFilter( outputFolder );
        Artifact artifact = fact.getSnapshotArtifact();
        filter.setUseSubDirectoryPerType( true );

        assertTrue( filter.isArtifactIncluded( artifact ) );
        createFile( artifact, false, true, false );
        assertFalse( filter.isArtifactIncluded( artifact ) );

        filter.setOverWriteSnapshots( true );
        assertTrue( filter.isArtifactIncluded( artifact ) );
    }

    public void testDestFileOverwriteIfNewer()
        throws MojoExecutionException, IOException, ArtifactFilterException
    {
        DestFileFilter filter = new DestFileFilter( outputFolder );

        fact.setCreateFiles( true );
        Artifact artifact = fact.getSnapshotArtifact();
        File artifactFile = artifact.getFile();
        assertTrue( artifactFile.setLastModified( artifactFile.lastModified() ) );
        filter.setOverWriteIfNewer( true );

        // should pass because the file doesn't exist yet.
        assertTrue( filter.isArtifactIncluded( artifact ) );

        // create the file in the destination
        File destFile = createFile( artifact, false, false, false );

        // set the last modified timestamp to be older than the source
        assertTrue( destFile.setLastModified( artifactFile.lastModified() - 1000 ) );
        assertTrue( filter.isArtifactIncluded( artifact ) );

        // now set the last modified timestamp to be newer than the source
        assertTrue( destFile.setLastModified( artifactFile.lastModified() + 1000 ) );

        assertFalse( filter.isArtifactIncluded( artifact ) );
    }

    public void testGettersSetters()
    {
        DestFileFilter filter = new DestFileFilter( null );
        assertNull( filter.getOutputFileDirectory() );
        filter.setOutputFileDirectory( outputFolder );
        assertSame( outputFolder, filter.getOutputFileDirectory() );

        filter.setOverWriteIfNewer( true );
        assertTrue( filter.isOverWriteIfNewer() );
        filter.setOverWriteIfNewer( false );
        assertFalse( filter.isOverWriteIfNewer() );

        filter.setOverWriteReleases( true );
        assertTrue( filter.isOverWriteReleases() );
        filter.setOverWriteReleases( false );
        assertFalse( filter.isOverWriteReleases() );

        filter.setOverWriteSnapshots( true );
        assertTrue( filter.isOverWriteSnapshots() );
        filter.setOverWriteSnapshots( false );
        assertFalse( filter.isOverWriteSnapshots() );

        filter.setUseSubDirectoryPerArtifact( true );
        assertTrue( filter.isUseSubDirectoryPerArtifact() );
        filter.setUseSubDirectoryPerArtifact( false );
        assertFalse( filter.isUseSubDirectoryPerArtifact() );

        filter.setUseSubDirectoryPerType( true );
        assertTrue( filter.isUseSubDirectoryPerType() );
        filter.setUseSubDirectoryPerType( false );
        assertFalse( filter.isUseSubDirectoryPerType() );

        filter.setRemoveVersion( true );
        assertTrue( filter.isRemoveVersion() );
        filter.setRemoveVersion( false );
        assertFalse( filter.isRemoveVersion() );
    }
}
