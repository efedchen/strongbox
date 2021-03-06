package org.carlspring.strongbox.cron.jobs;

import org.carlspring.strongbox.artifact.MavenArtifactUtils;
import org.carlspring.strongbox.config.Maven2LayoutProviderCronTasksTestConfig;
import org.carlspring.strongbox.data.CacheManagerTestExecutionListener;
import org.carlspring.strongbox.providers.io.RootRepositoryPath;
import org.carlspring.strongbox.services.ArtifactMetadataService;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.testing.artifact.ArtifactManagementTestExecutionListener;
import org.carlspring.strongbox.testing.artifact.MavenTestArtifact;
import org.carlspring.strongbox.testing.repository.MavenRepository;
import org.carlspring.strongbox.testing.storage.repository.RepositoryManagementTestExecutionListener;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import static org.awaitility.Awaitility.await;
import static org.carlspring.strongbox.storage.repository.RepositoryPolicyEnum.SNAPSHOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Kate Novik.
 * @author Pablo Tirado
 */
@ContextConfiguration(classes = Maven2LayoutProviderCronTasksTestConfig.class)
@SpringBootTest
@ActiveProfiles(profiles = "test")
@TestExecutionListeners(listeners = { CacheManagerTestExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Execution(CONCURRENT)
public class RemoveTimestampedMavenSnapshotCronJobTestIT
        extends BaseCronJobWithMavenIndexingTestCase
{

    private static final String REPOSITORY_SNAPSHOTS = "rtmscj-snapshots";

    private static final String ARTIFACT_BASE_PATH_STRONGBOX_TIMESTAMPED_FIRST = "org/carlspring/strongbox/strongbox-timestamped-first";
    private static final String ARTIFACT_BASE_PATH_STRONGBOX_TIMESTAMPED_SECOND = "org/carlspring/strongbox/strongbox-timestamped-second";

    private static final String GROUP_ID = "org.carlspring.strongbox";
    private static final String ARTIFACT_ID1 = "strongbox-timestamped-first";
    private static final String ARTIFACT_ID2 = "strongbox-timestamped-second";

    @Inject
    private ArtifactMetadataService artifactMetadataService;

    private void rebuildArtifactsMetadata(Repository repository)
            throws Exception
    {
        artifactMetadataService.rebuildMetadata(STORAGE0,
                                                repository.getId(),
                                                ARTIFACT_BASE_PATH_STRONGBOX_TIMESTAMPED_FIRST);

        artifactMetadataService.rebuildMetadata(STORAGE0,
                                                repository.getId(),
                                                ARTIFACT_BASE_PATH_STRONGBOX_TIMESTAMPED_SECOND);
    }

    @Test
    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                  ArtifactManagementTestExecutionListener.class })
    public void testRemoveTimestampedSnapshot(
            @MavenRepository(repositoryId = REPOSITORY_SNAPSHOTS, policy = SNAPSHOT) Repository repository,
            @MavenTestArtifact(repositoryId = REPOSITORY_SNAPSHOTS, id = GROUP_ID + ":" +
                                                                         ARTIFACT_ID1, versions = { "2.0-20190701.202015-1",
                                                                                                    "2.0-20190701.202101-2",
                                                                                                    "2.0-20190701.202203-3" })
                    List<Path> artifact)
            throws Exception
    {
        final UUID jobKey = expectedJobKey;
        final String jobName = expectedJobName;

        rebuildArtifactsMetadata(repository);

        final RootRepositoryPath repositoryPath = repositoryPathResolver.resolve(repository);
        final Path artifactPath = artifact.get(0).getParent();

        jobManager.registerExecutionListener(jobKey.toString(), (jobKey1, statusExecuted) ->
        {
            try
            {
                if (StringUtils.equals(jobKey1, jobKey.toString()) && statusExecuted)
                {
                    try (Stream<Path> pathStream = Files.walk(artifactPath))
                    {

                        long timestampedSnapshots = pathStream.filter(path -> path.toString().endsWith(".jar")).count();
                        assertEquals(1, timestampedSnapshots, "Amount of timestamped snapshots doesn't equal 1.");

                    }

                    assertTrue(getSnapshotArtifactVersion(repositoryPath, artifactPath).endsWith("-3"));
                }
            }
            catch (Exception e)
            {
                throw new UndeclaredThrowableException(e);
            }
        });

        addCronJobConfig(jobKey,
                         jobName,
                         RemoveTimestampedMavenSnapshotCronJob.class,
                         STORAGE0,
                         repository.getId(),
                         properties ->
                         {
                             properties.put("basePath", ARTIFACT_BASE_PATH_STRONGBOX_TIMESTAMPED_FIRST);
                             properties.put("numberToKeep", "1");
                             properties.put("keepPeriod", "0");
                         });

        await().atMost(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilTrue(receivedExpectedEvent());
    }

    @Test
    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                  ArtifactManagementTestExecutionListener.class })
    public void testRemoveTimestampedSnapshotInRepository(
            @MavenRepository(repositoryId = REPOSITORY_SNAPSHOTS, policy = SNAPSHOT) Repository repository,
            @MavenTestArtifact(repositoryId = REPOSITORY_SNAPSHOTS, id = GROUP_ID + ":" +
                                                                         ARTIFACT_ID2, versions = { "2.0-20190701.202015-1",
                                                                                                    "2.0-20190701.202101-2" })
                    List<Path> artifact)
            throws Exception
    {
        final UUID jobKey = expectedJobKey;
        final String jobName = expectedJobName;

        rebuildArtifactsMetadata(repository);

        final RootRepositoryPath repositoryPath = repositoryPathResolver.resolve(repository);
        final Path artifactPath = artifact.get(0).getParent();

        jobManager.registerExecutionListener(jobKey.toString(), (jobKey1, statusExecuted) ->
        {
            try
            {
                if (StringUtils.equals(jobKey1, jobKey.toString()) && statusExecuted)
                {
                    try (Stream<Path> pathStream = Files.walk(artifactPath))
                    {

                        long timestampedSnapshots = pathStream.filter(path -> path.toString().endsWith(".jar")).count();
                        assertEquals(1, timestampedSnapshots, "Amount of timestamped snapshots doesn't equal 1.");

                    }

                    assertTrue(getSnapshotArtifactVersion(repositoryPath, artifactPath).endsWith("-2"));
                }
            }
            catch (Exception e)
            {
                throw new UndeclaredThrowableException(e);
            }
        });

        addCronJobConfig(jobKey,
                         jobName,
                         RemoveTimestampedMavenSnapshotCronJob.class,
                         STORAGE0,
                         repository.getId(),
                         properties ->
                         {
                             properties.put("basePath", null);
                             properties.put("numberToKeep", "1");
                             properties.put("keepPeriod", "0");
                         });

        await().atMost(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilTrue(receivedExpectedEvent());
    }

    private String getSnapshotArtifactVersion(RootRepositoryPath repositoryPath,
                                              Path artifactPath)
            throws IOException
    {
        try (Stream<Path> pathStream = Files.walk(artifactPath))
        {
            Optional<Path> path = pathStream.filter(p -> p.toString().endsWith(".jar")).findFirst();
            if (path.isPresent())
            {
                Path relativize = repositoryPath.relativize(path.get());
                String unixBasedRelativePath = FilenameUtils.separatorsToUnix(relativize.toString());
                Artifact artifact = MavenArtifactUtils.convertPathToArtifact(unixBasedRelativePath);
                if (artifact != null)
                {
                    return artifact.getVersion();
                }
            }
        }

        return StringUtils.EMPTY;
    }

}
