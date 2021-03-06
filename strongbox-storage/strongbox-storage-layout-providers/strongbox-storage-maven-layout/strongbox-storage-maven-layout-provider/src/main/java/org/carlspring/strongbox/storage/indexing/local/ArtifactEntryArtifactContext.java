package org.carlspring.strongbox.storage.indexing.local;

import org.carlspring.strongbox.artifact.coordinates.MavenArtifactCoordinates;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.domain.MavenArtifactEntryUtils;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author Przemyslaw Fusik
 */
public class ArtifactEntryArtifactContext
        extends ArtifactContext
{

    private final ArtifactEntry artifactEntry;
    private final ArtifactEntryArtifactContextHelper artifactEntryArtifactContextHelper;

    public ArtifactEntryArtifactContext(final ArtifactEntry artifactEntry,
                                        final ArtifactEntryArtifactContextHelper artifactEntryArtifactContextHelper)
            throws IllegalArgumentException
    {
        super(null, null, null, asArtifactInfo(artifactEntry), asGav(artifactEntry));
        this.artifactEntry = artifactEntry;
        this.artifactEntryArtifactContextHelper = artifactEntryArtifactContextHelper;
    }

    private static ArtifactInfo asArtifactInfo(ArtifactEntry artifactEntry)
    {
        final MavenArtifactCoordinates coordinates = (MavenArtifactCoordinates) artifactEntry.getArtifactCoordinates();
        ArtifactInfo artifactInfo = new ArtifactInfo(artifactEntry.getRepositoryId(), coordinates.getGroupId(),
                                                     coordinates.getArtifactId(),
                                                     coordinates.getVersion(), coordinates.getClassifier(),
                                                     coordinates.getExtension());

        produce(coordinates, artifactInfo);

        return artifactInfo;
    }

    /**
     * @see org.apache.maven.index.DefaultArtifactContextProducer#getArtifactContext(org.apache.maven.index.context.IndexingContext, java.io.File)
     */
    private static void produce(MavenArtifactCoordinates coordinates,
                                ArtifactInfo artifactInfo)
    {
        if (!StringUtils.isEmpty(coordinates.getClassifier()))
        {
            artifactInfo.setPackaging(coordinates.getExtension());
        }

        artifactInfo.setFileName(FilenameUtils.getName(coordinates.toPath()));
        artifactInfo.setFileExtension(coordinates.getExtension());
    }

    private static Gav asGav(ArtifactEntry artifactEntry)
    {
        return MavenArtifactEntryUtils.toGav(artifactEntry);
    }

    public ArtifactEntry getArtifactEntry()
    {
        return artifactEntry;
    }

    @Override
    public File getArtifact()
    {
        throw new UnsupportedOperationException("This ArtifactContext base on ArtifactEntry");
    }

    @Override
    public File getMetadata()
    {
        throw new UnsupportedOperationException("This ArtifactContext base on ArtifactEntry");
    }

    @Override
    public File getPom()
    {
        throw new UnsupportedOperationException("This ArtifactContext base on ArtifactEntry");
    }

    @Override
    public Model getPomModel()
    {
        throw new UnsupportedOperationException("This ArtifactContext base on ArtifactEntry");
    }

    public boolean pomExists()
    {
        return artifactEntryArtifactContextHelper.pomExists();
    }

    public boolean sourcesExists()
    {
        return artifactEntryArtifactContextHelper.sourcesExists();
    }

    public boolean javadocExists()
    {
        return artifactEntryArtifactContextHelper.javadocExists();
    }
}
